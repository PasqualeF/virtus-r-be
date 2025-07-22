package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LibreBookingFullReservationsResponse {
    private List<Object> links;
    private String message;
    private List<LibreBookingFullReservationDto> reservations;
    private String startDateTime;
    private String endDateTime;
}