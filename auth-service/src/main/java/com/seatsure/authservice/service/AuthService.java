package com.seatsure.authservice.service;

import com.seatsure.authservice.dto.*;
import com.seatsure.authservice.entity.RefreshToken;
import com.seatsure.authservice.entity.Role;
import com.seatsure.authservice.entity.User;
import com.seatsure.authservice.exception.EmailAlreadyExistsException;
import com.seatsure.authservice.exception.ResourceNotFoundException;
import com.seatsure.authservice.repository.RoleRepository;
import com.seatsure.authservice.repository.UserRepository;
import com.seatsure.authservice.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
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

    public String register(RegisterRequest request){
        //check Duplicate email
        if(userRepository.existsByEmail(request.getEmail()))
            throw new EmailAlreadyExistsException("Email already registered" + request.getEmail());

            // 2. Get ROLE_USER
        Role role = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() ->
                            new ResourceNotFoundException("ROLE_USER not found"));

            // 3. Create User entity
            User user = new User();

            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());

            // Encrypt password
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            user.setPhone(request.getPhone());

            // 4. Assign role
            Set<Role> roles = new HashSet<>();
            roles.add(role);

            user.setRoles(roles);

            // 5. Save user
            userRepository.save(user);

            return "User registered successfully.";
        }
    public JwtResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        String token = jwtService.generateToken(userDetails);

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new JwtResponse(
                token,
                refreshToken.getToken(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roles
        );
    }
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
    public JwtResponse refreshToken(RefreshTokenRequest request) {
        return refreshTokenService.refreshAccessToken(request);
    }

}
