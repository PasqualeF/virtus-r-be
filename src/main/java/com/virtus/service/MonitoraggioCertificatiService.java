package com.virtus.service;

import com.virtus.model.CertificatoMedico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class MonitoraggioCertificatiService {

    private final Logger logger = LoggerFactory.getLogger(MonitoraggioCertificatiService.class);

    @Autowired
    private GoogleDriveService googleDriveService;

    @Autowired
    private EmailService emailService;

    // Repository per tenere traccia delle notifiche già inviate
  //  @Autowired
  //  private NotificaRepository notificaRepository;

    public void eseguiMonitoraggio() {
        try {
            logger.info("Inizio monitoraggio certificati medici...");

            List<CertificatoMedico> certificati = googleDriveService.scaricaCertificati();
            LocalDate oggi = LocalDate.now();

            // Raggruppa i certificati per fasce di scadenza
            List<CertificatoMedico> scadenza30Giorni = new ArrayList<>();
            List<CertificatoMedico> scadenza15Giorni = new ArrayList<>();
            List<CertificatoMedico> scadenza7Giorni = new ArrayList<>();

            for (CertificatoMedico cert : certificati) {
                long giorniRimanenti = ChronoUnit.DAYS.between(oggi, cert.getScadenzaCertificato());

                if (giorniRimanenti <= 30 && giorniRimanenti >= 16) {
                    scadenza30Giorni.add(cert);
                } else if (giorniRimanenti <= 15 && giorniRimanenti >= 8) {
                    scadenza15Giorni.add(cert);
                } else if (giorniRimanenti <= 7 && giorniRimanenti >= 0) {
                    scadenza7Giorni.add(cert);
                }
            }

            // Invio notifiche
            if (!scadenza30Giorni.isEmpty()) {
               // if (!isNotificaGiaInviata("30_GIORNI", oggi)) {
                    emailService.inviaNotifica30Giorni(scadenza30Giorni);
               //     salvaNotifica("30_GIORNI", oggi);
              //  }
            }

            if (!scadenza15Giorni.isEmpty()) {
             //   if (!isNotificaGiaInviata("15_GIORNI", oggi)) {
                    emailService.inviaReminder15Giorni(scadenza15Giorni);
             //       salvaNotifica("15_GIORNI", oggi);
             //   }
            }

            if (!scadenza7Giorni.isEmpty()) {
                // Per gli ultimi 7 giorni, inviamo sempre (ogni giorno)
                emailService.inviaReminderUltimi7Giorni(scadenza7Giorni);
             //   salvaNotifica("7_GIORNI", oggi);
            }

            logger.info("Monitoraggio completato. 30gg: {}, 15gg: {}, 7gg: {}",
                    scadenza30Giorni.size(), scadenza15Giorni.size(), scadenza7Giorni.size());

        } catch (Exception e) {
            logger.error("Errore durante il monitoraggio dei certificati", e);
        }
    }

  /*  private boolean isNotificaGiaInviata(String tipo, LocalDate data) {
        return notificaRepository.existsByTipoAndDataInvio(tipo, data);
    }

    private void salvaNotifica(String tipo, LocalDate data) {
        NotificaInviata notifica = new NotificaInviata();
        notifica.setTipo(tipo);
        notifica.setDataInvio(data);
        notifica.setTimestamp(LocalDateTime.now());
        notificaRepository.save(notifica);
    }*/
}