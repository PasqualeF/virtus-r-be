package com.virtus.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.virtus.model.CertificatoMedico;
import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Service
public class GoogleDriveService {

    private final Logger logger = LoggerFactory.getLogger(GoogleDriveService.class);

    @Value("${google.drive.file.id}")
    private String fileId;

    @Value("${google.drive.credentials.path}")
    private String credentialsPath;

    private Drive driveService;

    @PostConstruct
    public void init() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(credentialsPath))
                .createScoped(Collections.singletonList(DriveScopes.DRIVE_READONLY));

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        this.driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Medical Certificate Monitor")
                .build();
    }

    public List<CertificatoMedico> scaricaCertificati() throws IOException {
        logger.info("Scaricamento file Excel da Google Drive...");

        try {
            // Scarica il file Excel come bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            driveService.files().export(fileId, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .executeMediaAndDownloadTo(outputStream);

            byte[] excelData = outputStream.toByteArray();
            return parseExcelContent(excelData);

        } catch (Exception e) {
            logger.warn("Errore nello scaricamento come Excel, provo come CSV...", e);

            // Fallback: prova a scaricare come CSV
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            driveService.files().export(fileId, "text/csv").executeMediaAndDownloadTo(outputStream);

            String csvContent = outputStream.toString("UTF-8");
            return parseCsvContent(csvContent);
        }
    }

    private List<CertificatoMedico> parseExcelContent(byte[] excelData) throws IOException {
        List<CertificatoMedico> certificati = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(excelData)) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // Prende il primo foglio

            logger.info("Lettura foglio Excel: {}", sheet.getSheetName());

            // Itera sulle righe (salta l'header - riga 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String nome = getCellValueAsString(row.getCell(1));
                    String cognome = getCellValueAsString(row.getCell(2));
                    String dataNascitaStr = getCellValueAsString(row.getCell(4));
                    String scadenzaStr = getCellValueAsString(row.getCell(6));

                    if (nome.isEmpty() || cognome.isEmpty() || dataNascitaStr.isEmpty() || scadenzaStr.isEmpty()) {
                        continue; // Salta righe con dati mancanti
                    }

                    LocalDate dataNascita = parseDate(dataNascitaStr, formatter);
                    LocalDate scadenza = parseDate(scadenzaStr, formatter);

                    certificati.add(new CertificatoMedico(nome, cognome, dataNascita, scadenza));

                } catch (Exception e) {
                    logger.warn("Errore nel parsing della riga {}: {}", i, e.getMessage());
                }
            }

            workbook.close();
        }

        logger.info("Caricati {} certificati dal file Excel", certificati.size());
        return certificati;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Se è una data, la formatta come stringa
                    LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                    return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } else {
                    // Se è un numero, lo converte in stringa
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private LocalDate parseDate(String dateStr, DateTimeFormatter formatter) {
        try {
            // Prova prima con il formato dd/MM/yyyy
            return LocalDate.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            try {
                // Prova con formato alternativo yyyy-MM-dd
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException e2) {
                try {
                    // Prova con formato dd-MM-yyyy
                    return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                } catch (DateTimeParseException e3) {
                    logger.error("Impossibile parsare la data: {}", dateStr);
                    throw e3;
                }
            }
        }
    }

    private List<CertificatoMedico> parseCsvContent(String csvContent) {
        List<CertificatoMedico> certificati = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String[] lines = csvContent.split("\n");

        // Salta l'header (prima riga)
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] fields = line.split(",");
            if (fields.length >= 4) {
                try {
                    String nome = fields[1].trim().replace("\"", "");
                    String cognome = fields[2].trim().replace("\"", "");
                    LocalDate dataNascita = parseDate(fields[4].trim().replace("\"", ""), formatter);
                    LocalDate scadenza = parseDate(fields[6].trim().replace("\"", ""), formatter);

                    certificati.add(new CertificatoMedico(nome, cognome, dataNascita, scadenza));
                } catch (Exception e) {
                    logger.warn("Errore nel parsing della riga: " + line, e);
                }
            }
        }

        logger.info("Caricati {} certificati dal CSV", certificati.size());
        return certificati;
    }
}