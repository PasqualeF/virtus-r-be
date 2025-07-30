package com.virtus.service;

import com.virtus.dto.JwtClaimsDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret:mySecretKey12345678901234567890123456789012345678901234567890}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400}") // 24 ore di default
    private Long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Genera un JWT token con le informazioni utente e sessione LibreBooking
     */
    public String generateToken(JwtClaimsDto claims) {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("userId", claims.getUserId());
        claimsMap.put("username", claims.getUsername());
        claimsMap.put("email", claims.getEmail());
        claimsMap.put("cell", claims.getCell());
        claimsMap.put("organization", claims.getOrganization());
        claimsMap.put("firstName", claims.getFirstName());
        claimsMap.put("lastName", claims.getLastName());
        claimsMap.put("sessionToken", claims.getSessionToken());
        claimsMap.put("libreBookingUserId", claims.getLibreBookingUserId());
        claimsMap.put("sessionExpiry", claims.getSessionExpiry());

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpiration * 1000);

        String token = Jwts.builder()
                .addClaims(claimsMap)
                .setSubject(claims.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey())
                .compact();

        log.debug("🎫 Token JWT generato per utente: {}", claims.getUsername());
        return token;
    }

    /**
     * Valida un JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("🚨 Token JWT scaduto: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("🚨 Token JWT non supportato: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("🚨 Token JWT malformato: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("🚨 Token JWT vuoto: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("🚨 Errore validazione token JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Estrae le informazioni dal JWT token
     */
    public JwtClaimsDto extractClaims(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return JwtClaimsDto.builder()
                    .userId(claims.get("userId", Integer.class))
                    .username(claims.get("username", String.class))
                    .firstName(claims.get("firstName", String.class))
                    .lastName(claims.get("lastName", String.class))
                    .email(claims.get("email", String.class))
                    .cell(claims.get("cell", String.class))
                    .organization(claims.get("organization", String.class))
                    .sessionToken(claims.get("sessionToken", String.class))
                    .libreBookingUserId(claims.get("libreBookingUserId", String.class))
                    .sessionExpiry(claims.get("sessionExpiry", Long.class))
                    .build();
        } catch (Exception e) {
            log.error("🚨 Errore estrazione claims JWT: {}", e.getMessage());
            throw new RuntimeException("Token JWT non valido");
        }
    }

    /**
     * Estrae il nome utente dal token
     */
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("🚨 Errore estrazione username dal token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifica se il token è scaduto
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Verifica se la sessione LibreBooking è ancora valida
     */
    public boolean isLibreBookingSessionValid(String token) {
        try {
            JwtClaimsDto claims = extractClaims(token);
            Long sessionExpiry = claims.getSessionExpiry();

            if (sessionExpiry == null) {
                return false;
            }

            long currentTime = System.currentTimeMillis();

            boolean isValid = currentTime < sessionExpiry;
            long time = sessionExpiry - currentTime;
            Date now = new Date();
            if (!isValid) {
                log.warn("🚨 Sessione LibreBooking scaduta per utente: {}", claims.getUsername());
            }

            return isValid;
        } catch (Exception e) {
            log.error("🚨 Errore verifica sessione LibreBooking: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Calcola il tempo rimanente del token in secondi
     */
    public Long getTokenExpirationTime(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            Date now = new Date();

            if (expiration.before(now)) {
                return 0L;
            }

            return (expiration.getTime() - now.getTime()) / 1000;
        } catch (Exception e) {
            return 0L;
        }
    }
}