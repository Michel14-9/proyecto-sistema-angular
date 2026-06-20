package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Local;
import com.sistemaapolloAngular.sistema_apolloAngular.service.LocalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locales")
public class LocalController {

    private final LocalService localService;

    public LocalController(LocalService localService) {
        this.localService = localService;
    }


    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerTodosLosLocales() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Local> locales = localService.obtenerTodosLosLocales();

            response.put("success", true);
            response.put("data", locales);
            response.put("total", locales.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener los locales: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerLocalPorId(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Local local = localService.obtenerLocalPorId(id);

            if (local == null) {
                response.put("success", false);
                response.put("message", "Local no encontrado con ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            response.put("success", true);
            response.put("data", local);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener el local: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}