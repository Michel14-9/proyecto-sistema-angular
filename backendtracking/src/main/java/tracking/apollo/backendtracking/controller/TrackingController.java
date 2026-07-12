package tracking.apollo.backendtracking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tracking.apollo.backendtracking.dto.TrackingCreateDTO;
import tracking.apollo.backendtracking.dto.TrackingResponseDTO;
import tracking.apollo.backendtracking.dto.TrackingUpdateLocationDTO;
import tracking.apollo.backendtracking.service.TrackingService;

import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    @Autowired
    private TrackingService trackingService;

    /** Health check — confirma que el microservicio está vivo */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Tracking Delivery - Pollería Apollo",
                "port", "8082"
        ));
    }

    /** Admin/cajero crea sesión de tracking al asignar repartidor */
    @PostMapping("/crear")
    public ResponseEntity<TrackingResponseDTO> crearTracking(@RequestBody TrackingCreateDTO dto) {
        return ResponseEntity.ok(trackingService.crearTracking(dto));
    }

    /** Repartidor envía su GPS (automático cada 2-5 min desde Angular) */
    @PostMapping("/ubicacion")
    public ResponseEntity<TrackingResponseDTO> actualizarUbicacion(@RequestBody TrackingUpdateLocationDTO dto) {
        return ResponseEntity.ok(trackingService.actualizarUbicacion(dto));
    }

    /** Cliente consulta su tracking en sigue-tu-pedido */
    @GetMapping("/{numeroPedido}")
    public ResponseEntity<TrackingResponseDTO> obtenerTracking(@PathVariable String numeroPedido) {
        return ResponseEntity.ok(trackingService.obtenerTracking(numeroPedido));
    }

    /** Repartidor marca el pedido como entregado */
    @PutMapping("/{numeroPedido}/completar")
    public ResponseEntity<TrackingResponseDTO> completarDelivery(@PathVariable String numeroPedido) {
        return ResponseEntity.ok(trackingService.completarDelivery(numeroPedido));
    }
}