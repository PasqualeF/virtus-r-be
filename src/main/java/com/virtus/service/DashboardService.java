package com.virtus.service;

import com.virtus.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ReservationService reservationService;
    private final JwtService jwtService;

    /**
     * Recupera le statistiche della dashboard per l'utente
     */
    public DashboardStatsDto getDashboardStats(HttpServletRequest request, String token, String period) {
        try {
            log.info("📊 Calcolo statistiche dashboard per periodo: {}", period);

            // Estrae le informazioni di sessione dal JWT
            JwtClaimsDto claims = jwtService.extractClaims(token);

            // Verifica se la sessione LibreBooking è ancora valida
            if (!jwtService.isLibreBookingSessionValid(token)) {
                log.warn("🚨 Sessione LibreBooking scaduta per utente: {}", claims.getUsername());
                return DashboardStatsDto.builder()
                        .success(false)
                        .message("Sessione scaduta. Effettua nuovamente il login.")
                        .build();
            }

            // Recupera tutte le prenotazioni dell'utente
            UserReservationsResponse reservationsResponse = reservationService.getReservationUser(request, token, "all");

            if (!reservationsResponse.getSuccess() || reservationsResponse.getReservations() == null) {
                log.warn("⚠️ Errore nel recupero delle prenotazioni per le statistiche");
                return DashboardStatsDto.builder()
                        .success(false)
                        .message("Errore nel recupero delle prenotazioni")
                        .build();
            }

            List<UserReservationDto> reservations = reservationsResponse.getReservations();
            log.info("✅ Recuperate {} prenotazioni per il calcolo statistiche", reservations.size());

            // Calcola statistiche
            DashboardStatsDto stats = calculateDashboardStats(reservations, period);
            stats.setSuccess(true);
            stats.setMessage("Statistiche calcolate con successo");
            stats.setLastUpdated(LocalDateTime.now());

            return stats;

        } catch (RuntimeException e) {
            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
            return DashboardStatsDto.builder()
                    .success(false)
                    .message("Errore autenticazione: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("❌ Errore generico calcolo statistiche dashboard", e);
            return DashboardStatsDto.builder()
                    .success(false)
                    .message("Errore interno: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Recupera il riepilogo delle attività dell'utente
     */
    public DashboardStatsDto getActivitySummary(HttpServletRequest request, String token) {
        return getDashboardStats(request, token, "thisMonth");
    }

    /**
     * Calcola le statistiche della dashboard dalle prenotazioni
     */
    private DashboardStatsDto calculateDashboardStats(List<UserReservationDto> reservations, String period) {
        LocalDateTime now = LocalDateTime.now();

        // Definisci i periodi
        LocalDateTime periodStart = getPeriodStart(period, now);
        LocalDateTime periodEnd = now;

        // Filtra prenotazioni per il periodo se necessario
        List<UserReservationDto> periodReservations = reservations;
        if (!"all".equals(period)) {
            periodReservations = reservations.stream()
                    .filter(r -> r.getStartDateTime().isAfter(periodStart) && r.getStartDateTime().isBefore(periodEnd))
                    .collect(Collectors.toList());
        }

        // Calcola statistiche base
        long activeBookings = reservations.stream()
                .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.CONFERMATA)
                .filter(r -> r.getStartDateTime().isAfter(now))
                .count();

        double totalHours = reservations.stream()
                .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.CONFERMATA ||
                        r.getStatus() == UserReservationDto.ReservationStatus.COMPLETATA)
                .mapToDouble(this::calculateDurationHours)
                .sum();

        long gymsUsed = reservations.stream()
                .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.CONFERMATA ||
                        r.getStatus() == UserReservationDto.ReservationStatus.COMPLETATA)
                .map(UserReservationDto::getResourceName)
                .distinct()
                .count();

        long upcomingBookings = reservations.stream()
                .filter(r -> r.getStartDateTime().isAfter(now))
                .count();

        // Statistiche dettagliate
        long confirmedBookings = reservations.stream()
                .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.CONFERMATA)
                .count();

        long pendingBookings = reservations.stream()
                .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.IN_ATTESA)
                .count();

        long completedBookings = reservations.stream()
                .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.COMPLETATA)
                .count();

        long cancelledBookings = reservations.stream()
                .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.CANCELLATA)
                .count();

        // Statistiche temporali
        LocalDateTime oneWeekAgo = now.minusWeeks(1);
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        LocalDateTime oneYearAgo = now.minusYears(1);

        long thisWeekBookings = reservations.stream()
                .filter(r -> r.getStartDateTime().isAfter(oneWeekAgo))
                .count();

        long thisMonthBookings = reservations.stream()
                .filter(r -> r.getStartDateTime().isAfter(oneMonthAgo))
                .count();

        long thisYearBookings = reservations.stream()
                .filter(r -> r.getStartDateTime().isAfter(oneYearAgo))
                .count();

        // Calcola palestre più utilizzate
        Map<String, Long> gymUsage = reservations.stream()
                .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.CONFERMATA ||
                        r.getStatus() == UserReservationDto.ReservationStatus.COMPLETATA)
                .collect(Collectors.groupingBy(
                        UserReservationDto::getResourceName,
                        Collectors.counting()
                ));

        List<Map.Entry<String, Long>> topGyms = gymUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        // Calcola slot orario preferito
        Map<String, Long> timeSlotUsage = reservations.stream()
                .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.CONFERMATA ||
                        r.getStatus() == UserReservationDto.ReservationStatus.COMPLETATA)
                .collect(Collectors.groupingBy(
                        this::getTimeSlot,
                        Collectors.counting()
                ));

        Map.Entry<String, Long> preferredTimeSlot = timeSlotUsage.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        // Calcola durata media sessioni
        double averageSessionDuration = reservations.stream()
                .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.CONFERMATA ||
                        r.getStatus() == UserReservationDto.ReservationStatus.COMPLETATA)
                .mapToDouble(this::calculateDurationHours)
                .average()
                .orElse(0.0);

        // Costruisci il DTO di risposta
        DashboardStatsDto.DashboardStatsDtoBuilder builder = DashboardStatsDto.builder()
                .activeBookings(activeBookings)
                .totalHours(Math.round(totalHours * 10.0) / 10.0) // Arrotonda a 1 decimale
                .gymsUsed(gymsUsed)
                .upcomingBookings(upcomingBookings)
                .confirmedBookings(confirmedBookings)
                .pendingBookings(pendingBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .thisWeekBookings(thisWeekBookings)
                .thisMonthBookings(thisMonthBookings)
                .thisYearBookings(thisYearBookings)
                .period(period)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .averageSessionDuration(Math.round(averageSessionDuration * 10.0) / 10.0);

        // Aggiungi top palestre
        if (topGyms.size() > 0) {
            builder.topGym1(topGyms.get(0).getKey())
                    .topGym1Count(topGyms.get(0).getValue());
        }
        if (topGyms.size() > 1) {
            builder.topGym2(topGyms.get(1).getKey())
                    .topGym2Count(topGyms.get(1).getValue());
        }
        if (topGyms.size() > 2) {
            builder.topGym3(topGyms.get(2).getKey())
                    .topGym3Count(topGyms.get(2).getValue());
        }

        // Aggiungi slot preferito
        if (preferredTimeSlot != null) {
            builder.preferredTimeSlot(preferredTimeSlot.getKey())
                    .preferredTimeSlotCount(preferredTimeSlot.getValue());
        }

        return builder.build();
    }

    /**
     * Calcola l'inizio del periodo basato sul tipo
     */
    private LocalDateTime getPeriodStart(String period, LocalDateTime now) {
        return switch (period.toLowerCase()) {
            case "thisweek" -> now.minusWeeks(1);
            case "thismonth" -> now.minusMonths(1);
            case "thisyear" -> now.minusYears(1);
            case "last3months" -> now.minusMonths(3);
            case "last6months" -> now.minusMonths(6);
            default -> now.minusMonths(1); // Default: ultimo mese
        };
    }

    /**
     * Calcola la durata in ore di una prenotazione
     */
    private double calculateDurationHours(UserReservationDto reservation) {
        if (reservation.getStartDateTime() == null || reservation.getEndDateTime() == null) {
            return 0.0;
        }

        long durationMinutes = java.time.Duration.between(
                reservation.getStartDateTime(),
                reservation.getEndDateTime()
        ).toMinutes();

        return durationMinutes / 60.0;
    }

    /**
     * Determina lo slot orario di una prenotazione
     */
    private String getTimeSlot(UserReservationDto reservation) {
        if (reservation.getStartDateTime() == null) {
            return "Unknown";
        }

        int hour = reservation.getStartDateTime().getHour();

        if (hour >= 6 && hour < 12) {
            return "06:00-12:00";
        } else if (hour >= 12 && hour < 18) {
            return "12:00-18:00";
        } else if (hour >= 18 && hour < 24) {
            return "18:00-24:00";
        } else {
            return "00:00-06:00";
        }
    }
}