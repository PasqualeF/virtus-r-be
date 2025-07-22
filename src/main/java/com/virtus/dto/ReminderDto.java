package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per reminder (se necessario)
@Data
@Builder
public class ReminderDto {
    private Integer value;
    private String interval; // hours, minutes, days
}
