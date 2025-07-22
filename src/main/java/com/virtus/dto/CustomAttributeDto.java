package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per l'attributo personalizzato
@Data
@Builder
public class CustomAttributeDto {
    private Integer attributeId;
    private String attributeValue;
}
