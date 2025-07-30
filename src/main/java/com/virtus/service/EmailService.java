package com.virtus.service;

import com.virtus.config.LibreBookingProperties;
import com.virtus.dto.ReservationCreatedDto;
import com.virtus.dto.UserInfoDto;
import com.virtus.model.CertificatoMedico;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

// 3. SERVIZIO EMAIL
@Service
@RequiredArgsConstructor
public class EmailService {

    private final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final LibreBookingProperties properties;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${mail.dirigenti.recipients}")
    private String[] emailDirigenti;

    @Value("${mail.reservation.recipients}")
    private String[] emailReservation;

    @Value("${mail.from}")
    private String emailFrom;

    public void inviaNotifica30Giorni(List<CertificatoMedico> certificati) {
        String subject = "AVVISO: Certificati medici in scadenza tra 30 giorni";
        String body = creaBodyEmail(certificati, "scadranno tra 30 giorni", false);
        inviaEmail(subject, body);
    }

    public void inviaReminder15Giorni(List<CertificatoMedico> certificati) {
        String subject = "REMINDER: Certificati medici in scadenza tra 15 giorni";
        String body = creaBodyEmail(certificati, "scadranno tra 15 giorni", false);
        inviaEmail(subject, body);
    }

    public void inviaReminderUltimi7Giorni(List<CertificatoMedico> certificati) {
        String subject = "URGENTE: Certificati medici in scadenza tra 7 giorni o meno";
        String body = creaBodyEmail(certificati, "scadranno nei prossimi 7 giorni", true);
        inviaEmail(subject, body);
    }

    private String creaBodyEmail(List<CertificatoMedico> certificati, String messaggio, boolean urgente) {
        StringBuilder body = new StringBuilder();

        if (urgente) {
            body.append("<h2 style='color: red;'>⚠️ ATTENZIONE URGENTE ⚠️</h2>");
        }

        body.append("<h3>Certificati medici che ").append(messaggio).append(":</h3>");
        body.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        body.append("<tr style='background-color: #f2f2f2;'>");
        body.append("<th style='padding: 8px;'>Nome</th>");
        body.append("<th style='padding: 8px;'>Cognome</th>");
        body.append("<th style='padding: 8px;'>Data Nascita</th>");
        body.append("<th style='padding: 8px;'>Scadenza Certificato</th>");
        body.append("<th style='padding: 8px;'>Giorni Rimanenti</th>");
        body.append("</tr>");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate oggi = LocalDate.now();

        for (CertificatoMedico cert : certificati) {
            long giorniRimanenti = ChronoUnit.DAYS.between(oggi, cert.getScadenzaCertificato());

            body.append("<tr>");
            body.append("<td style='padding: 8px;'>").append(cert.getNome()).append("</td>");
            body.append("<td style='padding: 8px;'>").append(cert.getCognome()).append("</td>");
            body.append("<td style='padding: 8px;'>").append(cert.getDataNascita().format(formatter)).append("</td>");
            body.append("<td style='padding: 8px;'>").append(cert.getScadenzaCertificato().format(formatter)).append("</td>");
            body.append("<td style='padding: 8px; ").append(giorniRimanenti <= 7 ? "color: red; font-weight: bold;" : "").append("'>")
                    .append(giorniRimanenti).append("</td>");
            body.append("</tr>");
        }

        body.append("</table>");
        body.append("<br><p><strong>Totale certificati: ").append(certificati.size()).append("</strong></p>");
        body.append("<p><em>Email generata automaticamente dal sistema di monitoraggio certificati medici.</em></p>");

        return body.toString();
    }

    public void inviaNotificaRservation(UserInfoDto userInfoDto, ReservationCreatedDto reservationCreatedDto) {
        String subject = "Virtus: nuova prenotazione in attesa di approvazione";
        String body = creaBodyEmailReservation(reservationCreatedDto, null,userInfoDto);
        inviaEmailPrenotazioni(subject, body);
    }

    public void inviaNotificaRservationUpdate(UserInfoDto userInfoDto, ReservationCreatedDto reservationCreatedDto) {
        String subject = "Virtus: è stata modificata una prenotazione";
        String body = creaBodyEmailReservation(reservationCreatedDto, null, userInfoDto);
        inviaEmailPrenotazioni(subject, body);
    }



