package com.virtus.service;

import com.virtus.config.LibreBookingProperties;
import com.virtus.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrariAllenamentiService {

    private final WebClient webClient;
    private final LibreBookingProperties properties;

    // Cache in memoria (no database)
    private LibreBookingAuthResponse currentAuth;
    private LocalDateTime lastAuthTime;

    @Cacheable(value = "orari-allenamenti", key = "'all'")
    public List<OrarioAllenamentoDto> getOrariAllenamenti() {

        try {
            // 1. Autentica
            LibreBookingAuthResponse auth = authenticate();

            // 2. Recupera reservations (sempre 2 settimane per sicurezza)
            List<LibreBookingReservationDto> reservations = getReservations(auth, 2, null);

            // 3. Filtra per la settimana target e trasforma in formato Angular
            return reservations.stream()
                    .filter(this::isInTargetWeek)
                    .map(this::mapToOrarioAllenamento)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Errore recupero da LibreBooking", e);
            return getFallbackData();
        }
    }

    @Cacheable(value = "prenotazioni-per-palestra", key = "'all'")
    public List<OrarioAllenamentoDto> getPrenotazioniForPalestra(String nomePalestra) {

        try {
            // 1. Autentica
            LibreBookingAuthResponse auth = authenticate();

            // 2. Recupera reservations
            List<LibreBookingReservationDto> reservations = getReservations(auth, 4, nomePalestra);

            // 3. Filtra per la settimana target e trasforma in formato Angular
            return reservations.stream()
                    .filter(this::isInTargetWeek)
                    .map(this::mapToOrarioAllenamento)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Errore recupero da LibreBooking", e);
            return getFallbackData();
        }
    }

    /**
     * Determina se una prenotazione appartiene alla settimana target
     * Logica:
     * - Se oggi è Lunedì-Sabato: restituisce la settimana corrente
     * - Se oggi è Domenica dopo le 14:00: restituisce la settimana successiva
     * - Se oggi è Domenica prima delle 14:00: restituisce la settimana corrente
     */
    private boolean isInTargetWeek(LibreBookingReservationDto reservation) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetWeekStart = getTargetWeekStart(now);
        LocalDateTime targetWeekEnd = targetWeekStart.plusDays(6).withHour(23).withMinute(59).withSecond(59);

        // Usa la conversione del fuso orario per la data della prenotazione
        LocalDateTime reservationStart = convertToLocalTimezone(reservation.getStartDate());

        boolean isInWeek = !reservationStart.isBefore(targetWeekStart) &&
                !reservationStart.isAfter(targetWeekEnd);

        log.debug("🔍 Reservation {} - Start: {} | Target week: {} to {} | In week: {}",
                reservation.getTitle(),
                reservationStart.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy HH:mm")),
                targetWeekStart.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy")),
                targetWeekEnd.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy")),
                isInWeek);

        return isInWeek;
    }

    /**
     * Calcola l'inizio della settimana target basandosi sulla logica richiesta
     */
    private LocalDateTime getTargetWeekStart(LocalDateTime now) {
        DayOfWeek currentDay = now.getDayOfWeek();

        if (currentDay == DayOfWeek.SUNDAY && now.getHour() >= 14) {
            // Domenica dopo le 14:00 -> settimana successiva
            LocalDateTime nextMonday = now.plusDays(1)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            log.debug("📅 Domenica pomeriggio -> Settimana SUCCESSIVA da: {}",
                    nextMonday.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy")));
            return nextMonday;
        } else {
            // Tutti gli altri casi -> settimana corrente
            int daysToSubtract = currentDay.getValue() - DayOfWeek.MONDAY.getValue();
            LocalDateTime currentMonday = now.minusDays(daysToSubtract)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            log.debug("📅 Settimana CORRENTE da: {}",
                    currentMonday.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy")));
            return currentMonday;
        }
    }

    private LibreBookingAuthResponse authenticate() {
        if (isAuthValid()) {
            return currentAuth;
        }

        LibreBookingAuthRequest request = LibreBookingAuthRequest.builder()
                .username(properties.getCredentials().getUsername())
                .password(properties.getCredentials().getPassword())
                .build();

        LibreBookingAuthResponse response = webClient.post()
                .uri(properties.getBaseUrl() + properties.getAuthEndpoint())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LibreBookingAuthResponse.class)
                .block();

        currentAuth = response;
        lastAuthTime = LocalDateTime.now();

        return response;
    }

    private List<LibreBookingReservationDto> getReservations(LibreBookingAuthResponse auth, long weeks, String resourceNameFilter) {
        LocalDateTime startDate = getMondayOfCurrentWeek();
        LocalDateTime endDate = startDate.plusWeeks(weeks);

        String url = String.format("%s%s?startDateTime=%s&endDateTime=%s",
                properties.getBaseUrl(),
                properties.getReservationsEndpoint(),
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        try {
            LibreBookingReservationsResponse response = webClient.get()
                    .uri(url)
                    .header("X-Booked-SessionToken", auth.getSessionToken())
                    .header("X-Booked-UserId", auth.getUserId())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToMono(LibreBookingReservationsResponse.class)
                    .block();

            List<LibreBookingReservationDto> allReservations = response != null ? response.getReservations() : List.of();
            allReservations = allReservations.stream().filter(res -> !res.isRequiresApproval()).toList();

            // 🔍 Filtro per resourceName se specificato
            if (resourceNameFilter != null && !resourceNameFilter.isBlank()) {
                return allReservations.stream()
                        .filter(res -> resourceNameFilter.equalsIgnoreCase(res.getResourceName()))
                        .toList();
            }

            return allReservations;

        } catch (Exception e) {
            log.error("❌ Errore LibreBooking: {}", e.getMessage());
            throw new RuntimeException("Errore comunicazione LibreBooking: " + e.getMessage(), e);
        }
    }

    private OrarioAllenamentoDto mapToOrarioAllenamento(LibreBookingReservationDto reservation) {
        // Converti le date dal fuso orario dell'API al fuso orario locale (+2)
        LocalDateTime startDateTime = convertToLocalTimezone(reservation.getStartDate());
        LocalDateTime endDateTime = convertToLocalTimezone(reservation.getEndDate());

        log.debug("🕐 Conversione orario - Original: {} -> Local: {}",
                reservation.getStartDate(),
                startDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        return OrarioAllenamentoDto.builder()
                .gruppo(reservation.getFirstName() + " " + reservation.getLastName())
                .giorno(getGiornoSettimana(startDateTime))
                .orario(formatOrario(startDateTime, endDateTime))
                .palestra(reservation.getResourceName())
                .palestraId(reservation.getResourceId())
                .referenceNumber(reservation.getReferenceNumber())
                .title(reservation.getTitle())
                .description(reservation.getDescription())
                .startDate(startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .endDate(endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .isRecurring(reservation.isRecurring())
                .build();
    }

    /**
     * Converte una data/ora dal formato dell'API al fuso orario locale (+2)
     * Gestisce diversi formati di input e fusi orari
     */
    private LocalDateTime convertToLocalTimezone(String dateTimeString) {
        try {
            // Rimuovi il fuso orario se presente per fare un parsing pulito
            String cleanDateTime = dateTimeString.replaceAll("[+-]\\d{4}$", "").trim();

            // Parsing della data come LocalDateTime
            LocalDateTime parsedDateTime;
            try {
                parsedDateTime = LocalDateTime.parse(cleanDateTime);
            } catch (DateTimeParseException e) {
                // Prova con formato alternativo se il parsing standard fallisce
                parsedDateTime = LocalDateTime.parse(cleanDateTime,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            // Determina il fuso orario di origine dall'API
            ZoneId sourceZone;
            if (dateTimeString.contains("+0000") || dateTimeString.endsWith("Z")) {
                sourceZone = ZoneId.of("UTC");
            } else if (dateTimeString.contains("+0100")) {
                sourceZone = ZoneId.of("+01:00");
            } else if (dateTimeString.contains("+0200")) {
                sourceZone = ZoneId.of("+02:00");
            } else {
                // Default: assumiamo UTC se non specificato
                sourceZone = ZoneId.of("UTC");
                log.warn("⚠️ Fuso orario non riconosciuto in '{}', assumo UTC", dateTimeString);
            }

            // Fuso orario target (Italia/Roma = +2 in estate, +1 in inverno)
            ZoneId targetZone = ZoneId.of("Europe/Rome");

            // Converti al fuso orario target
            ZonedDateTime sourceZonedDateTime = parsedDateTime.atZone(sourceZone);
            ZonedDateTime targetZonedDateTime = sourceZonedDateTime.withZoneSameInstant(targetZone);

            LocalDateTime result = targetZonedDateTime.toLocalDateTime();

            log.debug("🌍 Timezone conversion: {} ({}) -> {} ({})",
                    parsedDateTime, sourceZone, result, targetZone);

            return result;

        } catch (Exception e) {
            log.error("❌ Errore conversione timezone per '{}': {}", dateTimeString, e.getMessage());
            // Fallback: parsing semplice senza conversione
            return LocalDateTime.parse(dateTimeString.replace("+0000", "").trim());
        }
    }

    private boolean isAuthValid() {
        return currentAuth != null &&
                lastAuthTime != null &&
                lastAuthTime.plusMinutes(30).isAfter(LocalDateTime.now());
    }

    private String getGiornoSettimana(LocalDateTime dateTime) {
        String[] giorni = {"Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato"};
        return giorni[dateTime.getDayOfWeek().getValue() % 7];
    }

    private String formatOrario(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return start.format(formatter) + "-" + end.format(formatter);
    }

    private List<OrarioAllenamentoDto> getFallbackData() {
        return List.of(
                OrarioAllenamentoDto.builder()
                        .gruppo("Under 19")
                        .giorno("Lunedì")
                        .orario("18:00-20:00")
                        .palestra("Palestra A")
                        .build()
                // ... altri dati fallback
        );
    }

    @CacheEvict(value = "orari-allenamenti", allEntries = true)
    public List<OrarioAllenamentoDto> refreshOrariAllenamenti() {
        log.warn("🔄 CACHE EVICT - Forzando refresh...");
        return getOrariAllenamenti();
    }

    /**
     * Ottiene il lunedì della settimana corrente alle 00:00
     */
    private LocalDateTime getMondayOfCurrentWeek() {
        LocalDateTime now = LocalDateTime.now();

        // Trova il lunedì della settimana corrente
        DayOfWeek currentDay = now.getDayOfWeek();
        int daysToSubtract = currentDay.getValue() - DayOfWeek.MONDAY.getValue();

        LocalDateTime monday = now.minusDays(daysToSubtract)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        log.debug("📅 Lunedì settimana corrente: {}", monday.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy HH:mm")));

        return monday;
    }
}