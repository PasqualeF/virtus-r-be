package com.virtus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO per i link nella risposta
@Data
@Builder
@NoArgsConstructor  // ← NECESSARIO per Jackson
@AllArgsConstructor // ← Per Builder pattern
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkDto {
    private String href;
    private String title;
}