    private String creaBodyEmailReservation(ReservationCreatedDto reservationCreatedDto, String messaggio,UserInfoDto userInfoDto) {
        StringBuilder body = new StringBuilder();


        // Header principale
        body.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9;'>");
        body.append("<div style='background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>");

        // Titolo principale
        String statoReservation = Boolean.TRUE.equals(reservationCreatedDto.getSuccess()) ? "✅ REGISTRATA" : "❌ NON CONFERMATA";
        String coloreTitolo = Boolean.TRUE.equals(reservationCreatedDto.getSuccess()) ? "#28a745" : "#dc3545";

        body.append("<h1 style='color: ").append(coloreTitolo).append("; text-align: center; margin-bottom: 30px;'>");
        body.append("Prenotazione ").append(statoReservation).append("</h1>");

        // Dettagli della prenotazione
        body.append("<div style='background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin-bottom: 20px;'>");
        body.append("<h3 style='color: #495057; margin-top: 0;'>📋 Dettagli Prenotazione</h3>");

        // Numero di riferimento
        body.append("<p><strong>Numero di Riferimento:</strong> <span style='color: #007bff; font-weight: bold;'>");
        body.append(reservationCreatedDto.getReferenceNumber() != null ? reservationCreatedDto.getReferenceNumber() : "N/A");
        body.append("</span></p>");

        // Titolo/Descrizione
        if (reservationCreatedDto.getTitle() != null && !reservationCreatedDto.getTitle().trim().isEmpty()) {
            body.append("<p><strong>Titolo:</strong> ").append(reservationCreatedDto.getTitle()).append("</p>");
        }

        // Date e orari
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        if (reservationCreatedDto.getStartDateTime() != null) {
            body.append("<p><strong>📅 Data Inizio:</strong> ").append(reservationCreatedDto.getStartDateTime().format(dateFormatter)).append("</p>");
            body.append("<p><strong>🕐 Ora Inizio:</strong> ").append(reservationCreatedDto.getStartDateTime().format(timeFormatter)).append("</p>");
        }

        if (reservationCreatedDto.getEndDateTime() != null) {
            body.append("<p><strong>🕐 Ora Fine:</strong> ").append(reservationCreatedDto.getEndDateTime().format(timeFormatter)).append("</p>");
        }

        // Durata calcolata
        if (reservationCreatedDto.getStartDateTime() != null && reservationCreatedDto.getEndDateTime() != null) {
            Duration durata = Duration.between(reservationCreatedDto.getStartDateTime(), reservationCreatedDto.getEndDateTime());
            long ore = durata.toHours();
            long minuti = durata.toMinutesPart();
            body.append("<p><strong>⏱️ Durata:</strong> ");
            if (ore > 0) {
                body.append(ore).append(" ora").append(ore > 1 ? "e" : "");
            }
            if (minuti > 0) {
                if (ore > 0) body.append(" e ");
                body.append(minuti).append(" minuto").append(minuti > 1 ? "i" : "");
            }
            body.append("</p>");
        }

        // ID Risorsa
        if (reservationCreatedDto.getResourceId() != null) {
            body.append("<p><strong>🏢 ID Risorsa:</strong> ").append(reservationCreatedDto.getResourceId()).append("</p>");
        }

        body.append("</div>");

        // Stato approvazione
        if (Boolean.TRUE.equals(reservationCreatedDto.getIsPendingApproval())) {
            body.append("<div style='background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin-bottom: 20px;'>");
            body.append("<h4 style='color: #856404; margin-top: 0;'>⏳ In Attesa di Approvazione</h4>");
            body.append("<p style='color: #856404; margin-bottom: 0;'>La prenotazione è stata creata ma necessita di approvazione prima di essere definitiva.</p>");
            body.append("<h4 style='color: #856404; margin-top: 0;'>Clicca qui per approvare:</h4>");
            String url = properties.getCalendarUrl() + properties.getResApprove() + reservationCreatedDto.getReferenceNumber();
            body.append(url);
            body.append("</div>");
        }

        // Messaggio personalizzato se presente
        if (messaggio != null && !messaggio.trim().isEmpty()) {
            body.append("<div style='background-color: #e3f2fd; border-left: 4px solid #2196f3; padding: 15px; margin-bottom: 20px;'>");
            body.append("<h4 style='color: #1976d2; margin-top: 0;'>💬 Messaggio</h4>");
            body.append("<p style='color: #1976d2; margin-bottom: 0;'>").append(messaggio).append("</p>");
            body.append("</div>");
        }

        // Messaggio dal DTO se presente
        if (userInfoDto != null ) {
            body.append("<div style='background-color: #f3e5f5; border-left: 4px solid #9c27b0; padding: 15px; margin-bottom: 20px;'>");
            body.append("<h4 style='color: #7b1fa2; margin-top: 0;'>ℹ️ Informazioni Aggiuntive</h4>");
            body.append("<p style='color: #856404; margin-bottom: 0;'>La prenotazione è stata creata dall'utente:</p>");
            body.append("<p style='color: #7b1fa2; margin-bottom: 0;'>").append("Utente: ").append(userInfoDto.getFirstName()).append(" ").append(userInfoDto.getLastName()).append("</p>");
            body.append("<p style='color: #7b1fa2; margin-bottom: 0;'>").append("Email: ").append(userInfoDto.getEmailAddress()).append("</p>");
            body.append("<p style='color: #7b1fa2; margin-bottom: 0;'>").append("Telefono: ").append(userInfoDto.getPhone() != null ? userInfoDto.getPhone() : " ").append("</p>");
            body.append("<p style='color: #7b1fa2; margin-bottom: 0;'>").append("Per conto di: ").append(userInfoDto.getOrganization()).append("</p>");
            body.append("</div>");
        }


        // Footer con timestamp
        LocalDateTime oggi = LocalDateTime.now();
        DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'alle' HH:mm");

        body.append("<div style='border-top: 1px solid #dee2e6; padding-top: 20px; margin-top: 20px;'>");
        body.append("<p style='color: #6c757d; font-size: 12px; text-align: center; margin-bottom: 0;'>");
        body.append("<em>📧 Email generata automaticamente dal sistema di monitoraggio certificati medici<br>");
        body.append("Generata il ").append(oggi.format(timestampFormatter)).append("</em>");
        body.append("</p>");
        body.append("</div>");

        body.append("</div></div>");

        return body.toString();
    }

    private void inviaEmail(String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(emailDirigenti);
            helper.setSubject(subject);
            helper.setText(body, true); // true per HTML

            mailSender.send(message);
            logger.info("Email inviata con successo: {}", subject);

        } catch (Exception e) {
            logger.error("Errore nell'invio dell'email: {}", subject, e);
        }
    }

    private void inviaEmailPrenotazioni(String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(emailReservation);
            helper.setSubject(subject);
            helper.setText(body, true); // true per HTML

            mailSender.send(message);
            logger.info("Email inviata con successo: {}", subject);

        } catch (Exception e) {
            logger.error("Errore nell'invio dell'email: {}", subject, e);
        }
    }
}
