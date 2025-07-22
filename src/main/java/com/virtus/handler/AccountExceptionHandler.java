package com.virtus.handler;

import com.virtus.dto.AccountCreatedResponseDto;
import com.virtus.dto.AccountUpdatedResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestControllerAdvice
@Slf4j
public class AccountExceptionHandler {

    /**
     * Gestisce gli errori di validazione sui campi dei DTO
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        errors.put("message", "Errori di validazione");
        errors.put("errors", fieldErrors);
        errors.put("success", false);

        log.warn("🚨 Errori di validazione: {}", fieldErrors);
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Gestisce gli errori di validazione sui parametri del path
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> errors = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            fieldErrors.put(fieldName, errorMessage);
        }

        errors.put("message", "Errori di validazione parametri");
        errors.put("errors", fieldErrors);
        errors.put("success", false);

        log.warn("🚨 Errori di validazione parametri: {}", fieldErrors);
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Gestisce gli errori di risposta da LibreBooking
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientResponseException(WebClientResponseException ex) {
        Map<String, Object> error = new HashMap<>();

        String message = "Errore comunicazione con LibreBooking";
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        // Personalizza il messaggio in base al codice di errore
        switch (ex.getStatusCode().value()) {
            case 400 -> message = "Dati non validi";
            case 401 -> message = "Autenticazione fallita";
            case 403 -> message = "Accesso negato";
            case 404 -> message = "Risorsa non trovata";
            case 409 -> message = "Conflitto: l'utente potrebbe già esistere";
            case 500 -> message = "Errore interno del server di prenotazione";
        }

        error.put("message", message);
        error.put("success", false);
        error.put("statusCode", ex.getStatusCode().value());

        log.error("🚨 Errore LibreBooking: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Gestisce le eccezioni generiche
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> error = new HashMap<>();

        error.put("message", "Errore interno del server");
        error.put("success", false);
        error.put("details", ex.getMessage());

        log.error("🚨 Errore generico: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Gestisce le eccezioni generiche per tutti gli altri casi
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> error = new HashMap<>();

        error.put("message", "Errore imprevisto");
        error.put("success", false);

        log.error("🚨 Errore imprevisto: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}