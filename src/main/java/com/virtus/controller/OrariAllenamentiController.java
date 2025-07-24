package com.virtus.controller;

import com.virtus.dto.ApiResponse;
import com.virtus.dto.OrarioAllenamentoDto;
import com.virtus.service.OrariAllenamentiService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/orari-allenamenti")
@RequiredArgsConstructor
@Slf4j
public class OrariAllenamentiController {

    private final OrariAllenamentiService bridgeService;


    @GetMapping
    public ResponseEntity<ApiResponse<List<OrarioAllenamentoDto>>> getOrariAllenamenti() {

        try {
            List<OrarioAllenamentoDto> orari = bridgeService.getOrariAllenamenti();
            return ResponseEntity.ok(ApiResponse.success(orari));
        } catch (Exception e) {
            log.error("Errore durante il recupero degli orari", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Errore: " + e.getMessage()));
        }
    }

    @GetMapping("/{palestra}")
    public ResponseEntity<ApiResponse<List<OrarioAllenamentoDto>>> getPrenotazioniForPalestra(@PathVariable @NotNull String palestra) {

        try {
            List<OrarioAllenamentoDto> orari = bridgeService.getPrenotazioniForPalestra(palestra);
            return ResponseEntity.ok(ApiResponse.success(orari));
        } catch (Exception e) {
            log.error("Errore durante il recupero degli orari", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Errore: " + e.getMessage()));
        }
    }



    @GetMapping("/refresh")
    public ResponseEntity<ApiResponse<List<OrarioAllenamentoDto>>> refreshData() {
        log.warn("🔄 POST /orari-allenamenti/refresh - FORZANDO REFRESH");

        try {
            List<OrarioAllenamentoDto> orari = bridgeService.refreshOrariAllenamenti();
            return ResponseEntity.ok(ApiResponse.success(orari));
        } catch (Exception e) {
            log.error("Errore durante il refresh", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Errore refresh: " + e.getMessage()));
        }
    }



}