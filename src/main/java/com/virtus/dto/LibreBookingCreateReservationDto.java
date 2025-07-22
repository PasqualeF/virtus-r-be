package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// DTO per la richiesta completa a LibreBooking
@Data
@Builder
public class LibreBookingCreateReservationDto {
    private List<AccessoryDto> accessories;
    private List<CustomAttributeReservationDto> customAttributes;
    private String description;
    private String endDateTime; // Formato ISO string
    private List<Integer> invitees;
    private List<Integer> participants;
    private List<String> participatingGuests;
    private List<String> invitedGuests;
    private RecurrenceRuleDto recurrenceRule;
    private Integer resourceId;
    private List<Integer> resources;
    private String startDateTime; // Formato ISO string
    private String title;
    private Integer userId;
    private ReminderDto startReminder;
    private ReminderDto endReminder;
    private Boolean allowParticipation;
    private List<RetryParameterDto> retryParameters;
    private Boolean termsAccepted;
}
