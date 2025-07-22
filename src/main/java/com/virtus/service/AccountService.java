package com.virtus.service;

import com.virtus.config.LibreBookingProperties;
import com.virtus.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final WebClient webClient;
    private final LibreBookingProperties properties;
    private final JwtService jwtService;

    /**
     * Crea un nuovo account utente (endpoint pubblico)
     */
    public AccountCreatedResponseDto createAccount(AccountCreationDto accountData) {
        log.info("📝 Creazione nuovo account per utente: {}", accountData.getUserName());

        try {
            CreateAccountRequestDto request = CreateAccountRequestDto.builder()
                    .firstName(accountData.getFirstName())
                    .lastName(accountData.getLastName())
                    .emailAddress(accountData.getEmailAddress())
                    .userName(accountData.getUserName())
                    .language(accountData.getLanguage() != null ? accountData.getLanguage() : "it_it")
                    .timezone(accountData.getTimezone() != null ? accountData.getTimezone() : "Europe/Rome")
                    .phone(accountData.getPhone())
                    .organization(accountData.getOrganization())
                    .position(accountData.getPosition())
                    .customAttributes(List.of())
                    .password(accountData.getPassword())
                    .acceptTermsOfService(accountData.isAcceptTermsOfService())
                    .build();

            String url = properties.getBaseUrl() + properties.getAccountsEndpoint() + "/";

            AccountCreatedResponseDto response = webClient.post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AccountCreatedResponseDto.class)
                    .block();

            log.info("✅ Account creato con successo per utente: {}", accountData.getUserName());
            return response;

        } catch (WebClientResponseException e) {
            log.error("❌ Errore creazione account: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Errore creazione account: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Errore generico creazione account", e);
            throw new RuntimeException("Errore creazione account: " + e.getMessage(), e);
        }
    }

    /**
     * Aggiorna un account esistente (richiede autenticazione)
     */
    public AccountUpdatedResponseDto updateAccount(Integer userId, AccountUpdateDto accountData,String token) {
        log.info("🔄 Aggiornamento account per userId: {}", userId);

        try {

            JwtClaimsDto claims = jwtService.extractClaims(token);

            // Verifica se la sessione LibreBooking è ancora valida
            if (!jwtService.isLibreBookingSessionValid(token)) {
                log.warn("🚨 Sessione LibreBooking scaduta per utente: {}", claims.getUsername());
                return AccountUpdatedResponseDto.builder()
                        .success(false)
                        .message("Sessione scaduta")
                        .build();
            }

            UpdateAccountRequestDto updateRequest = UpdateAccountRequestDto.builder()
                    .firstName(accountData.getFirstName())
                    .lastName(accountData.getLastName())
                    .emailAddress(accountData.getEmailAddress())
                    .userName(accountData.getUserName())
                    .language(accountData.getLanguage() != null ? accountData.getLanguage() : "it_it")
                    .timezone(accountData.getTimezone() != null ? accountData.getTimezone() : "Europe/Rome")
                    .phone(accountData.getPhone())
                    .organization(accountData.getOrganization())
                    .position(accountData.getPosition())
                    .customAttributes(List.of())
                    .build();

            String url = properties.getBaseUrl() + properties.getAccountsEndpoint() + "/" + userId;

            AccountUpdatedResponseDto response = webClient.post()
                    .uri(url)
                    .header("X-Booked-SessionToken", claims.getSessionToken())
                    .header("X-Booked-UserId", claims.getLibreBookingUserId())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .bodyValue(updateRequest)
                    .retrieve()
                    .bodyToMono(AccountUpdatedResponseDto.class)
                    .block();

            log.info("✅ Account aggiornato con successo per userId: {}", userId);
            if (response != null) {
                response.setSuccess(true);
            }
            return response;

        } catch (WebClientResponseException e) {
            log.error("❌ Errore aggiornamento account: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Errore aggiornamento account: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Errore generico aggiornamento account", e);
            throw new RuntimeException("Errore aggiornamento account: " + e.getMessage(), e);
        }
    }

    /**
     * Aggiorna la password di un account (richiede autenticazione)
     */
    public AccountUpdatedResponseDto updatePassword(Integer userId, PasswordUpdateDto passwordData, String token) {
        log.info("🔑 Aggiornamento password per userId: {}", userId);

        try {

            JwtClaimsDto claims = jwtService.extractClaims(token);

            // Verifica se la sessione LibreBooking è ancora valida
            if (!jwtService.isLibreBookingSessionValid(token)) {
                log.warn("🚨 Sessione LibreBooking scaduta per utente: {}", claims.getUsername());
                return AccountUpdatedResponseDto.builder()
                        .success(false)
                        .message("Sessione scaduta")
                        .build();
            }
            UpdatePasswordRequestDto updateRequest = UpdatePasswordRequestDto.builder()
                    .currentPassword(passwordData.getCurrentPassword())
                    .newPassword(passwordData.getNewPassword())
                    .build();

            String url = properties.getBaseUrl() + properties.getAccountsEndpoint() + "/" + userId + "/Password";

            AccountUpdatedResponseDto response = webClient.post()
                    .uri(url)
                    .header("X-Booked-SessionToken", claims.getSessionToken())
                    .header("X-Booked-UserId", claims.getLibreBookingUserId())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .bodyValue(updateRequest)
                    .retrieve()
                    .bodyToMono(AccountUpdatedResponseDto.class)
                    .block();

            log.info("✅ Password aggiornata con successo per userId: {}", userId);
            if (response != null) {
                response.setSuccess(true);
            }
            return response;

        } catch (WebClientResponseException e) {
            log.error("❌ Errore aggiornamento password: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Errore aggiornamento password: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Errore generico aggiornamento password", e);
            throw new RuntimeException("Errore aggiornamento password: " + e.getMessage(), e);
        }
    }

    /**
     * Recupera le informazioni dell'account (richiede autenticazione)
     */
    public AccountInfoDto getAccount(Integer userId, HttpServletRequest request) {
        log.info("📋 Recupero informazioni account per userId: {}", userId);

        try {
            // Estrae le informazioni di sessione dal JWT
            String sessionToken = (String) request.getAttribute("sessionToken");
            String libreBookingUserId = (String) request.getAttribute("libreBookingUserId");

            String url = properties.getBaseUrl() + properties.getAccountsEndpoint() + "/" + userId;

            AccountResponseDto response = webClient.get()
                    .uri(url)
                    .header("X-Booked-SessionToken", sessionToken)
                    .header("X-Booked-UserId", libreBookingUserId)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToMono(AccountResponseDto.class)
                    .block();

            AccountInfoDto accountInfo = mapToAccountInfo(response);

            log.info("✅ Informazioni account recuperate con successo per userId: {}", userId);
            return accountInfo;

        } catch (WebClientResponseException e) {
            log.error("❌ Errore recupero account: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Errore recupero account: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Errore generico recupero account", e);
            throw new RuntimeException("Errore recupero account: " + e.getMessage(), e);
        }
    }

    /**
     * Mappa la risposta di LibreBooking per il frontend
     */
    private AccountInfoDto mapToAccountInfo(AccountResponseDto response) {
        return AccountInfoDto.builder()
                .userId(response.getUserId())
                .firstName(response.getFirstName())
                .lastName(response.getLastName())
                .emailAddress(response.getEmailAddress())
                .userName(response.getUserName())
                .language(response.getLanguage())
                .timezone(response.getTimezone())
                .phone(response.getPhone())
                .organization(response.getOrganization())
                .position(response.getPosition())
                .icsUrl(response.getIcsUrl())
                .build();
    }
}