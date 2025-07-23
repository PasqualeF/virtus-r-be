package com.virtus.controller;

import com.virtus.dto.*;
import com.virtus.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "https://virtustaranto.duckdns.org", allowCredentials = "true")
@Validated
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint per il login
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        log.info("🌐 REST: Richiesta login per utente: {}", loginRequest.getUsername());

        LoginResponseDto response = authService.login(loginRequest);

        if (response.isSuccess()) {
            log.info("✅ Login completato per utente: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ Login fallito per utente: {}", loginRequest.getUsername());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Endpoint per il logout
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDto> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        log.info("🌐 REST: Richiesta logout");

        LogoutResponseDto response = authService.logout();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint per ottenere informazioni sull'utente corrente
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponseDto> getCurrentUser(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        log.info("token me : {}", token);

        if (token == null) {
            return ResponseEntity.badRequest().body(
                    CurrentUserResponseDto.builder()
                            .authenticated(false)
                            .build()
            );
        }

        CurrentUserResponseDto response = authService.getCurrentUser(token);

        if (response.isAuthenticated()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * Endpoint per il refresh del token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDto> refreshToken(
            HttpServletRequest request,
            @Valid @RequestBody LoginRequestDto credentials) {

        String token = extractTokenFromRequest(request);

        if (token == null) {
            return ResponseEntity.badRequest().body(
                    RefreshTokenResponseDto.builder()
                            .success(false)
                            .message("Token non fornito")
                            .build()
            );
        }

        RefreshTokenResponseDto response = authService.refreshToken(
                token,
                credentials.getUsername(),
                credentials.getPassword()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Endpoint per verificare lo stato del token
     * GET /api/auth/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);

        Map<String, Object> response = new HashMap<>();

        if (token == null) {
            response.put("valid", false);
            response.put("message", "Token non fornito");
            return ResponseEntity.badRequest().body(response);
        }

        CurrentUserResponseDto userResponse = authService.getCurrentUser(token);

        response.put("valid", userResponse.isAuthenticated());
        response.put("user", userResponse.getUser());
        response.put("expiresIn", userResponse.getTokenExpiresIn());

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint per test di connettività
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "auth");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Estrae il token JWT dalla richiesta HTTP
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        log.info("✅ bearerToken: {}", bearerToken);

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}