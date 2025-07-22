package com.virtus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtus.config.LibreBookingProperties;
import com.virtus.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final WebClient webClient;
    private final LibreBookingProperties properties;
    private final JwtService jwtService;
   // private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
// Formatter con timezone per LibreBooking


    // Timezone di default (cambia secondo le tue necessità)
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Rome"); // +01:00/+02:00

    private static final DateTimeFormatter LIBREBOOKING_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    /**
     * Crea una nuova prenotazione
     */
    public ReservationCreatedDto createReservation(CreateReservationDto reservationData, HttpServletRequest request,String token) {
        log.info("📅 Creazione nuova prenotazione: {}", reservationData.getTitle());

        try {

            // Estrae le informazioni di sessione dal JWT
            JwtClaimsDto claims = jwtService.extractClaims(token);

            // Verifica se la sessione LibreBooking è ancora valida
            if (!jwtService.isLibreBookingSessionValid(token)) {
                log.warn("🚨 Sessione LibreBooking scaduta per utente: {}", claims.getUsername());
                return ReservationCreatedDto.builder()
                        .build();
            }
            UserInfoDto userInfo = UserInfoDto.builder()
                    .userId(claims.getUserId())
                    .username(claims.getUsername())
                    .firstName(claims.getFirstName())
                    .lastName(claims.getLastName())
                    .build();
            // Validazioni business logic
            validateReservationData(reservationData);

            // Prepara la richiesta per LibreBooking
            LibreBookingCreateReservationDto librebookingRequest = mapToLibreBookingRequest(reservationData, claims.getUserId());

            // Chiamata a LibreBooking
            String url = properties.getBaseUrl() + properties.getReservationsEndpoint();

            LibreBookingReservationResponseDto response = webClient.post()
                    .uri(url)
                    .header("X-Booked-SessionToken", claims.getSessionToken())
                    .header("X-Booked-UserId", claims.getLibreBookingUserId())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .bodyValue(librebookingRequest)
                    .retrieve()
                    .bodyToMono(LibreBookingReservationResponseDto.class)
                    .block();

            // Mappa la risposta per il frontend
            ReservationCreatedDto result = mapToFrontendResponse(response, reservationData);

            return result;

        } catch (RuntimeException e) {
            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Errore generico creazione prenotazione", e);
            throw new RuntimeException("Errore creazione prenotazione: " + e.getMessage(), e);
        }
    }


  public ReservationCreatedDto updateReservation(CreateReservationDto reservationData, HttpServletRequest request,String token, String reservationId) {
        log.info("📅 Update prenotazione: {}", reservationId);

        try {

            // Estrae le informazioni di sessione dal JWT
            JwtClaimsDto claims = jwtService.extractClaims(token);

            // Verifica se la sessione LibreBooking è ancora valida
            if (!jwtService.isLibreBookingSessionValid(token)) {
                log.warn("🚨 Sessione LibreBooking scaduta per utente: {}", claims.getUsername());
                return ReservationCreatedDto.builder()
                        .build();
            }
            UserInfoDto userInfo = UserInfoDto.builder()
                    .userId(claims.getUserId())
                    .username(claims.getUsername())
                    .firstName(claims.getFirstName())
                    .lastName(claims.getLastName())
                    .build();
            // Validazioni business logic
            validateReservationData(reservationData);

            // Prepara la richiesta per LibreBooking
            LibreBookingCreateReservationDto librebookingRequest = mapToLibreBookingRequest(reservationData, claims.getUserId());

            // Chiamata a LibreBooking
            String url = properties.getBaseUrl() + properties.getReservationsEndpoint() + reservationId;

            LibreBookingReservationResponseDto response = webClient.post()
                    .uri(url)
                    .header("X-Booked-SessionToken", claims.getSessionToken())
                    .header("X-Booked-UserId", claims.getLibreBookingUserId())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .bodyValue(librebookingRequest)
                    .retrieve()
                    .bodyToMono(LibreBookingReservationResponseDto.class)
                    .block();

            // Mappa la risposta per il frontend
            ReservationCreatedDto result = mapToFrontendResponse(response, reservationData);

            return result;

        } catch (RuntimeException e) {
            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Errore generico creazione prenotazione", e);
            throw new RuntimeException("Errore creazione prenotazione: " + e.getMessage(), e);
        }
    }

