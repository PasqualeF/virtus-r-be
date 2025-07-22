package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per la risposta di refresh token
@Data
@Builder
public class RefreshTokenResponseDto {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private boolean success;
    private String message;
}
