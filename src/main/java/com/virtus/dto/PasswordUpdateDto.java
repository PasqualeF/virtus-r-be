package com.virtus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

// DTO per le richieste dal frontend - Cambio password
@Data
@Builder
public class PasswordUpdateDto {

    @NotBlank(message = "La password corrente è obbligatoria")
    private String currentPassword;

    @NotBlank(message = "La nuova password è obbligatoria")
    @Size(min = 8, message = "La password deve essere di almeno 8 caratteri")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "La password deve contenere almeno una lettera minuscola, una maiuscola e un numero")
    private String newPassword;
}
