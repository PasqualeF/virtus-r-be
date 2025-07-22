package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per accessori (se necessario)
@Data
@Builder
public class AccessoryDto {
    private Integer accessoryId;
    private Integer quantityRequested;
}
