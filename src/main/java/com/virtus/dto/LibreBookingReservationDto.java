package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LibreBookingReservationDto {
    private String referenceNumber;
    private String startDate;
    private String endDate;
    private String firstName;
    private String lastName;
    private String resourceName;
    private String title;
    private String description;
    private boolean isRecurring;
    private String resourceId;
    private String userId;
    private String duration;
    private String color;
}
