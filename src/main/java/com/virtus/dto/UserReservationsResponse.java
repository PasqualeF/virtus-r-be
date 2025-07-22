package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserReservationsResponse {
    private List<UserReservationDto> reservations;
    private Integer totalCount;
    private String startDateTime;
    private String endDateTime;
    private Boolean success;
    private String message;

    // Statistiche per il frontend
    private Long confirmedCount;
    private Long pendingCount;
    private Long upcomingCount;
}