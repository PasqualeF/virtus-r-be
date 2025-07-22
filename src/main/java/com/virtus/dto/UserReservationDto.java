package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserReservationDto {
    // Identificativi
    private String referenceNumber;
    private Integer resourceId;
    private String resourceName;

    // Date e orari
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String duration;
    private String formattedDate; // "Oggi", "Domani", "15 Gen"
    private String formattedTimeRange; // "18:00 - 20:00"

    // Dettagli prenotazione
    private String title;
    private String description;
    private String tipo; // Derivato da title o description

    // Stato
    private ReservationStatus status;
    private Boolean isPendingApproval;
    private Boolean isRecurring;

    // Permessi utente
    private Boolean canModify;
    private Boolean canCancel;
    private Boolean canCheckIn;
    private Boolean canCheckOut;

    // Colori per UI
    private String color;
    private String textColor;

    // Partecipanti
    private List<String> participants;
    private List<String> invitees;
    private Integer participantCount;

    // Check-in/out
    private LocalDateTime checkInDate;
    private LocalDateTime checkOutDate;

    // Crediti e costi
    private Integer creditsConsumed;

    // Enum per lo stato
    public enum ReservationStatus {
        CONFERMATA("confermata"),
        IN_ATTESA("in-attesa"),
        COMPLETATA("completata"),
        CANCELLATA("cancellata");

        private final String value;

        ReservationStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}