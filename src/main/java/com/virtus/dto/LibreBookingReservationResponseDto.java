package com.virtus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO per la risposta da LibreBooking
@Data
@Builder
@NoArgsConstructor  // ← NECESSARIO per Jackson
@AllArgsConstructor // ← Per Builder pattern
@JsonIgnoreProperties(ignoreUnknown = true)
public class LibreBookingReservationResponseDto {
    private List<LinkDto> links;
    private String message;
    private String referenceNumber;
    private Boolean isPendingApproval;
}