/*
    // Metodo updateReservation migliorato con gestione errori
    public ReservationCreatedDto updateReservation(CreateReservationDto reservationData,
                                                   HttpServletRequest request,
                                                   String token,
                                                   String reservationId) {
        log.info("📅 Update prenotazione: {}", reservationId);

        try {
            // Estrae le informazioni di sessione dal JWT
            JwtClaimsDto claims = jwtService.extractClaims(token);

            // Verifica se la sessione LibreBooking è ancora valida
            if (!jwtService.isLibreBookingSessionValid(token)) {
                log.warn("🚨 Sessione LibreBooking scaduta per utente: {}", claims.getUsername());
                return ReservationCreatedDto.builder()
                        .success(false)
                        .message("Sessione scaduta")
                        .build();
            }

            // Validazioni business logic
            validateReservationData(reservationData);

            // Prepara la richiesta per LibreBooking
            LibreBookingCreateReservationDto librebookingRequest = mapToLibreBookingRequest(reservationData, claims.getUserId());

            // CORREZIONE 1: URL corretto per l'update (usa PUT invece di POST)
            String url = properties.getBaseUrl() + properties.getReservationsEndpoint() +reservationId;

            log.debug("🌐 Chiamata PUT per update: {}", url);
            log.debug("📝 Payload: {}", librebookingRequest);

            try {
                // CORREZIONE 2: Gestione risposta più robusta
                String responseBody = webClient.post() // ← CAMBIATO DA POST A PUT
                        .uri(url)
                        .header("X-Booked-SessionToken", claims.getSessionToken())
                        .header("X-Booked-UserId", claims.getLibreBookingUserId())
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .bodyValue(librebookingRequest)
                        .retrieve()
                        .onStatus(
                                status -> status.is4xxClientError() || status.is5xxServerError(),
                                response -> response.bodyToMono(String.class)
                                        .map(body -> {
                                            log.error("❌ Errore HTTP {}: {}", response.statusCode(), body);
                                            return new RuntimeException("Errore server: " + response.statusCode() + " - " + body);
                                        })
                        )
                        .bodyToMono(String.class) // ← Prima recupera come String
                        .block();

                log.debug("📄 Risposta raw: {}", responseBody);

                // CORREZIONE 3: Verifica content type della risposta
                if (responseBody != null) {
                    // Prova a fare il parsing JSON manualmente
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        LibreBookingReservationResponseDto response = objectMapper.readValue(
                                responseBody, LibreBookingReservationResponseDto.class);

                        return mapToFrontendResponse(response, reservationData);

                    } catch (Exception jsonException) {
                        log.error("❌ Errore parsing JSON: {}", jsonException.getMessage());
                        log.error("📄 Contenuto risposta (non JSON): {}", responseBody);

                        // Se la risposta contiene HTML, probabilmente è una pagina di errore
                        if (responseBody.toLowerCase().contains("<html")) {
                            throw new RuntimeException("Server ha restituito una pagina HTML invece di JSON. Possibile errore di routing o configurazione.");
                        }

                        throw new RuntimeException("Risposta non valida dal server: " + jsonException.getMessage());
                    }
                } else {
                    throw new RuntimeException("Risposta vuota dal server");
                }

            } catch (WebClientResponseException e) {
                log.error("❌ Errore HTTP durante update: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new RuntimeException(getErrorMessage(e), e);
            }

        } catch (RuntimeException e) {
            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Errore generico update prenotazione", e);
            throw new RuntimeException("Errore update prenotazione: " + e.getMessage(), e);
        }
    }

    // CORREZIONE 4: Metodo alternativo con debugging avanzato
    public ReservationCreatedDto updateReservationWithDebug(CreateReservationDto reservationData,
                                                            HttpServletRequest request,
                                                            String token,
                                                            String reservationId) {
        log.info("📅 Update prenotazione con debug: {}", reservationId);

        try {
            JwtClaimsDto claims = jwtService.extractClaims(token);

            if (!jwtService.isLibreBookingSessionValid(token)) {
                log.warn("🚨 Sessione LibreBooking scaduta per utente: {}", claims.getUsername());
                return ReservationCreatedDto.builder()
                        .success(false)
                        .message("Sessione scaduta")
                        .build();
            }

            validateReservationData(reservationData);
            LibreBookingCreateReservationDto librebookingRequest = mapToLibreBookingRequest(reservationData, claims.getUserId());

            String url = properties.getBaseUrl() + properties.getReservationsEndpoint() +  reservationId;

            return webClient.post()
                    .uri(url)
                    .header("X-Booked-SessionToken", claims.getSessionToken())
                    .header("X-Booked-UserId", claims.getLibreBookingUserId())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .bodyValue(librebookingRequest)
                    .exchangeToMono(response -> {
                        // Debug completo della risposta
                        log.info("🔍 Status Code: {}", response.statusCode());
                        log.info("🔍 Headers: {}", response.headers().asHttpHeaders());

                        String contentType = response.headers().contentType()
                                .map(mediaType -> mediaType.toString())
                                .orElse("unknown");
                        log.info("🔍 Content-Type: {}", contentType);

                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.info("🔍 Response Body: {}", body);

                                    try {
                                        if (contentType.toLowerCase().contains("json")) {
                                            ObjectMapper objectMapper = new ObjectMapper();
                                            LibreBookingReservationResponseDto dto = objectMapper.readValue(
                                                    body, LibreBookingReservationResponseDto.class);
                                            return Mono.just(mapToFrontendResponse(dto, reservationData));
                                        } else {
                                            return Mono.error(new RuntimeException(
                                                    "Content-Type non supportato: " + contentType +
                                                            ". Corpo risposta: " + body.substring(0, Math.min(500, body.length()))));
                                        }
                                    } catch (Exception e) {
                                        return Mono.error(new RuntimeException("Errore parsing risposta: " + e.getMessage()));
                                    }
                                });
                    })
                    .block();

        } catch (Exception e) {
            log.error("❌ Errore update prenotazione con debug", e);
            throw new RuntimeException("Errore update prenotazione: " + e.getMessage(), e);
        }
    }

    // CORREZIONE 5: Aggiungi ObjectMapper come dependency se non presente
    @Autowired
    private ObjectMapper objectMapper; // Assicurati che sia configurato nel context
*/

    // Aggiungi questo metodo alla classe ReservationService esistente

    /**
     * Recupera le prenotazioni dell'utente con filtri
     */
    public UserReservationsResponse getReservationUser(HttpServletRequest request, String token, String options) {
        try {
            log.info("📅 Recupero prenotazioni utente con opzioni: {}", options);

            // Estrae le informazioni di sessione dal JWT
            JwtClaimsDto claims = jwtService.extractClaims(token);

            // Verifica se la sessione LibreBooking è ancora valida
            if (!jwtService.isLibreBookingSessionValid(token)) {
                log.warn("🚨 Sessione LibreBooking scaduta per utente: {}", claims.getUsername());
                return UserReservationsResponse.builder()
                        .success(false)
                        .message("Sessione scaduta")
                        .reservations(Collections.emptyList())
                        .totalCount(0)
                        .build();
            }

            // Recupera prenotazioni da LibreBooking
            List<LibreBookingFullReservationDto> librebookingReservations = getFullReservations(claims, options);

            // Trasforma in formato per il frontend
            List<UserReservationDto> userReservations = librebookingReservations.stream()
                    .map(this::mapToUserReservation)
                    .collect(Collectors.toList());

            // Calcola statistiche
            long confirmedCount = userReservations.stream()
                    .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.CONFERMATA)
                    .count();

            long pendingCount = userReservations.stream()
                    .filter(r -> r.getStatus() == UserReservationDto.ReservationStatus.IN_ATTESA)
                    .count();

            long upcomingCount = userReservations.stream()
                    .filter(r -> r.getStartDateTime().isAfter(LocalDateTime.now()))
                    .count();

            return UserReservationsResponse.builder()
                    .success(true)
                    .reservations(userReservations)
                    .totalCount(userReservations.size())
                    .confirmedCount(confirmedCount)
                    .pendingCount(pendingCount)
                    .upcomingCount(upcomingCount)
                    .build();

        } catch (RuntimeException e) {
            log.error("❌ Errore JWT/autenticazione: {}", e.getMessage());
            return UserReservationsResponse.builder()
                    .success(false)
                    .message("Errore autenticazione: " + e.getMessage())
                    .reservations(Collections.emptyList())
                    .totalCount(0)
                    .build();
        } catch (Exception e) {
            log.error("❌ Errore generico recupero prenotazioni", e);
            return UserReservationsResponse.builder()
                    .success(false)
                    .message("Errore recupero prenotazioni: " + e.getMessage())
                    .reservations(Collections.emptyList())
                    .totalCount(0)
                    .build();
        }
    }

    /**
     * Recupera le prenotazioni complete da LibreBooking
     */
    private List<LibreBookingFullReservationDto> getFullReservations(JwtClaimsDto claimsDto, String options) {
        // Calcola date di default (prossime 4 settimane se non specificate)
        LocalDateTime startDate = getMondayOfCurrentWeek();
        LocalDateTime endDate = startDate.plusWeeks(4);

        // Costruisci URL con parametri
        String url = String.format("%s%s?userId=%s&startDateTime=%s&endDateTime=%s",
                properties.getBaseUrl(),
                properties.getReservationsEndpoint(),
                claimsDto.getUserId(), // AGGIUNTO userId alla chiamata
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        log.debug("🌐 Chiamata LibreBooking: {}", url);

        try {
            LibreBookingFullReservationsResponse response = webClient.get()
                    .uri(url)
                    .header("X-Booked-SessionToken", claimsDto.getSessionToken())
                    .header("X-Booked-UserId", claimsDto.getLibreBookingUserId())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToMono(LibreBookingFullReservationsResponse.class)
                    .block();



            if (response != null && response.getReservations() != null) {
                log.info("✅ Recuperate {} prenotazioni da LibreBooking", response.getReservations().size());
                return response.getReservations();
            } else {
                log.warn("⚠️ Risposta vuota da LibreBooking");
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("❌ Errore LibreBooking: {}", e.getMessage());
            throw new RuntimeException("Errore comunicazione LibreBooking: " + e.getMessage(), e);
        }
    }



    public LibreBookingReservationResponseDto deleteReservation(String reservationId,String token){
        // Estrae le informazioni di sessione dal JWT
        JwtClaimsDto claims = jwtService.extractClaims(token);

        // Verifica se la sessione LibreBooking è ancora valida
        if (!jwtService.isLibreBookingSessionValid(token)) {
            log.warn("🚨 Sessione LibreBooking scaduta per utente: {}", claims.getUsername());
            return LibreBookingReservationResponseDto.builder()
                    .message("Sessione scaduta")
                    .build();
        }

        // Chiamata a LibreBooking
        String url = properties.getBaseUrl() + properties.getReservationsEndpoint()+ "/" + reservationId ;
        try {
            LibreBookingReservationResponseDto response = webClient.delete()
                    .uri(url)
                    .header("X-Booked-SessionToken", claims.getSessionToken())
                    .header("X-Booked-UserId", claims.getLibreBookingUserId())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToMono(LibreBookingReservationResponseDto.class)
                    .block();
            if (response != null ) {
                log.info("✅ delete {} prenotazioni da LibreBooking", reservationId);
                return response;
            } else {
                log.warn("⚠️ Risposta vuota da LibreBooking");
                return null;
            }
        }catch (Exception e) {
            log.error("❌ Errore LibreBooking: {}", e.getMessage());
            throw new RuntimeException("Errore comunicazione LibreBooking: " + e.getMessage(), e);
        }
    }
    /**
     * Trasforma una prenotazione LibreBooking in formato frontend
     */
    private UserReservationDto mapToUserReservation(LibreBookingFullReservationDto librebookingReservation) {
        try {
            // Parse delle date
            LocalDateTime startDateTime = parseLibreBookingDate(librebookingReservation.getStartDate()).toLocalDateTime();
            LocalDateTime endDateTime = parseLibreBookingDate(librebookingReservation.getEndDate()).toLocalDateTime();

            // Determina lo stato
            UserReservationDto.ReservationStatus status = determineReservationStatus(
                    librebookingReservation, startDateTime, endDateTime);

            // Calcola permessi
            boolean canModify = canModifyReservation(librebookingReservation, startDateTime);
            boolean canCancel = canCancelReservation(librebookingReservation, startDateTime);

            // Formatta date per display
            String formattedDate = formatDateForDisplay(startDateTime);
            String formattedTimeRange = formatTimeRange(startDateTime, endDateTime);

            // Determina tipo di prenotazione
            String tipo = determineReservationType(librebookingReservation.getTitle(),
                    librebookingReservation.getDescription());

            return UserReservationDto.builder()
                    .referenceNumber(librebookingReservation.getReferenceNumber())
                    .resourceId(librebookingReservation.getResourceId())
                    .resourceName(librebookingReservation.getResourceName())
                    .startDateTime(startDateTime)
                    .endDateTime(endDateTime)
                    .duration(librebookingReservation.getDuration())
                    .formattedDate(formattedDate)
                    .formattedTimeRange(formattedTimeRange)
                    .title(librebookingReservation.getTitle())
                    .description(librebookingReservation.getDescription())
                    .tipo(tipo)
                    .status(status)
                    .isPendingApproval(librebookingReservation.getRequiresApproval())
                    .isRecurring(librebookingReservation.getIsRecurring())
                    .canModify(canModify)
                    .canCancel(canCancel)
                    .canCheckIn(librebookingReservation.getIsCheckInEnabled() != null &&
                            librebookingReservation.getIsCheckInEnabled())
                    .canCheckOut(librebookingReservation.getCheckInDate() != null)
                    .color(librebookingReservation.getColor())
                    .textColor(librebookingReservation.getTextColor())
                    .participants(librebookingReservation.getParticipants())
                    .invitees(librebookingReservation.getInvitees())
                    .participantCount(calculateParticipantCount(librebookingReservation))
                    .checkInDate(parseLibreBookingDateSafe(librebookingReservation.getCheckInDate()))
                    .checkOutDate(parseLibreBookingDateSafe(librebookingReservation.getCheckOutDate()))
                    .creditsConsumed(librebookingReservation.getCreditsConsumed())
                    .build();

        } catch (Exception e) {
            log.error("❌ Errore mapping prenotazione: {}", e.getMessage());
            throw new RuntimeException("Errore trasformazione prenotazione", e);
        }
    }

    /**
     * Parse di una data LibreBooking (con timezone)
     */
    private ZonedDateTime parseLibreBookingDate(String dateString) {


            if (dateString == null) return null;

            try {
                // Parsing della data con fuso orario incluso (es. +0000)
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateString, LIBREBOOKING_FORMATTER);
                // Converte in zona di default (Europe/Rome)
                return offsetDateTime.atZoneSameInstant(DEFAULT_ZONE);
            } catch (Exception e) {
                return null;

        }
    }

    /**
     * Parse sicuro per date opzionali
     */
    private LocalDateTime parseLibreBookingDateSafe(String dateString) {
        try {
            return parseLibreBookingDate(dateString).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Determina lo stato della prenotazione
     */
    private UserReservationDto.ReservationStatus determineReservationStatus(
            LibreBookingFullReservationDto reservation,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {

        LocalDateTime now = LocalDateTime.now();

        // Se in attesa di approvazione
        if (Boolean.TRUE.equals(reservation.getRequiresApproval())) {
            return UserReservationDto.ReservationStatus.IN_ATTESA;
        }

        // Se già terminata
        if (endDateTime != null && endDateTime.isBefore(now)) {
            return UserReservationDto.ReservationStatus.COMPLETATA;
        }

        // Se confermata (default)
        return UserReservationDto.ReservationStatus.CONFERMATA;
    }

    /**
     * Verifica se può modificare la prenotazione
     */
    private boolean canModifyReservation(LibreBookingFullReservationDto reservation, LocalDateTime startDateTime) {
        if (startDateTime == null) return false;

        // Non può modificare se già iniziata o in attesa di approvazione
        LocalDateTime now = LocalDateTime.now();
        boolean notStarted = startDateTime.isAfter(now.plusHours(1)); // Almeno 1 ora prima
        boolean notPending = !Boolean.TRUE.equals(reservation.getRequiresApproval());

        return notStarted && notPending;
    }

    /**
     * Verifica se può cancellare la prenotazione
     */
    private boolean canCancelReservation(LibreBookingFullReservationDto reservation, LocalDateTime startDateTime) {
        if (startDateTime == null) return false;

        // Può cancellare se non è ancora iniziata
        LocalDateTime now = LocalDateTime.now();
        return startDateTime.isAfter(now.plusMinutes(30)); // Almeno 30 min prima
    }

    /**
     * Formatta la data per il display
     */
    private String formatDateForDisplay(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        LocalDate today = LocalDate.now();
        LocalDate reservationDate = dateTime.toLocalDate();

        if (reservationDate.equals(today)) {
            return "Oggi";
        } else if (reservationDate.equals(today.plusDays(1))) {
            return "Domani";
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("dd MMM", java.util.Locale.ITALIAN));
        }
    }

    /**
     * Formatta l'orario per il display
     */
    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "";

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        return start.format(timeFormatter) + " - " + end.format(timeFormatter);
    }

    /**
     * Determina il tipo di prenotazione dal titolo/descrizione
     */
    private String determineReservationType(String title, String description) {
        if (title == null) title = "";
        if (description == null) description = "";

        String combined = (title + " " + description).toLowerCase();

        if (combined.contains("allenamento")) return "Allenamento";
        if (combined.contains("partita")) return "Partita";
        if (combined.contains("torneo")) return "Torneo";
        if (combined.contains("amichevole")) return "Amichevole";

        return title.isEmpty() ? "Prenotazione" : title;
    }

    /**
     * Calcola il numero totale di partecipanti
     */
    private Integer calculateParticipantCount(LibreBookingFullReservationDto reservation) {
        int count = 0;

        if (reservation.getParticipants() != null) {
            count += reservation.getParticipants().size();
        }

        if (reservation.getParticipatingGuests() != null) {
            count += reservation.getParticipatingGuests().size();
        }

        return count > 0 ? count : null;
    }

    /**
     * Valida i dati della prenotazione
     */
    private void validateReservationData(CreateReservationDto data) {
        // Verifica che la data di fine sia dopo quella di inizio
        if (data.getEndDateTime().isBefore(data.getStartDateTime()) ||
                data.getEndDateTime().isEqual(data.getStartDateTime())) {
            throw new IllegalArgumentException("La data di fine deve essere posteriore a quella di inizio");
        }

        // Verifica durata massima (es. 8 ore)
        long hours = java.time.Duration.between(data.getStartDateTime(), data.getEndDateTime()).toHours();
        if (hours > 8) {
            throw new IllegalArgumentException("La prenotazione non può durare più di 8 ore");
        }

        // Verifica che non sia troppo nel futuro (es. max 6 mesi)
        if (data.getStartDateTime().isAfter(java.time.LocalDateTime.now().plusMonths(6))) {
            throw new IllegalArgumentException("Non è possibile prenotare oltre 6 mesi nel futuro");
        }

        log.debug("✅ Validazione prenotazione completata");
    }

    /**
     * Mappa i dati frontend alla richiesta LibreBooking
     */
    private LibreBookingCreateReservationDto mapToLibreBookingRequest(CreateReservationDto frontendData, Integer userId) {

        // Converti date con timezone
        String startDateTimeFormatted = frontendData.getStartDateTime()
                .atZone(DEFAULT_ZONE)
                .format(LIBREBOOKING_FORMATTER);

        String endDateTimeFormatted = frontendData.getEndDateTime()
                .atZone(DEFAULT_ZONE)
                .format(LIBREBOOKING_FORMATTER);


        LibreBookingCreateReservationDto.LibreBookingCreateReservationDtoBuilder builder =
                LibreBookingCreateReservationDto.builder()
                        // CAMPI OBBLIGATORI
                        .resourceId(frontendData.getResourceId())  // ← Ora è Integer, non String
                        .startDateTime(startDateTimeFormatted)  // ← Con timezone
                        .endDateTime(endDateTimeFormatted)      // ← Con timezone
                        .title(frontendData.getTitle())
                        .userId(userId)

                        // CAMPI OPZIONALI - Solo se presenti
                        .description(frontendData.getDescription())
                        .termsAccepted(frontendData.getTermsAccepted() != null ? frontendData.getTermsAccepted() : true);

        // Aggiungi solo liste non vuote
        if (frontendData.getParticipants() != null && !frontendData.getParticipants().isEmpty()) {
            builder.participants(frontendData.getParticipants());
        }

        if (frontendData.getParticipatingGuests() != null && !frontendData.getParticipatingGuests().isEmpty()) {
            builder.participatingGuests(frontendData.getParticipatingGuests());
        }

        if (frontendData.getInvitees() != null && !frontendData.getInvitees().isEmpty()) {
            builder.invitees(frontendData.getInvitees());
        }

        if (frontendData.getInvitedGuests() != null && !frontendData.getInvitedGuests().isEmpty()) {
            builder.invitedGuests(frontendData.getInvitedGuests());
        }

        if (frontendData.getRecurrenceRule() != null) {
            builder.recurrenceRule(frontendData.getRecurrenceRule());
        }

        if (frontendData.getAllowParticipation() != null) {
            builder.allowParticipation(frontendData.getAllowParticipation());
        }


        return builder.build();
    }

    /**
     * Mappa la risposta LibreBooking per il frontend
     */
    private ReservationCreatedDto mapToFrontendResponse(LibreBookingReservationResponseDto librebookingResponse,
                                                        CreateReservationDto originalData) {
        return ReservationCreatedDto.builder()
                .referenceNumber(librebookingResponse.getReferenceNumber())
                .isPendingApproval(librebookingResponse.getIsPendingApproval())
                .message(librebookingResponse.getMessage())
                .success(true)

                // Aggiungi info dalla richiesta originale per comodità del frontend
                .startDateTime(originalData.getStartDateTime())
                .endDateTime(originalData.getEndDateTime())
                .title(originalData.getTitle())
                .resourceId(originalData.getResourceId())
                .build();
    }

    /**
     * Converte gli errori HTTP in messaggi user-friendly
     */
    private String getErrorMessage(WebClientResponseException e) {
        return switch (e.getStatusCode().value()) {
            case 400 -> "Dati prenotazione non validi";
            case 401 -> "Sessione scaduta, effettua il login";
            case 403 -> "Non hai i permessi per creare questa prenotazione";
            case 404 -> "Risorsa non trovata";
            case 409 -> "Conflitto: la risorsa potrebbe essere già prenotata in questo orario";
            case 422 -> "Dati prenotazione non accettabili";
            case 500 -> "Errore interno del server di prenotazione";
            default -> "Errore durante la creazione della prenotazione";
        };
    }

    private List<LibreBookingReservationDto> getReservations(JwtClaimsDto claimsDto, long weeks) {
        LocalDateTime startDate = getMondayOfCurrentWeek();
        LocalDateTime endDate = startDate.plusWeeks(weeks);


        String url = String.format("%s%s?startDateTime=%s&endDateTime=%s&userid=%s",
                properties.getBaseUrl(),
                properties.getReservationsEndpoint(),

                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                claimsDto.getUserId()
        );


        try {
            LibreBookingReservationsResponse response = webClient.get()
                    .uri(url)
                    .header("X-Booked-SessionToken", claimsDto.getSessionToken())
                    .header("X-Booked-UserId", claimsDto.getLibreBookingUserId())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToMono(LibreBookingReservationsResponse.class)
                    .block();

            return response != null ? response.getReservations() : List.of();


        } catch (Exception e) {
            log.error("❌ Errore LibreBooking: {}", e.getMessage());
            throw new RuntimeException("Errore comunicazione LibreBooking: " + e.getMessage(), e);
        }
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