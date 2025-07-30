package com.virtus.scheduler;

import com.virtus.service.MonitoraggioCertificatiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CertificatiScheduler {

    private final Logger logger = LoggerFactory.getLogger(CertificatiScheduler.class);

    @Autowired
    private MonitoraggioCertificatiService monitoraggioService;

    // Esegue ogni giorno alle 7:00
    @Scheduled(cron = "0 0 8 * * *")
    public void eseguiMonitoraggioGiornaliero() {
        logger.info("Avvio monitoraggio schedulato certificati medici...");
        monitoraggioService.eseguiMonitoraggio();
    }
}