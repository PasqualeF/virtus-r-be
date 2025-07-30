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
import java.time.format.DateTimeFormatter;
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

            // 2. Recupera reservations
            List<LibreBookingReservationDto> reservations = getReservations(auth,2,null);
            // 3. Trasforma in formato Angular
            return reservations.stream()
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
            List<LibreBookingReservationDto> reservations = getReservations(auth,4,nomePalestra);
            // 3. Trasforma in formato Angular
            return reservations.stream()
                    .map(this::mapToOrarioAllenamento)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Errore recupero da LibreBooking", e);
            return getFallbackData();
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
        LocalDateTime startDateTime = LocalDateTime.parse(
                reservation.getStartDate().replace("+0000", "")
        );
        LocalDateTime endDateTime = LocalDateTime.parse(
                reservation.getEndDate().replace("+0000", "")
        );

        return OrarioAllenamentoDto.builder()
                .gruppo(reservation.getFirstName() + " " + reservation.getLastName())
                .giorno(getGiornoSettimana(startDateTime))
                .orario(formatOrario(startDateTime, endDateTime))
                .palestra(reservation.getResourceName())
                .palestraId(reservation.getResourceId())
                .referenceNumber(reservation.getReferenceNumber())
                .title(reservation.getTitle())
                .description(reservation.getDescription())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .isRecurring(reservation.isRecurring())
                .build();
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