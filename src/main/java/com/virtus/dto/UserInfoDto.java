package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per le informazioni utente nel JWT
@Data
@Builder
public class UserInfoDto {
    private Integer userId;
    private String username;
    private String firstName;
    private String lastName;
    private String emailAddress;
    private String language;
    private String timezone;
    private String phone;
    private String organization;
    private String position;
}
