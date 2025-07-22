package com.virtus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

// DTO per la richiesta di login
@Data
@Builder
public class LoginRequestDto {
    @NotBlank(message = "Il nome utente è obbligatorio")
    private String username;

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 1, message = "La password non può essere vuota")
    private String password;
}

