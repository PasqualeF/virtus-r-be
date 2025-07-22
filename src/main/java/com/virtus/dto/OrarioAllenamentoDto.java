package com.virtus.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class OrarioAllenamentoDto {
    private String gruppo;
    private String giorno;
    private String orario;
    private String palestra;
    private String palestraId;
    private String referenceNumber;
    private String title;
    private String description;
    private String startDate;
    private String endDate;
    private Boolean isRecurring;
}



