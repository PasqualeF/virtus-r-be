package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// DTO per la regola di ricorrenza
@Data
@Builder
public class RecurrenceRuleDto {
    private String type; // daily, weekly, monthly, yearly, none
    private Integer interval;
    private String monthlyType; // dayOfMonth, dayOfWeek, null
    private List<Integer> weekdays; // 0=Sunday, 1=Monday, etc.
    private LocalDateTime repeatTerminationDate;
}
