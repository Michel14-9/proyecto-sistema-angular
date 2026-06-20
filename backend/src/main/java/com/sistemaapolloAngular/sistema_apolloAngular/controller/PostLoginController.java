package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class PostLoginController {


    @GetMapping("/rol")
    public ResponseEntity<Map<String, Object>> obtenerRol(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("authenticated", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Obtener el rol del usuario
            String rol = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_USER");

            // Obtener el nombre del usuario
            String username = authentication.getName();

            // Mapeo de roles a rutas en Angular
            String rutaAngular = mapearRolARuta(rol);

            response.put("authenticated", true);
            response.put("username", username);
            response.put("rol", rol);
            response.put("rolNombre", obtenerNombreRol(rol));
            response.put("ruta", rutaAngular);
            response.put("message", "Usuario autenticado correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener rol: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping("/redireccion")
    public ResponseEntity<Map<String, Object>> obtenerRedireccion(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("authenticated", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String rol = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_USER");

            String rutaAngular = mapearRolARuta(rol);

            response.put("success", true);
            response.put("rol", rol);
            response.put("ruta", rutaAngular);
            response.put("message", "Redirección obtenida correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener redirección: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/verificar")
    public ResponseEntity<Map<String, Object>> verificarAutenticacion(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("authenticated", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.ok(response);
            }

            String rol = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_USER");

            response.put("authenticated", true);
            response.put("username", authentication.getName());
            response.put("rol", rol);
            response.put("rolNombre", obtenerNombreRol(rol));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al verificar autenticación: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private String mapearRolARuta(String rol) {
        switch (rol) {
            case "ROLE_ADMIN":
                return "/admin-menu";
            case "ROLE_CAJERO":
                return "/cajero";
            case "ROLE_COCINERO":
                return "/cocinero";
            case "ROLE_DELIVERY":
                return "/delivery";
            case "ROLE_CLIENTE":
                return "/menu";
            default:
                return "/";
        }
    }

    private String obtenerNombreRol(String rol) {
        switch (rol) {
            case "ROLE_ADMIN":
                return "Administrador";
            case "ROLE_CAJERO":
                return "Cajero";
            case "ROLE_COCINERO":
                return "Cocinero";
            case "ROLE_DELIVERY":
                return "Delivery";
            case "ROLE_CLIENTE":
                return "Cliente";
            default:
                return "Usuario";
        }
    }
}