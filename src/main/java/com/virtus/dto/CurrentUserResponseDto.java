package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per la risposta di "current user"
@Data
@Builder
public class CurrentUserResponseDto {
    private UserInfoDto user;
    private boolean authenticated;
    private Long tokenExpiresIn;
}
