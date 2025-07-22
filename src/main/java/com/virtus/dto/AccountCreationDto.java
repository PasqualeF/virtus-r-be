package com.virtus.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountCreationDto {

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il nome deve essere tra 2 e 50 caratteri")
    private String firstName;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il cognome deve essere tra 2 e 50 caratteri")
    private String lastName;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    @Size(max = 100, message = "L'email non può superare 100 caratteri")
    private String emailAddress;

    @NotBlank(message = "Il nome utente è obbligatorio")
    @Size(min = 3, max = 30, message = "Il nome utente deve essere tra 3 e 30 caratteri")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Il nome utente può contenere solo lettere, numeri, punti, trattini e underscore")
    private String userName;

    @Pattern(regexp = "^[a-z]{2}_[a-z]{2}$", message = "Il formato della lingua deve essere xx_xx (es: it_it)")
    private String language;

    @Pattern(regexp = "^[A-Za-z]+/[A-Za-z_]+$", message = "Il formato del timezone deve essere Continente/Città (es: Europe/Rome)")
    private String timezone;

    @Pattern(regexp = "^[+]?[0-9\\s.-]{0,20}$", message = "Formato telefono non valido")
    private String phone;

    @Size(max = 100, message = "L'organizzazione non può superare 100 caratteri")
    private String organization;

    @Size(max = 100, message = "La posizione non può superare 100 caratteri")
    private String position;

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 8, message = "La password deve essere di almeno 8 caratteri")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "La password deve contenere almeno una lettera minuscola, una maiuscola e un numero")
    private String password;

    @AssertTrue(message = "Devi accettare i termini di servizio")
    private boolean acceptTermsOfService;
}