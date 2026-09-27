package com.fatmanur.ecommerce.auth.service;

import com.fatmanur.ecommerce.auth.dto.AuthSession;
import com.fatmanur.ecommerce.auth.dto.LoginRequest;
import com.fatmanur.ecommerce.auth.dto.RegisterRequest;
import com.fatmanur.ecommerce.auth.entity.RefreshToken;
import com.fatmanur.ecommerce.auth.exception.EmailAlreadyExistsException;
import com.fatmanur.ecommerce.auth.exception.InvalidCredentialsException;
import com.fatmanur.ecommerce.auth.exception.UserNotFoundException;
import com.fatmanur.ecommerce.auth.repository.RefreshTokenRepository;
import com.fatmanur.ecommerce.auth.security.JwtUtil;
import com.fatmanur.ecommerce.user.entity.User;
import com.fatmanur.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();

        userRepository.save(user);
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return createSession(user);
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthSession refresh(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(hashToken(rawRefreshToken))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            throw new InvalidCredentialsException("Refresh token expired");
        }

        // Token rotation: eski token tekrar kullanılamaz.
        refreshToken.setRevoked(true);

        return createSession(refreshToken.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository
                .findByTokenHashAndRevokedFalse(hashToken(rawRefreshToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        refreshTokenRepository.findAllByUserIdAndRevokedFalse(user.getId())
                .forEach(token -> token.setRevoked(true));

        user.setDeleted(true);
    }

    private AuthSession createSession(User user) {
        String rawRefreshToken = generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hashToken(rawRefreshToken))
                .user(user)
                .expiresAt(LocalDateTime.now()
                        .plusSeconds(refreshTokenExpirationMs / 1000))
                .build();

        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtUtil.generateAccessToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthSession(
                accessToken,
                rawRefreshToken,
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}