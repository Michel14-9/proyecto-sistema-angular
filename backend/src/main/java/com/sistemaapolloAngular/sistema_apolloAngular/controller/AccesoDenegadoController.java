package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AccesoDenegadoController {

    @GetMapping("/acceso-denegado")
    public ResponseEntity<Map<String, Object>> accesoDenegado() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("mensaje", "Acceso denegado. No tienes permisos para este recurso.");
        response.put("codigo", 403);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}