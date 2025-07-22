package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per parametri retry (se necessario)
@Data
@Builder
public class RetryParameterDto {
    private String name;
    private String value;
}
