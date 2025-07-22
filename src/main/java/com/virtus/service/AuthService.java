package com.virtus.service;

import com.virtus.config.LibreBookingProperties;
import com.virtus.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final WebClient webClient;
    private final LibreBookingProperties properties;
    private final JwtService jwtService;

    /**
     * Esegue il login dell'utente
     */
    public LoginResponseDto login(LoginRequestDto loginRequest) {
        log.info("🔐 Tentativo di login per utente: {}", loginRequest.getUsername());

        try {
            // 1. Autentica con LibreBooking usando le credenziali dell'utente
            LibreBookingAuthResponse authResponse = authenticateWithLibreBooking(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
            );

            // 2. Recupera informazioni utente da LibreBooking
            AccountResponseDto accountInfo = getUserInfoFromLibreBooking(authResponse);

            // 3. Prepara i claims per il JWT
            JwtClaimsDto claims = JwtClaimsDto.builder()
                    .userId(accountInfo.getUserId())
                    .username(accountInfo.getUserName())
                    .firstName(accountInfo.getFirstName())
                    .lastName(accountInfo.getLastName())
                    .sessionToken(authResponse.getSessionToken())
                    .libreBookingUserId(authResponse.getUserId())
                    .sessionExpiry(calculateSessionExpiryFromBooking(authResponse.getSessionExpires()))
                    .build();

            // 4. Genera JWT token
            String jwtToken = jwtService.generateToken(claims);

            // 5. Prepara risposta
            UserInfoDto userInfo = UserInfoDto.builder()
                    .userId(accountInfo.getUserId())
                    .username(accountInfo.getUserName())
                    .firstName(accountInfo.getFirstName())
                    .lastName(accountInfo.getLastName())
                    .emailAddress(accountInfo.getEmailAddress())
                    .language(accountInfo.getLanguage())
                    .timezone(accountInfo.getTimezone())
                    .phone(accountInfo.getPhone())
                    .organization(accountInfo.getOrganization())
                    .position(accountInfo.getPosition())
                    .build();

            log.info("✅ Login completato con successo per utente: {}", loginRequest.getUsername());

            return LoginResponseDto.builder()
                    .accessToken(jwtToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getTokenExpirationTime(jwtToken))
                    .user(userInfo)
                    .success(true)
                    .message("Login effettuato con successo")
                    .build();

        } catch (WebClientResponseException e) {
            log.error("❌ Errore autenticazione LibreBooking: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            String message = getAuthErrorMessage(e.getStatusCode().value());
            return LoginResponseDto.builder()
                    .success(false)
                    .message(message)
                    .build();

        } catch (Exception e) {
            log.error("❌ Errore generico durante il login", e);
            return LoginResponseDto.builder()
                    .success(false)
                    .message("Errore interno del server")
                    .build();
        }
    }

    /**
     * Recupera informazioni sull'utente corrente
     */
    public CurrentUserResponseDto getCurrentUser(String token) {
        try {
            if (!jwtService.validateToken(token)) {
                return CurrentUserResponseDto.builder()
                        .authenticated(false)
                        .build();
            }

            JwtClaimsDto claims = jwtService.extractClaims(token);

            // Verifica se la sessione LibreBooking è ancora valida
            if (!jwtService.isLibreBookingSessionValid(token)) {
                log.warn("🚨 Sessione LibreBooking scaduta per utente: {}", claims.getUsername());
                return CurrentUserResponseDto.builder()
                        .authenticated(false)
                        .build();
            }

            UserInfoDto userInfo = UserInfoDto.builder()
                    .userId(claims.getUserId())
                    .username(claims.getUsername())
                    .firstName(claims.getFirstName())
                    .lastName(claims.getLastName())
                    .build();

            return CurrentUserResponseDto.builder()
                    .user(userInfo)
                    .authenticated(true)
                    .tokenExpiresIn(jwtService.getTokenExpirationTime(token))
                    .build();

        } catch (Exception e) {
            log.error("❌ Errore recupero utente corrente", e);
            return CurrentUserResponseDto.builder()
                    .authenticated(false)
                    .build();
        }
    }

    /**
     * Refresh del token quando la sessione LibreBooking è scaduta
     */
    public RefreshTokenResponseDto refreshToken(String token, String username, String password) {
        try {
            log.info("🔄 Refresh token per utente: {}", username);

            // Re-autentica con LibreBooking
            LibreBookingAuthResponse authResponse = authenticateWithLibreBooking(username, password);

            // Recupera claims esistenti
            JwtClaimsDto existingClaims = jwtService.extractClaims(token);

            // Aggiorna con nuova sessione
            JwtClaimsDto newClaims = null;
//                    existingClaims.toBuilder()
//                    .sessionToken(authResponse.getSessionToken())
//                    .libreBookingUserId(authResponse.getUserId())
//                    .sessionExpiry(calculateSessionExpiry())
//                    .build();

            // Genera nuovo token
            String newJwtToken = jwtService.generateToken(newClaims);

            return RefreshTokenResponseDto.builder()
                    .accessToken(newJwtToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getTokenExpirationTime(newJwtToken))
                    .success(true)
                    .message("Token rinnovato con successo")
                    .build();

        } catch (Exception e) {
            log.error("❌ Errore refresh token", e);
            return RefreshTokenResponseDto.builder()
                    .success(false)
                    .message("Errore durante il refresh del token")
                    .build();
        }
    }

    /**
     * Logout (logicamente, il JWT non può essere invalidato server-side)
     */
    public LogoutResponseDto logout() {
        log.info("👋 Logout effettuato");
        return LogoutResponseDto.builder()
                .success(true)
                .message("Logout effettuato con successo")
                .build();
    }

    /**
     * Autentica con LibreBooking usando le credenziali dell'utente
     */
    private LibreBookingAuthResponse authenticateWithLibreBooking(String username, String password) {
        LibreBookingAuthRequest request = LibreBookingAuthRequest.builder()
                .username(username)
                .password(password)
                .build();

        return webClient.post()
                .uri(properties.getBaseUrl() + properties.getAuthEndpoint())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LibreBookingAuthResponse.class)
                .block();
    }

    /**
     * Recupera informazioni utente da LibreBooking
     */
    private AccountResponseDto getUserInfoFromLibreBooking(LibreBookingAuthResponse authResponse) {
        String url = properties.getBaseUrl() + properties.getAccountsEndpoint() + "/" + authResponse.getUserId();

        return webClient.get()
                .uri(url)
                .header("X-Booked-SessionToken", authResponse.getSessionToken())
                .header("X-Booked-UserId", authResponse.getUserId())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(AccountResponseDto.class)
                .block();
    }

    /**
     * Calcola la scadenza della sessione LibreBooking (30 minuti)
     */
    private Long calculateSessionExpiry() {
        return LocalDateTime.now()
                .plusMinutes(30)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
    private Long calculateSessionExpiryFromBooking(String expireDate) {
        // Parsing del timestamp con offset senza due punti (+0000)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

        // Ottieni OffsetDateTime in UTC (come +0000)
        OffsetDateTime odtUtc = OffsetDateTime.parse(expireDate, formatter);

        // Convertilo istantaneamente a Europe/Rome (gestisce DST)
        ZonedDateTime rome = odtUtc.atZoneSameInstant(ZoneId.of("Europe/Rome"));

        // Sottrai 2 minuti
        ZonedDateTime adjusted = rome.minusMinutes(2);
        return adjusted.toInstant().toEpochMilli();

    }


    /**
     * Converte i codici di errore HTTP in messaggi user-friendly
     */
    private String getAuthErrorMessage(int statusCode) {
        switch (statusCode) {
            case 401:
                return "Credenziali non valide";
            case 403:
                return "Accesso negato";
            case 404:
                return "Utente non trovato";
            case 500:
                return "Errore interno del server di autenticazione";
            default:
                return "Errore durante l'autenticazione";
        }
    }
}