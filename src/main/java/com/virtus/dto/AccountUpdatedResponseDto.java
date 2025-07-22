package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// DTO per la risposta di aggiornamento account
@Data
@Builder
public class AccountUpdatedResponseDto {
    private List<Object> links;
    private String message;
    private boolean success;
}
