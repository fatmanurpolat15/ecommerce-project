package com.fatmanur.ecommerce.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieService {

    private final long expirationMs;
    private final boolean secure;

    public CookieService(
            @Value("${app.refresh-token-expiration-ms}") long expirationMs,
            @Value("${app.refresh-cookie-secure}") boolean secure) {
        this.expirationMs = expirationMs;
        this.secure = secure;
    }

    public ResponseCookie createRefreshCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofMillis(expirationMs))
                .build();
    }

    public ResponseCookie createExpiredRefreshCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}