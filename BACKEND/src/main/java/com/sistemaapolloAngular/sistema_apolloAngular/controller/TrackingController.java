package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sistemaapolloAngular.sistema_apolloAngular.dto.TrackingCreateDTO;
import com.sistemaapolloAngular.sistema_apolloAngular.dto.TrackingResponseDTO;
import com.sistemaapolloAngular.sistema_apolloAngular.dto.TrackingUpdateLocationDTO;
import com.sistemaapolloAngular.sistema_apolloAngular.service.TrackingService;

@RestController
@RequestMapping("/api/tracking")
@CrossOrigin(origins = "http://localhost:4200")
public class TrackingController {

    @Autowired
    private TrackingService trackingService;

    /**
     * Crea una sesión de tracking cuando se asigna repartidor.
     * Lo llama el admin/cajero desde el panel cuando asigna el delivery.
     * POST /api/tracking/crear
     */
    @PostMapping("/crear")
    public ResponseEntity<TrackingResponseDTO> crearTracking(@RequestBody TrackingCreateDTO dto) {
        TrackingResponseDTO resp = trackingService.crearTracking(dto);
        return ResponseEntity.ok(resp);
    }

    /**
     * El repartidor envía su ubicación GPS periódicamente.
     * POST /api/tracking/ubicacion
     */
    @PostMapping("/ubicacion")
    public ResponseEntity<TrackingResponseDTO> actualizarUbicacion(@RequestBody TrackingUpdateLocationDTO dto) {
        TrackingResponseDTO resp = trackingService.actualizarUbicacion(dto);
        return ResponseEntity.ok(resp);
    }

    /**
     * El frontend del cliente consulta el estado del tracking.
     * GET /api/tracking/{numeroPedido}
     */
    @GetMapping("/{numeroPedido}")
    public ResponseEntity<TrackingResponseDTO> obtenerTracking(@PathVariable String numeroPedido) {
        TrackingResponseDTO resp = trackingService.obtenerTracking(numeroPedido);
        return ResponseEntity.ok(resp);
    }

    /**
     * Marca el delivery como entregado.
     * PUT /api/tracking/{numeroPedido}/completar
     */
    @PutMapping("/{numeroPedido}/completar")
    public ResponseEntity<TrackingResponseDTO> completarDelivery(@PathVariable String numeroPedido) {
        TrackingResponseDTO resp = trackingService.completarDelivery(numeroPedido);
        return ResponseEntity.ok(resp);
    }
}