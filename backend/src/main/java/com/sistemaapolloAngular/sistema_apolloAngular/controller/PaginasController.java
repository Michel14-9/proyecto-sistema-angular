package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaginasController {


    @GetMapping("/auth/status")
    public ResponseEntity<Map<String, Object>> verificarAutenticacion(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();

        response.put("authenticated", isAuthenticated);

        if (isAuthenticated) {
            response.put("username", authentication.getName());

            // Obtener roles del usuario
            String roles = authentication.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            response.put("roles", roles);
            response.put("mensaje", "Usuario autenticado correctamente");
        } else {
            response.put("mensaje", "Usuario no autenticado");
        }

        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            response.put("success", true);
            response.put("message", "Sesión cerrada correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al cerrar sesión: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/auth/user")
    public ResponseEntity<Map<String, Object>> obtenerUsuarioActual(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("authenticated", false);
            response.put("message", "Usuario no autenticado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        response.put("authenticated", true);
        response.put("username", authentication.getName());

        // Obtener roles
        String roles = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        response.put("roles", roles);
        response.put("message", "Usuario autenticado");

        return ResponseEntity.ok(response);
    }
}