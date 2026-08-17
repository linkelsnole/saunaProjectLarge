package com.example.modbus_api.security.service;

import com.example.modbus_api.config.JwtProperties;
import com.example.modbus_api.exception.AuthException;
import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.RefreshToken;
import com.example.modbus_api.security.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public RefreshToken createRefreshToken(AppUser user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String token) {
        RefreshToken currentToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthException("Refresh token not found"));

        if (currentToken.getExpiryAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(currentToken);
            throw new AuthException("Refresh token expired");
        }

        AppUser user = currentToken.getUser();
        refreshTokenRepository.delete(currentToken);

        return createRefreshToken(user);
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}
