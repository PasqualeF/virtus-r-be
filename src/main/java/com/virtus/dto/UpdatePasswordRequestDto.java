package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per l'aggiornamento password
@Data
@Builder
public class UpdatePasswordRequestDto {
    private String currentPassword;
    private String newPassword;
}
