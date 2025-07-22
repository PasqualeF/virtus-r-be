package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// DTO per la risposta di creazione account
@Data
@Builder
public class AccountCreatedResponseDto {
    private List<Object> links;
    private String message;
    private Integer userId;
    private boolean success;
}
