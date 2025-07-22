package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per la risposta al frontend - Informazioni account
@Data
@Builder
public class AccountInfoDto {
    private Integer userId;
    private String firstName;
    private String lastName;
    private String emailAddress;
    private String userName;
    private String language;
    private String timezone;
    private String phone;
    private String organization;
    private String position;
    private String icsUrl;
}
