package com.seatsure.authservice.service;

import com.seatsure.authservice.dto.JwtResponse;
import com.seatsure.authservice.dto.LoginRequest;
import com.seatsure.authservice.dto.LogoutRequest;
import com.seatsure.authservice.dto.RefreshTokenRequest;
import com.seatsure.authservice.dto.RegisterRequest;
import com.seatsure.authservice.dto.UserProfileResponse;
import com.seatsure.authservice.entity.RefreshToken;
import com.seatsure.authservice.entity.Role;
import com.seatsure.authservice.entity.User;
import com.seatsure.authservice.exception.EmailAlreadyExistsException;
import com.seatsure.authservice.exception.ResourceNotFoundException;
import com.seatsure.authservice.repository.RoleRepository;
import com.seatsure.authservice.repository.UserRepository;
import com.seatsure.authservice.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Register User
     */
    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "Email already registered : " + request.getEmail());
        }

        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() ->
                        new ResourceNotFoundException("ROLE_USER not found"));

        User user = new User();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        user.setPassword(
                passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        user.setRoles(roles);

        userRepository.save(user);

        return "User registered successfully.";
    }

    /**
     * Login
     */
    public JwtResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()));

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        String accessToken =
                jwtService.generateToken(userDetails);

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new JwtResponse(
                accessToken,
                refreshToken.getToken(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roles
        );
    }

    /**
     * Refresh Access Token
     */
    public JwtResponse refreshToken(
            RefreshTokenRequest request) {

        return refreshTokenService.refreshAccessToken(request);
    }

    /**
     * Logout User
     */
    public String logout(LogoutRequest request) {

        RefreshToken refreshToken =
                refreshTokenService.findByToken(
                                request.getRefreshToken())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Refresh token not found"));

        refreshToken.setRevoked(true);

        refreshTokenService.save(refreshToken);

        return "Logout successful.";
    }

    /**
     * Current Logged-in User
     */
    public UserProfileResponse getCurrentUser(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roles
        );
    }
}