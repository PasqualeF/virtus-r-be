///*
//package com.virtus.security;
//
//import com.virtus.dto.JwtClaimsDto;
//import com.virtus.service.JwtService;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.ArrayList;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    private final JwtService jwtService;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException {
//
//        String path = request.getRequestURI();
//        String method = request.getMethod();
//
//        log.debug("🔍 JWT Filter processing: {} {}", method, path);
//
//        // Se è un endpoint pubblico, passa oltre senza processare JWT
//        if (isPublicEndpoint(path, method)) {
//            log.debug("🌍 Endpoint pubblico, skippo JWT: {}", path);
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        try {
//            String token = extractTokenFromRequest(request);
//
//            if (token != null && jwtService.validateToken(token)) {
//                // Verifica anche se la sessione LibreBooking è valida
//                if (jwtService.isLibreBookingSessionValid(token)) {
//                    JwtClaimsDto claims = jwtService.extractClaims(token);
//
//                    // Crea i dettagli dell'utente per Spring Security
//                    UserDetails userDetails = User.builder()
//                            .username(claims.getUsername())
//                            .password("") // Non serve la password per l'autenticazione JWT
//                            .authorities(new ArrayList<>()) // Puoi aggiungere ruoli se necessario
//                            .build();
//
//                    UsernamePasswordAuthenticationToken authentication =
//                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//
//                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//
//                    // Aggiungi le informazioni della sessione LibreBooking alla richiesta
//                    request.setAttribute("jwtClaims", claims);
//                    request.setAttribute("sessionToken", claims.getSessionToken());
//                    request.setAttribute("libreBookingUserId", claims.getLibreBookingUserId());
//
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//
//                    log.debug("🔐 Autenticazione JWT valida per utente: {}", claims.getUsername());
//                } else {
//                    log.warn("🚨 Sessione LibreBooking scaduta, token JWT non valido per path: {}", path);
//                }
//            } else if (token != null) {
//                log.warn("🚨 Token JWT non valido per path: {}", path);
//            }
//
//        } catch (Exception e) {
//            log.error("❌ Errore durante l'autenticazione JWT per path {}: {}", path, e.getMessage());
//            SecurityContextHolder.clearContext();
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//    */
///**
//     * Verifica se un endpoint è pubblico
//     *//*
//
//    private boolean isPublicEndpoint(String path, String method) {
//        // OPTIONS sempre pubblico per CORS
//        if ("OPTIONS".equals(method)) {
//            return true;
//        }
//
//        // Endpoint di autenticazione (senza context path)
//        if (path.startsWith("/auth/")) {
//            return true;
//        }
//
//        // Health check (senza context path)
//        if (path.equals("/accounts/health")) {
//            return true;
//        }
//
//        // Creazione account (solo POST, senza context path)
//        if (("POST".equals(method)) && path.equals("/accounts")) {
//            return true;
//        }
//
//        // Orari allenamenti (senza context path)
//        if (path.contains("/orari-allenamenti")) {
//            return true;
//        }
//
//        return false;
//    }
//
//    */
///**
//     * Estrae il token JWT dalla richiesta HTTP
//     *//*
//
//    private String extractTokenFromRequest(HttpServletRequest request) {
//        String bearerToken = request.getHeader("Authorization");
//
//        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
//            return bearerToken.substring(7);
//        }
//
//        return null;
//    }
//}*/
