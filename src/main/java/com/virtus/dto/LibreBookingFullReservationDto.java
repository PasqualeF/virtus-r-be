package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LibreBookingFullReservationDto {
    private String referenceNumber;
    private String startDate;
    private String endDate;
    private String firstName;
    private String lastName;
    private String resourceName;
    private String title;
    private String description;
    private Boolean requiresApproval;
    private Boolean isRecurring;
    private Integer scheduleId;
    private Integer userId;
    private Integer resourceId;
    private String duration;
    private String bufferTime;
    private String bufferedStartDate;
    private String bufferedEndDate;
    private List<String> participants;
    private List<String> invitees;
    private List<String> participatingGuests;
    private List<String> invitedGuests;
    private Integer startReminder;
    private Integer endReminder;
    private String color;
    private String textColor;
    private String checkInDate;
    private String checkOutDate;
    private String originalEndDate;
    private Boolean isCheckInEnabled;
    private Integer autoReleaseMinutes;
    private Integer resourceStatusId;
    private Integer creditsConsumed;

    // Links e message opzionali
    private List<Object> links;
    private String message;
}