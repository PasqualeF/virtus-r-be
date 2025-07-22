package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DashboardStatsDto {
    // Stato della risposta
    private Boolean success;
    private String message;

    // Statistiche principali
    private Long activeBookings;
    private Double totalHours;
    private Long gymsUsed;
    private Long upcomingBookings;

    // Statistiche dettagliate
    private Long confirmedBookings;
    private Long pendingBookings;
    private Long completedBookings;
    private Long cancelledBookings;

    // Statistiche temporali
    private Long thisWeekBookings;
    private Long thisMonthBookings;
    private Long thisYearBookings;

    // Tendenze (opzionale)
    private Double weeklyGrowth; // Percentuale crescita settimanale
    private Double monthlyGrowth; // Percentuale crescita mensile

    // Informazioni periodo
    private String period;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // Palestre più utilizzate (top 3)
    private String topGym1;
    private Long topGym1Count;
    private String topGym2;
    private Long topGym2Count;
    private String topGym3;
    private Long topGym3Count;

    // Statistiche orari preferiti
    private String preferredTimeSlot; // Es: "18:00-20:00"
    private Long preferredTimeSlotCount;

    // Media durata sessioni
    private Double averageSessionDuration;

    // Ultimo aggiornamento
    private LocalDateTime lastUpdated;
}