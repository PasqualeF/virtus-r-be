package com.virtus.controller;

import com.virtus.service.MonitoraggioCertificatiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 8. CONTROLLER PER TEST MANUALE
@RestController
@RequestMapping("/api/certificati")
public class CertificatiController {

    @Autowired
    private MonitoraggioCertificatiService monitoraggioService;

    @PostMapping("/monitor")
    public ResponseEntity<String> eseguiMonitoraggioManuale() {
        try {
            monitoraggioService.eseguiMonitoraggio();
            return ResponseEntity.ok("Monitoraggio eseguito con successo");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Errore durante il monitoraggio: " + e.getMessage());
        }
    }
}