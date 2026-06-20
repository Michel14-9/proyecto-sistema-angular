package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.dto.DireccionDTO;
import com.sistemaapolloAngular.sistema_apolloAngular.dto.DireccionRequestDTO;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Direccion;
import com.sistemaapolloAngular.sistema_apolloAngular.service.DireccionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/direcciones")
public class DireccionController {

    @Autowired
    private DireccionService direccionService;


    @GetMapping
    public ResponseEntity<?> obtenerDireccionesUsuario(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<Direccion> direcciones = direccionService.obtenerDireccionesUsuario(username);
            List<DireccionDTO> direccionesDTO = direcciones.stream()
                    .map(DireccionDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(direccionesDTO);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al obtener las direcciones: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    @PostMapping
    public ResponseEntity<?> guardarDireccion(@Valid @RequestBody DireccionRequestDTO direccionRequest,
                                              Authentication authentication) {
        try {
            String username = authentication.getName();

            Direccion direccion = direccionRequest.toEntity();
            Direccion direccionGuardada = direccionService.guardarDireccion(direccion, username);
            DireccionDTO direccionDTO = new DireccionDTO(direccionGuardada);

            return ResponseEntity.status(HttpStatus.CREATED).body(direccionDTO);

        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al guardar la dirección: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarDireccion(@PathVariable Long id,
                                                 @Valid @RequestBody DireccionRequestDTO direccionRequest,
                                                 Authentication authentication) {
        try {
            String username = authentication.getName();

            Direccion direccionActualizada = direccionRequest.toEntity();
            Direccion direccionActualizadaBD = direccionService.actualizarDireccion(id, direccionActualizada, username);
            DireccionDTO direccionDTO = new DireccionDTO(direccionActualizadaBD);

            return ResponseEntity.ok(direccionDTO);

        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al actualizar la dirección: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarDireccion(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();

            direccionService.eliminarDireccion(id, username);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Dirección eliminada correctamente");
            response.put("id", id.toString());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al eliminar la dirección: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PutMapping("/{id}/predeterminada")
    public ResponseEntity<?> marcarPredeterminada(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();

            direccionService.marcarPredeterminada(id, username);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Dirección establecida como predeterminada");
            response.put("id", id.toString());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al establecer la dirección como predeterminada: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping("/predeterminada")
    public ResponseEntity<?> obtenerDireccionPredeterminada(Authentication authentication) {
        try {
            String username = authentication.getName();

            return direccionService.obtenerDireccionPredeterminada(username)
                    .map(direccion -> ResponseEntity.ok(new DireccionDTO(direccion)))
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error al obtener la dirección predeterminada: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}