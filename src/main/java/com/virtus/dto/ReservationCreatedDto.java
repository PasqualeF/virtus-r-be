package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// DTO per la risposta al frontend
@Data
@Builder
public class ReservationCreatedDto {
    private String referenceNumber;
    private Boolean isPendingApproval;
    private String message;
    private Boolean success;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String title;
    private Integer resourceId;
}
