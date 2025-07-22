package com.virtus.controller;

import com.virtus.dto.*;
import com.virtus.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Crea una nuova prenotazione (richiede JWT)
     * POST /reservations
     */
    @PostMapping
    public ResponseEntity<ReservationCreatedDto> createReservation(
            @Valid @RequestBody CreateReservationDto reservationData,
            HttpServletRequest request) {

        log.info("🌐 REST: Richiesta creazione prenotazione: {}", reservationData.getTitle());

        try {
            String token = extractTokenFromRequest(request);

            if (token == null) {
                return ResponseEntity.badRequest().body(
                        ReservationCreatedDto.builder()
                                .build()
                );
            }
            ReservationCreatedDto response = reservationService.createReservation(reservationData, request,token);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
            return ResponseEntity.status(401).body(
                    ReservationCreatedDto.builder()
                            .success(false)
                            .message("Errore autenticazione: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("❌ Errore nella creazione prenotazione", e);
            return ResponseEntity.badRequest().body(
                    ReservationCreatedDto.builder()
                            .success(false)
                            .message("Errore creazione prenotazione: " + e.getMessage())
                            .build()
            );
        }
    }


    /**
     * Crea una nuova prenotazione (richiede JWT)
     * POST /reservations
     */
    @PostMapping("/{reservationId}")
    public ResponseEntity<ReservationCreatedDto> updateReservation(
            @Valid @RequestBody CreateReservationDto reservationData,
            @PathVariable @NotNull String reservationId,
            HttpServletRequest request) {

        log.info("🌐 REST: Richiesta update prenotazione: {}", reservationId);

        try {
            String token = extractTokenFromRequest(request);

            if (token == null) {
                return ResponseEntity.badRequest().body(
                        ReservationCreatedDto.builder()
                                .build()
                );
            }
            ReservationCreatedDto response = reservationService.updateReservation(reservationData, request,token,reservationId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
            return ResponseEntity.status(400).body(
                    ReservationCreatedDto.builder()
                            .success(false)
                            .message("Errore autenticazione: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("❌ Errore nella creazione prenotazione", e);
            return ResponseEntity.badRequest().body(
                    ReservationCreatedDto.builder()
                            .success(false)
                            .message("Errore creazione prenotazione: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Recupera le prenotazioni dell'utente corrente (richiede JWT)
     * GET /reservations/me?options=filter_options
     */
    @GetMapping("/me")
    public ResponseEntity<UserReservationsResponse> getReservationUser(
            @RequestParam(value = "options", required = false, defaultValue = "upcoming") String options,
            HttpServletRequest request) {

        log.info("🌐 REST: Richiesta prenotazioni utente con opzioni: {}", options);

        try {
            String token = extractTokenFromRequest(request);

            if (token == null) {
                log.warn("❌ Token JWT mancante");
                return ResponseEntity.status(401).body(
                        UserReservationsResponse.builder()
                                .success(false)
                                .message("Token di autenticazione mancante")
                                .reservations(Collections.emptyList())
                                .totalCount(0)
                                .build()
                );
            }

            UserReservationsResponse response = reservationService.getReservationUser(request, token, options);

            if (response.getSuccess()) {
                log.info("✅ Recuperate {} prenotazioni per l'utente", response.getTotalCount());
                return ResponseEntity.ok(response);
            } else {
                log.warn("⚠️ Errore nel recupero prenotazioni: {}", response.getMessage());
                return ResponseEntity.status(401).body(response);
            }

        } catch (RuntimeException e) {
            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
            return ResponseEntity.status(401).body(
                    UserReservationsResponse.builder()
                            .success(false)
                            .message("Errore autenticazione: " + e.getMessage())
                            .reservations(Collections.emptyList())
                            .totalCount(0)
                            .build()
            );
        } catch (Exception e) {
            log.error("❌ Errore nel recupero prenotazioni utente", e);
            return ResponseEntity.status(500).body(
                    UserReservationsResponse.builder()
                            .success(false)
                            .message("Errore interno del server: " + e.getMessage())
                            .reservations(Collections.emptyList())
                            .totalCount(0)
                            .build()
            );
        }
    }

    /**
     * Verifica disponibilità risorsa (richiede JWT)
     * GET /reservations/availability?resourceId=1&startDateTime=2025-07-17T10:00:00&endDateTime=2025-07-17T12:00:00
     */
//    @GetMapping("/availability")
//    public ResponseEntity<Map<String, Object>> checkAvailability(
//            @RequestParam Integer resourceId,
//            @RequestParam String startDateTime,
//            @RequestParam String endDateTime,
//            HttpServletRequest request) {
//
//        log.info("🌐 REST: Verifica disponibilità risorsa {} dalle {} alle {}",
//                resourceId, startDateTime, endDateTime);
//
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            // Valida JWT
//            jwtUtil.extractAndValidateJwt(request);
//
//            // TODO: Implementa logica di verifica disponibilità
//            // Per ora ritorna sempre disponibile
//            response.put("available", true);
//            response.put("resourceId", resourceId);
//            response.put("startDateTime", startDateTime);
//            response.put("endDateTime", endDateTime);
//            response.put("message", "Risorsa disponibile");
//
//            return ResponseEntity.ok(response);
//
//        } catch (RuntimeException e) {
//            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
//            response.put("available", false);
//            response.put("message", "Errore autenticazione: " + e.getMessage());
//            return ResponseEntity.status(401).body(response);
//        } catch (Exception e) {
//            log.error("❌ Errore verifica disponibilità", e);
//            response.put("available", false);
//            response.put("message", "Errore verifica disponibilità: " + e.getMessage());
//            return ResponseEntity.badRequest().body(response);
//        }
//    }

    /**
     * Ottieni le risorse disponibili (richiede JWT)
     * GET /reservations/resources
     */
    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> getResources(HttpServletRequest request) {
        log.info("🌐 REST: Richiesta elenco risorse");

        Map<String, Object> response = new HashMap<>();

        try {
            // Valida JWT
           // jwtUtil.extractAndValidateJwt(request);

            // TODO: Implementa logica per recuperare risorse da LibreBooking
            // Per ora ritorna dati mock
            response.put("resources", java.util.List.of(
                    java.util.Map.of(
                            "id", 1,
                            "name", "Palestra A",
                            "description", "Palestra principale",
                            "capacity", 50
                    ),
                    java.util.Map.of(
                            "id", 2,
                            "name", "Palestra B",
                            "description", "Palestra secondaria",
                            "capacity", 30
                    )
            ));
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Errore autenticazione: " + e.getMessage());
            return ResponseEntity.status(401).body(response);
        } catch (Exception e) {
            log.error("❌ Errore recupero risorse", e);
            response.put("success", false);
            response.put("message", "Errore recupero risorse: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Ottieni le prenotazioni dell'utente corrente (richiede JWT)
     * GET /reservations/my
     */
//    @GetMapping("/my")
//    public ResponseEntity<Map<String, Object>> getMyReservations(HttpServletRequest request) {
//        log.info("🌐 REST: Richiesta prenotazioni utente corrente");
//
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            // Valida JWT e ottieni userId
//        //    Integer userId = jwtUtil.getUserIdFromToken(request);
//
//            // TODO: Implementa logica per recuperare prenotazioni utente da LibreBooking
//            // Per ora ritorna dati mock
//            response.put("reservations", java.util.List.of());
//            response.put("userId", userId);
//            response.put("success", true);
//            response.put("message", "Prenotazioni recuperate con successo");
//
//            return ResponseEntity.ok(response);
//
//        } catch (RuntimeException e) {
//            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
//            response.put("success", false);
//            response.put("message", "Errore autenticazione: " + e.getMessage());
//            return ResponseEntity.status(401).body(response);
//        } catch (Exception e) {
//            log.error("❌ Errore recupero prenotazioni", e);
//            response.put("success", false);
//            response.put("message", "Errore recupero prenotazioni: " + e.getMessage());
//            return ResponseEntity.badRequest().body(response);
//        }
//    }


    @DeleteMapping("/{reservationId}")
    public ResponseEntity<LibreBookingReservationResponseDto> deleteReservation(
            @PathVariable @NotNull String reservationId,
            HttpServletRequest request) {

        log.info("🌐 REST: Richiesta aggiornamento account per userId: {}", reservationId);
        String token = extractTokenFromRequest(request);

        if (token == null) {
            return ResponseEntity.badRequest().body(
                    LibreBookingReservationResponseDto.builder()
                            .build()
            );
        }
        try {
            LibreBookingReservationResponseDto response = reservationService.deleteReservation(reservationId, token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Errore nell'aggiornamento account", e);
            return ResponseEntity.badRequest().body(
                    LibreBookingReservationResponseDto.builder()
                            .message("Errore aggiornamento account: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Endpoint per test di connettività
     * GET /reservations/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "reservations");
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