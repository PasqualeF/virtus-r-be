package com.virtus.controller;

import com.virtus.dto.*;
import com.virtus.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;

    /**
     * Crea un nuovo account utente (endpoint pubblico)
     * POST /api/accounts
     */
    @PostMapping
    public ResponseEntity<AccountCreatedResponseDto> createAccount(@Valid @RequestBody AccountCreationDto accountData) {
        log.info("🌐 REST: Richiesta creazione account per utente: {}", accountData.getUserName());

        try {
            AccountCreatedResponseDto response = accountService.createAccount(accountData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Errore nella creazione account", e);
            return ResponseEntity.badRequest().body(
                    AccountCreatedResponseDto.builder()
                            .success(false)
                            .message("Errore creazione account: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Aggiorna un account esistente (richiede autenticazione)
     * PUT /api/accounts/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<AccountUpdatedResponseDto> updateAccount(
            @PathVariable @NotNull @Positive Integer userId,
            @Valid @RequestBody AccountUpdateDto accountData,
            HttpServletRequest request) {

        log.info("🌐 REST: Richiesta aggiornamento account per userId: {}", userId);

        try {
            String token = extractTokenFromRequest(request);
            AccountUpdatedResponseDto response = accountService.updateAccount(userId, accountData,token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Errore nell'aggiornamento account", e);
            return ResponseEntity.badRequest().body(
                    AccountUpdatedResponseDto.builder()
                            .success(false)
                            .message("Errore aggiornamento account: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Aggiorna la password di un account (richiede autenticazione)
     * PUT /api/accounts/{userId}/password
     */
    @PutMapping("/{userId}/password")
    public ResponseEntity<AccountUpdatedResponseDto> updatePassword(
            @PathVariable @NotNull @Positive Integer userId,
            @Valid @RequestBody PasswordUpdateDto passwordData,
            HttpServletRequest request) {

        log.info("🌐 REST: Richiesta aggiornamento password per userId: {}", userId);

        try {
            String token = extractTokenFromRequest(request);

            AccountUpdatedResponseDto response = accountService.updatePassword(userId, passwordData, token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Errore nell'aggiornamento password", e);
            return ResponseEntity.badRequest().body(
                    AccountUpdatedResponseDto.builder()
                            .success(false)
                            .message("Errore aggiornamento password: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Recupera le informazioni dell'account (richiede autenticazione)
     * GET /api/accounts/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AccountInfoDto> getAccount(
            @PathVariable @NotNull @Positive Integer userId,
            HttpServletRequest request) {

        log.info("🌐 REST: Richiesta informazioni account per userId: {}", userId);

        try {
            AccountInfoDto response = accountService.getAccount(userId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Errore nel recupero account", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Recupera le informazioni dell'account corrente (dall'utente autenticato)
     * GET /api/accounts/me
     */
    @GetMapping("/me")
    public ResponseEntity<AccountInfoDto> getCurrentAccount(HttpServletRequest request) {
        log.info("🌐 REST: Richiesta informazioni account corrente");

        try {
            // Estrae l'userId dal JWT
            JwtClaimsDto claims = (JwtClaimsDto) request.getAttribute("jwtClaims");

            if (claims == null || claims.getUserId() == null) {
                return ResponseEntity.badRequest().build();
            }

            AccountInfoDto response = accountService.getAccount(claims.getUserId(), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Errore nel recupero account corrente", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Endpoint per test di connettività
     * GET /api/accounts/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "accounts");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Estrae il token JWT dalla richiesta HTTP
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}