package com.seatsure.authservice.service;

import com.seatsure.authservice.dto.JwtResponse;
import com.seatsure.authservice.dto.RefreshTokenRequest;
import com.seatsure.authservice.entity.RefreshToken;
import com.seatsure.authservice.entity.User;
import com.seatsure.authservice.exception.RefreshTokenExpiredException;
import com.seatsure.authservice.exception.RefreshTokenNotFoundException;
import com.seatsure.authservice.exception.RefreshTokenRevokedException;
import com.seatsure.authservice.repository.RefreshTokenRepository;
import com.seatsure.authservice.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenDuration;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService) {

        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    /**
     * Create Refresh Token
     */
    public RefreshToken createRefreshToken(User user) {

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(
                Instant.now().plusMillis(refreshTokenDuration));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Find Refresh Token
     */
    public Optional<RefreshToken> findByToken(String token) {

        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Save Refresh Token
     */
    public RefreshToken save(RefreshToken refreshToken) {

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify Refresh Token
     */
    public RefreshToken verifyExpiration(RefreshToken token) {

        if (token.isRevoked()) {
            throw new RefreshTokenRevokedException(
                    "Refresh token has been revoked.");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {

            token.setRevoked(true);
            refreshTokenRepository.save(token);

            throw new RefreshTokenExpiredException(
                    "Refresh token has expired. Please login again.");
        }

        return token;
    }

    /**
     * Logout
     */
    public void logout(String refreshTokenValue) {

        RefreshToken refreshToken = findByToken(refreshTokenValue)
                .orElseThrow(() ->
                        new RefreshTokenNotFoundException("Refresh token not found."));

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Generate New Access Token
     */
    public JwtResponse refreshAccessToken(
            RefreshTokenRequest request) {

        RefreshToken refreshToken =
                findByToken(request.getRefreshToken())
                        .orElseThrow(() ->
                                new RefreshTokenNotFoundException(
                                        "Refresh token not found."));

        verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .authorities(
                                user.getRoles()
                                        .stream()
                                        .map(role -> role.getName())
                                        .toArray(String[]::new)
                        )
                        .build();

        String accessToken =
                jwtService.generateToken(userDetails);

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
                        .collect(Collectors.toSet())
        );

        return response;
    }
}