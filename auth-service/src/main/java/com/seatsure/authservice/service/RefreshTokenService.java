package com.seatsure.authservice.service;

import com.seatsure.authservice.repository.RefreshTokenRepository;
import com.seatsure.authservice.repository.UserRepository;
import com.seatsure.authservice.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.seatsure.authservice.entity.RefreshToken;
import com.seatsure.authservice.entity.User;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.seatsure.authservice.dto.JwtResponse;
import com.seatsure.authservice.dto.RefreshTokenRequest;
import org.springframework.security.core.userdetails.UserDetails;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenDuration;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtService jwtService) {

        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public RefreshToken createRefreshToken(User user) {

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);

        refreshToken.setToken(UUID.randomUUID().toString());

        refreshToken.setExpiryDate(
                Instant.now().plusMillis(refreshTokenDuration));

        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {

        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {

        if (token.getExpiryDate().isBefore(Instant.now())) {

            refreshTokenRepository.delete(token);

            throw new RuntimeException("Refresh token has expired. Please login again.");
        }

        return token;
    }

    public void deleteByUser(User user) {

        refreshTokenRepository.deleteByUser(user);
    }

    public JwtResponse refreshAccessToken(RefreshTokenRequest request) {

        RefreshToken refreshToken = findByToken(request.getRefreshToken())
                .orElseThrow(() ->
                        new RuntimeException("Refresh token not found"));

        verifyExpiration(refreshToken);

        com.seatsure.authservice.entity.User user = refreshToken.getUser();

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .authorities(
                                user.getRoles()
                                        .stream()
                                        .map(role -> role.getName())
                                        .toArray(String[]::new)
                        )
                        .build();

        String accessToken = jwtService.generateToken(userDetails);

        JwtResponse response = new JwtResponse();

        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken.getToken());
        response.setUserId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setRoles(
                user.getRoles()
                        .stream()
                        .map(role -> role.getName())
                        .collect(java.util.stream.Collectors.toSet())
        );

        return response;
    }
}