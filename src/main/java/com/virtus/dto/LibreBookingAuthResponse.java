package com.virtus.dto;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class LibreBookingAuthResponse {
    private String sessionToken;
    private String userId;
    private boolean isAuthenticated;
    private String message;
    private String sessionExpires;
}
