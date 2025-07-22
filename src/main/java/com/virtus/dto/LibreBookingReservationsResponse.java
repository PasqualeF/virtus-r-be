package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LibreBookingReservationsResponse {
    private List<LibreBookingReservationDto> reservations;
    private String startDateTime;
    private String endDateTime;
    private String message;
}