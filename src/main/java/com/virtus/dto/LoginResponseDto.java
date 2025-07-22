package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per la risposta di login
@Data
@Builder
public class LoginResponseDto {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfoDto user;
    private boolean success;
    private String message;
}
