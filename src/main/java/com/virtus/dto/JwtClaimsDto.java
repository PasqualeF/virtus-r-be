package com.virtus.dto;

import lombok.Builder;
import lombok.Data;

// DTO per i claims del JWT
@Data
@Builder
public class JwtClaimsDto {
    private Integer userId;
    private String username;
    private String firstName;
    private String lastName;
    private String sessionToken; // Token di LibreBooking
    private String libreBookingUserId; // ID utente su LibreBooking
    private Long sessionExpiry; // Timestamp scadenza sessione LibreBooking
}
