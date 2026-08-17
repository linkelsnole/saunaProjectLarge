package com.example.modbus_api.security.service;

import com.example.modbus_api.config.JwtProperties;
import com.example.modbus_api.exception.AuthException;
import com.example.modbus_api.logging.ActionType;
import com.example.modbus_api.security.CookieUtils;
import com.example.modbus_api.security.dto.AuthResponse;
import com.example.modbus_api.security.dto.UserInfoResponse;
import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.RefreshToken;
import com.example.modbus_api.security.repository.UserRepository;
import com.example.modbus_api.service.UserActionLogService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserActionLogService userActionLogService;
    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthService(
            AuthenticationManager authenticationManager,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            JwtProperties jwtProperties,
            UserActionLogService userActionLogService,
            UserRepository userRepository,
            UserDetailsServiceImpl userDetailsService
    ) {
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userActionLogService = userActionLogService;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }

    public AuthResponse login(String username, String password, HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (AuthenticationException e) {
            userActionLogService.logAs(username, ActionType.AUTH, "Вход в систему", false, "Неверный логин или пароль");
            throw e;
        }
        userActionLogService.logAs(username, ActionType.AUTH, "Вход в систему", true, null);

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("Пользователь не найден: " + username));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        addRefreshCookie(response, refreshToken);

        return buildResponse(user, (UserDetails) authentication.getPrincipal());
    }

    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = CookieUtils.getCookieValue(request, jwtProperties.getRefreshCookieName())
                .orElseThrow(() -> new AuthException("Refresh token is missing"));

        RefreshToken refreshToken = refreshTokenService.rotateRefreshToken(refreshTokenValue);
        addRefreshCookie(response, refreshToken);

        AppUser user = refreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        return buildResponse(user, userDetails);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current != null && current.getName() != null && !"anonymousUser".equals(current.getName())) {
            userActionLogService.logAs(current.getName(), ActionType.AUTH, "Выход из системы", true, null);
        }
        String refreshTokenValue = CookieUtils.getCookieValue(request, jwtProperties.getRefreshCookieName()).orElse(null);
        if (refreshTokenValue != null && !refreshTokenValue.trim().isEmpty()) {
            refreshTokenService.deleteByToken(refreshTokenValue);
        }

        CookieUtils.clearCookie(
                response,
                jwtProperties.getRefreshCookieName(),
                jwtProperties.isSecureCookie(),
                jwtProperties.getRefreshCookiePath(),
                jwtProperties.getRefreshCookieSameSite()
        );
    }

    public UserInfoResponse currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new AuthException("User is not authenticated");
        }

        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AuthException("Пользователь не найден: " + authentication.getName()));
        return UserInfoResponse.from(user);
    }

    private void addRefreshCookie(HttpServletResponse response, RefreshToken refreshToken) {
        CookieUtils.addHttpOnlyCookie(
                response,
                jwtProperties.getRefreshCookieName(),
                refreshToken.getToken(),
                jwtProperties.getRefreshTokenExpirationMs(),
                jwtProperties.isSecureCookie(),
                jwtProperties.getRefreshCookiePath(),
                jwtProperties.getRefreshCookieSameSite()
        );
    }

    private AuthResponse buildResponse(AppUser user, UserDetails userDetails) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(userDetails))
                .tokenType("Bearer")
                .expiresInMs(jwtProperties.getAccessTokenExpirationMs())
                .user(UserInfoResponse.from(user))
                .build();
    }
}
