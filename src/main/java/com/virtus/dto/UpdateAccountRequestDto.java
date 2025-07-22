package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// DTO per l'aggiornamento account
@Data
@Builder
public class UpdateAccountRequestDto {
    private String firstName;
    private String lastName;
    private String emailAddress;
    private String userName;
    private String language;
    private String timezone;
    private String phone;
    private String organization;
    private String position;
    private List<CustomAttributeDto> customAttributes;
}
