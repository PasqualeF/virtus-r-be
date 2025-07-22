package com.virtus.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

// DTO per la richiesta dal frontend (semplificato)
@Data
@Builder
public class CreateReservationDto {

    @NotNull(message = "La risorsa è obbligatoria")
    @Positive(message = "ID risorsa deve essere positivo")
    private Integer resourceId;

    @NotNull(message = "Data/ora inizio è obbligatoria")
    @Future(message = "La data deve essere futura")
    private LocalDateTime startDateTime;

    @NotNull(message = "Data/ora fine è obbligatoria")
    @Future(message = "La data deve essere futura")
    private LocalDateTime endDateTime;

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 200, message = "Il titolo non può superare 200 caratteri")
    private String title;

    @Size(max = 1000, message = "La descrizione non può superare 1000 caratteri")
    private String description;

    // Campi opzionali
    private List<Integer> participants;
    private List<String> participatingGuests;
    private List<Integer> invitees;
    private List<String> invitedGuests;
    private RecurrenceRuleDto recurrenceRule;
    private Boolean allowParticipation;
    private Boolean termsAccepted;
}

