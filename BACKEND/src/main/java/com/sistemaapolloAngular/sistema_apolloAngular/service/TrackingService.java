package com.sistemaapolloAngular.sistema_apolloAngular.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sistemaapolloAngular.sistema_apolloAngular.dto.TrackingCreateDTO;
import com.sistemaapolloAngular.sistema_apolloAngular.dto.TrackingResponseDTO;
import com.sistemaapolloAngular.sistema_apolloAngular.dto.TrackingUpdateLocationDTO;
import com.sistemaapolloAngular.sistema_apolloAngular.model.TrackingDelivery;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.TrackingDeliveryRepository;

@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    // Coordenadas fijas del local (Pollería Apollo - Ica, Perú)
    private static final double LAT_LOCAL = -14.0678;
    private static final double LNG_LOCAL = -75.7356;

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    @Autowired
    private TrackingDeliveryRepository trackingRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // ─── CREAR SESIÓN DE TRACKING ─────────────────────────────────────────────

    public TrackingResponseDTO crearTracking(TrackingCreateDTO dto) {
        // Evita duplicados: si ya existe un tracking activo para este pedido, lo devuelve
        Optional<TrackingDelivery> existente = trackingRepository
                .findByNumeroPedidoAndEstadoIn(dto.getNumeroPedido(), List.of("READY", "ACTIVE"));
        if (existente.isPresent()) {
            return toDTO(existente.get(), "Tracking ya existente para este pedido");
        }

        TrackingDelivery tracking = new TrackingDelivery();
        tracking.setPedidoId(dto.getPedidoId());
        tracking.setNumeroPedido(dto.getNumeroPedido());
        tracking.setDireccionCliente(dto.getDireccionCliente());
        tracking.setNombreRepartidor(dto.getNombreRepartidor());
        tracking.setLatLocal(LAT_LOCAL);
        tracking.setLngLocal(LNG_LOCAL);

        // Posición inicial del repartidor = local
        tracking.setLatRepartidor(LAT_LOCAL);
        tracking.setLngRepartidor(LNG_LOCAL);

        // Obtener coordenadas del cliente
        if (dto.getLatCliente() != null && dto.getLngCliente() != null) {
            // Coordenadas directas (mejor opción)
            tracking.setLatCliente(dto.getLatCliente());
            tracking.setLngCliente(dto.getLngCliente());
        } else if (dto.getDireccionCliente() != null && !googleMapsApiKey.isBlank()) {
            // Geocodificar dirección textual
            double[] coords = geocodificarDireccion(dto.getDireccionCliente());
            if (coords != null) {
                tracking.setLatCliente(coords[0]);
                tracking.setLngCliente(coords[1]);
            }
        }

        // Calcular ETA inicial si tenemos coordenadas completas
        if (tracking.getLatCliente() != null && !googleMapsApiKey.isBlank()) {
            calcularETA(tracking);
        } else {
            tracking.setEtaMinutos(30); // valor por defecto
            tracking.setDistanciaKm(5.0);
        }

        tracking.setEstado("READY");
        TrackingDelivery guardado = trackingRepository.save(tracking);
        log.info("Tracking creado para pedido {}", dto.getNumeroPedido());
        return toDTO(guardado, "Tracking iniciado correctamente");
    }

    // ─── ACTUALIZAR UBICACIÓN DEL REPARTIDOR ─────────────────────────────────

    public TrackingResponseDTO actualizarUbicacion(TrackingUpdateLocationDTO dto) {
        Optional<TrackingDelivery> opt = trackingRepository
                .findByNumeroPedidoAndEstadoIn(dto.getNumeroPedido(), List.of("READY", "ACTIVE"));

        if (opt.isEmpty()) {
            TrackingResponseDTO err = new TrackingResponseDTO();
            err.setMensaje("No se encontró tracking activo para el pedido: " + dto.getNumeroPedido());
            return err;
        }

        TrackingDelivery tracking = opt.get();

        // Validación anti-saltos de GPS (más de 50 km en un update = ignorar)
        if (tracking.getLatRepartidor() != null) {
            double distSalto = calcularDistanciaHaversine(
                    tracking.getLatRepartidor(), tracking.getLngRepartidor(),
                    dto.getLatRepartidor(), dto.getLngRepartidor()
            );
            if (distSalto > 50.0) {
                log.warn("GPS jump detectado en pedido {}: {}km", dto.getNumeroPedido(), distSalto);
                return toDTO(tracking, "Ubicación ignorada: salto GPS detectado");
            }
        }

        tracking.setLatRepartidor(dto.getLatRepartidor());
        tracking.setLngRepartidor(dto.getLngRepartidor());
        tracking.setUltimaActualizacion(LocalDateTime.now());

        if ("READY".equals(tracking.getEstado())) {
            tracking.setEstado("ACTIVE");
            tracking.setFechaInicio(LocalDateTime.now());
        }

        // Recalcular ETA con Distance Matrix si tenemos API key
        if (tracking.getLatCliente() != null && !googleMapsApiKey.isBlank()) {
            calcularETA(tracking);
        } else if (tracking.getLatCliente() != null) {
            // Fallback: cálculo con Haversine sin Google
            double distancia = calcularDistanciaHaversine(
                    dto.getLatRepartidor(), dto.getLngRepartidor(),
                    tracking.getLatCliente(), tracking.getLngCliente()
            );
            tracking.setDistanciaKm(Math.round(distancia * 10.0) / 10.0);
            tracking.setEtaMinutos((int) Math.ceil(distancia / 0.5)); // ~30 km/h en ciudad
        }

        // Geofencing: si el repartidor está a menos de 300m del cliente
        if (tracking.getLatCliente() != null) {
            double distanciaMetros = calcularDistanciaHaversine(
                    dto.getLatRepartidor(), dto.getLngRepartidor(),
                    tracking.getLatCliente(), tracking.getLngCliente()
            ) * 1000;
            if (distanciaMetros < 300) {
                log.info("DELIVERY_NEAR: Repartidor a {}m del cliente - Pedido {}",
                        (int) distanciaMetros, dto.getNumeroPedido());
            }
        }

        TrackingDelivery actualizado = trackingRepository.save(tracking);
        return toDTO(actualizado, "Ubicación actualizada");
    }

    // ─── OBTENER ESTADO ACTUAL ─────────────────────────────────────────────────

    public TrackingResponseDTO obtenerTracking(String numeroPedido) {
        Optional<TrackingDelivery> opt = trackingRepository.findByNumeroPedido(numeroPedido);
        if (opt.isEmpty()) {
            TrackingResponseDTO err = new TrackingResponseDTO();
            err.setMensaje("No se encontró tracking para el pedido: " + numeroPedido);
            return err;
        }
        return toDTO(opt.get(), "OK");
    }

    // ─── COMPLETAR DELIVERY ────────────────────────────────────────────────────

    public TrackingResponseDTO completarDelivery(String numeroPedido) {
        Optional<TrackingDelivery> opt = trackingRepository
                .findByNumeroPedidoAndEstadoIn(numeroPedido, List.of("READY", "ACTIVE"));
        if (opt.isEmpty()) {
            TrackingResponseDTO err = new TrackingResponseDTO();
            err.setMensaje("No se encontró tracking activo para: " + numeroPedido);
            return err;
        }
        TrackingDelivery tracking = opt.get();
        tracking.setEstado("COMPLETED");
        tracking.setFechaCompletado(LocalDateTime.now());
        tracking.setEtaMinutos(0);
        TrackingDelivery guardado = trackingRepository.save(tracking);
        log.info("Delivery completado para pedido {}", numeroPedido);
        return toDTO(guardado, "Delivery completado exitosamente");
    }

    // ─── GOOGLE GEOCODING API ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private double[] geocodificarDireccion(String direccion) {
        try {
            String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                    + direccion.replace(" ", "+") + ",Ica,Peru&key=" + googleMapsApiKey;
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && "OK".equals(resp.get("status"))) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
                if (!results.isEmpty()) {
                    Map<String, Object> geometry = (Map<String, Object>) results.get(0).get("geometry");
                    Map<String, Object> location = (Map<String, Object>) geometry.get("location");
                    double lat = ((Number) location.get("lat")).doubleValue();
                    double lng = ((Number) location.get("lng")).doubleValue();
                    return new double[]{lat, lng};
                }
            }
        } catch (Exception e) {
            log.error("Error en Geocoding API: {}", e.getMessage());
        }
        return null;
    }

    // ─── GOOGLE DISTANCE MATRIX API ───────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void calcularETA(TrackingDelivery tracking) {
        try {
            String origins = tracking.getLatRepartidor() + "," + tracking.getLngRepartidor();
            String destinations = tracking.getLatCliente() + "," + tracking.getLngCliente();
            String url = "https://maps.googleapis.com/maps/api/distancematrix/json"
                    + "?origins=" + origins
                    + "&destinations=" + destinations
                    + "&mode=driving"
                    + "&departure_time=now"
                    + "&key=" + googleMapsApiKey;

            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && "OK".equals(resp.get("status"))) {
                List<Map<String, Object>> rows = (List<Map<String, Object>>) resp.get("rows");
                if (!rows.isEmpty()) {
                    List<Map<String, Object>> elements = (List<Map<String, Object>>) rows.get(0).get("elements");
                    if (!elements.isEmpty() && "OK".equals(elements.get(0).get("status"))) {
                        Map<String, Object> element = elements.get(0);
                        Map<String, Object> duration = (Map<String, Object>) element.get("duration_in_traffic");
                        if (duration == null) duration = (Map<String, Object>) element.get("duration");
                        Map<String, Object> distance = (Map<String, Object>) element.get("distance");

                        int segundos = ((Number) duration.get("value")).intValue();
                        int metros = ((Number) distance.get("value")).intValue();

                        tracking.setEtaMinutos((int) Math.ceil(segundos / 60.0));
                        tracking.setDistanciaKm(Math.round((metros / 1000.0) * 10.0) / 10.0);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error en Distance Matrix API: {}", e.getMessage());
        }
    }

    // ─── HAVERSINE (cálculo de distancia sin Google) ───────────────────────────

    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ─── MAPPER ────────────────────────────────────────────────────────────────

    private TrackingResponseDTO toDTO(TrackingDelivery t, String mensaje) {
        TrackingResponseDTO dto = new TrackingResponseDTO();
        dto.setId(t.getId());
        dto.setNumeroPedido(t.getNumeroPedido());
        dto.setEstado(t.getEstado());
        dto.setLatRepartidor(t.getLatRepartidor());
        dto.setLngRepartidor(t.getLngRepartidor());
        dto.setLatCliente(t.getLatCliente());
        dto.setLngCliente(t.getLngCliente());
        dto.setLatLocal(t.getLatLocal());
        dto.setLngLocal(t.getLngLocal());
        dto.setDireccionCliente(t.getDireccionCliente());
        dto.setEtaMinutos(t.getEtaMinutos());
        dto.setDistanciaKm(t.getDistanciaKm());
        dto.setNombreRepartidor(t.getNombreRepartidor());
        dto.setMensaje(mensaje);
        return dto;
    }
}