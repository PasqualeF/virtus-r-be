package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// DTO per l'attributo personalizzato nella risposta
@Data
@Builder
public class CustomAttributeResponseDto {
    private List<Object> links;
    private String message;
    private Integer id;
    private String label;
    private String value;
}
