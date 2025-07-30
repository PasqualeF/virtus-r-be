package com.virtus.model;

import java.time.LocalDate;

// 1. MODELLO DATI
public class CertificatoMedico {
    private String nome;
    private String cognome;
    private LocalDate dataNascita;
    private LocalDate scadenzaCertificato;

    // Costruttori
    public CertificatoMedico() {}

    public CertificatoMedico(String nome, String cognome, LocalDate dataNascita, LocalDate scadenzaCertificato) {
        this.nome = nome;
        this.cognome = cognome;
        this.dataNascita = dataNascita;
        this.scadenzaCertificato = scadenzaCertificato;
    }

    // Getters e Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    public LocalDate getDataNascita() { return dataNascita; }
    public void setDataNascita(LocalDate dataNascita) { this.dataNascita = dataNascita; }

    public LocalDate getScadenzaCertificato() { return scadenzaCertificato; }
    public void setScadenzaCertificato(LocalDate scadenzaCertificato) { this.scadenzaCertificato = scadenzaCertificato; }

    public String getNomeCompleto() {
        return nome + " " + cognome;
    }
}