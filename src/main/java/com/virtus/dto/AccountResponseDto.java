package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// DTO per la risposta di ottenimento account
@Data
@Builder
public class AccountResponseDto {
    private List<Object> links;
    private String message;
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
    private List<CustomAttributeResponseDto> customAttributes;
    private String icsUrl;
}
