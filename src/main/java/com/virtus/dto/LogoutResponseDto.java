package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per la risposta di logout
@Data
@Builder
public class LogoutResponseDto {
    private boolean success;
    private String message;
}
