package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller REST de autenticación para Angular.
 *
 * Endpoints disponibles:
 *   POST /api/auth/login    → manejado por Spring Security (SecurityConfig)
 *   POST /api/auth/logout   → manejado por Spring Security (SecurityConfig)
 *   GET  /api/auth/me       → devuelve usuario y roles de la sesión activa
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * Devuelve los datos del usuario autenticado actualmente.
     * Angular llama a este endpoint al iniciar la app para saber
     * si ya hay una sesión activa (cookie JSESSIONID válida).
     *
     * Respuesta exitosa (200):
     * {
     *   "autenticado": true,
     *   "usuario": "admin@apollo.com",
     *   "roles": ["ROLE_ADMIN"]
     * }
     *
     * Sin sesión (401): manejado automáticamente por el authenticationEntryPoint
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            Map<String, Object> body = new HashMap<>();
            body.put("autenticado", false);
            body.put("mensaje", "No hay sesión activa");
            return ResponseEntity.status(401).body(body);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("autenticado", true);
        body.put("usuario", authentication.getName());
        body.put("roles", authentication.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList()));

        return ResponseEntity.ok(body);
    }
}