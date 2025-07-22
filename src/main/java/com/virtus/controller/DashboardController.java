package com.virtus.controller;

import com.virtus.dto.DashboardStatsDto;
import com.virtus.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Recupera le statistiche della dashboard per l'utente corrente
     * GET /dashboard/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats(
            @RequestParam(value = "period", required = false, defaultValue = "thisMonth") String period,
            HttpServletRequest request) {

        log.info("🌐 REST: Richiesta statistiche dashboard per periodo: {}", period);

        try {
            String token = extractTokenFromRequest(request);

            if (token == null) {
                log.warn("❌ Token JWT mancante");
                return ResponseEntity.status(401).body(
                        DashboardStatsDto.builder()
                                .success(false)
                                .message("Token di autenticazione mancante")
                                .build()
                );
            }

            DashboardStatsDto response = dashboardService.getDashboardStats(request, token, period);

            if (response.getSuccess()) {
                log.info("✅ Statistiche dashboard recuperate per periodo: {}", period);
                return ResponseEntity.ok(response);
            } else {
                log.warn("⚠️ Errore nel recupero statistiche: {}", response.getMessage());
                return ResponseEntity.status(401).body(response);
            }

        } catch (RuntimeException e) {
            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
            return ResponseEntity.status(401).body(
                    DashboardStatsDto.builder()
                            .success(false)
                            .message("Errore autenticazione: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("❌ Errore nel recupero statistiche dashboard", e);
            return ResponseEntity.status(500).body(
                    DashboardStatsDto.builder()
                            .success(false)
                            .message("Errore interno del server: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Recupera il riepilogo delle attività dell'utente
     * GET /dashboard/activity-summary
     */
    @GetMapping("/activity-summary")
    public ResponseEntity<DashboardStatsDto> getActivitySummary(
            HttpServletRequest request) {

        log.info("🌐 REST: Richiesta riepilogo attività utente");

        try {
            String token = extractTokenFromRequest(request);

            if (token == null) {
                return ResponseEntity.status(401).body(
                        DashboardStatsDto.builder()
                                .success(false)
                                .message("Token di autenticazione mancante")
                                .build()
                );
            }

            DashboardStatsDto response = dashboardService.getActivitySummary(request, token);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Errore nel recupero riepilogo attività", e);
            return ResponseEntity.status(500).body(
                    DashboardStatsDto.builder()
                            .success(false)
                            .message("Errore interno del server: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Endpoint per test di connettività dashboard
     * GET /dashboard/health
     */
    @GetMapping("/health")
    public ResponseEntity<DashboardStatsDto> health() {
        return ResponseEntity.ok(
                DashboardStatsDto.builder()
                        .success(true)
                        .message("Dashboard service is healthy")
                        .activeBookings(0L)
                        .totalHours(0.0)
                        .gymsUsed(0L)
                        .upcomingBookings(0L)
                        .build()
        );
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