package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per attributi personalizzati (se necessario)
@Data
@Builder
public class CustomAttributeReservationDto {
    private Integer attributeId;
    private String attributeValue;
}
