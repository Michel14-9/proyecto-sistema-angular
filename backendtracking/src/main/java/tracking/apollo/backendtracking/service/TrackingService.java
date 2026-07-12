package tracking.apollo.backendtracking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tracking.apollo.backendtracking.dto.TrackingCreateDTO;
import tracking.apollo.backendtracking.dto.TrackingResponseDTO;
import tracking.apollo.backendtracking.dto.TrackingUpdateLocationDTO;
import tracking.apollo.backendtracking.model.TrackingDelivery;
import tracking.apollo.backendtracking.repository.TrackingDeliveryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    @Value("${local.latitud:-14.070505106029492}")
    private double latLocal;

    @Value("${local.longitud:-75.72412960638542}")
    private double lngLocal;

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    @Autowired
    private TrackingDeliveryRepository trackingRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // ─── VELOCIDAD MEDIA EN CIUDAD (km/h) para ETA sin Google ────────────────
    private static final double VELOCIDAD_CIUDAD_KMH = 25.0;

    // ─── CREAR SESIÓN ─────────────────────────────────────────────────────────

    public TrackingResponseDTO crearTracking(TrackingCreateDTO dto) {
        // Evitar duplicados
        Optional<TrackingDelivery> existente = trackingRepository
                .findByNumeroPedidoAndEstadoIn(dto.getNumeroPedido(), List.of("READY", "ACTIVE"));
        if (existente.isPresent()) {
            log.info("Tracking ya existe para pedido {}", dto.getNumeroPedido());
            return toDTO(existente.get(), "Tracking ya existente para este pedido");
        }

        TrackingDelivery t = new TrackingDelivery();
        t.setPedidoId(dto.getPedidoId());
        t.setNumeroPedido(dto.getNumeroPedido());
        t.setDireccionCliente(dto.getDireccionCliente());
        t.setNombreRepartidor(dto.getNombreRepartidor());
        t.setLatLocal(latLocal);
        t.setLngLocal(lngLocal);
        // El repartidor empieza en el local
        t.setLatRepartidor(latLocal);
        t.setLngRepartidor(lngLocal);

        // Coordenadas del cliente
        if (dto.getLatCliente() != null && dto.getLngCliente() != null) {
            t.setLatCliente(dto.getLatCliente());
            t.setLngCliente(dto.getLngCliente());
        } else if (dto.getDireccionCliente() != null && tieneApiKey()) {
            // Intentar geocodificar con Google si hay API key activa
            double[] coords = geocodificarDireccion(dto.getDireccionCliente());
            if (coords != null) {
                t.setLatCliente(coords[0]);
                t.setLngCliente(coords[1]);
            }
        }

        // Calcular ETA y distancia inicial
        calcularDistanciaYEta(t);
        t.setEstado("READY");

        TrackingDelivery guardado = trackingRepository.save(t);
        log.info("Tracking creado - Pedido: {} | ETA: {}min | Distancia: {}km",
                dto.getNumeroPedido(), guardado.getEtaMinutos(), guardado.getDistanciaKm());
        return toDTO(guardado, "Tracking iniciado correctamente");
    }

    // ─── ACTUALIZAR UBICACIÓN DEL REPARTIDOR ─────────────────────────────────

    public TrackingResponseDTO actualizarUbicacion(TrackingUpdateLocationDTO dto) {
        Optional<TrackingDelivery> opt = trackingRepository
                .findByNumeroPedidoAndEstadoIn(dto.getNumeroPedido(), List.of("READY", "ACTIVE"));

        if (opt.isEmpty()) {
            TrackingResponseDTO err = new TrackingResponseDTO();
            err.setMensaje("No se encontró tracking activo para: " + dto.getNumeroPedido());
            return err;
        }

        TrackingDelivery t = opt.get();

        // Validación anti-salto GPS (>50 km instantáneo = señal inválida)
        if (t.getLatRepartidor() != null) {
            double salto = haversineKm(
                    t.getLatRepartidor(), t.getLngRepartidor(),
                    dto.getLatRepartidor(), dto.getLngRepartidor()
            );
            if (salto > 50.0) {
                log.warn("GPS jump detectado en pedido {}: {}km - señal ignorada",
                        dto.getNumeroPedido(), String.format("%.1f", salto));
                return toDTO(t, "Ubicación ignorada: señal GPS inválida");
            }
        }

        t.setLatRepartidor(dto.getLatRepartidor());
        t.setLngRepartidor(dto.getLngRepartidor());
        t.setUltimaActualizacion(LocalDateTime.now());

        // Primera actualización → activar tracking
        if ("READY".equals(t.getEstado())) {
            t.setEstado("ACTIVE");
            t.setFechaInicio(LocalDateTime.now());
            log.info("Tracking ACTIVADO para pedido {}", dto.getNumeroPedido());
        }

        // Recalcular distancia y ETA
        calcularDistanciaYEta(t);

        // Geofencing: alertar si repartidor está a menos de 300m
        if (t.getLatCliente() != null) {
            double metros = haversineKm(
                    dto.getLatRepartidor(), dto.getLngRepartidor(),
                    t.getLatCliente(), t.getLngCliente()
            ) * 1000;
            if (metros < 300) {
                log.info("DELIVERY_NEAR - Pedido {} - Repartidor a {}m del cliente",
                        dto.getNumeroPedido(), (int) metros);
            }
        }

        TrackingDelivery actualizado = trackingRepository.save(t);
        return toDTO(actualizado, "Ubicación actualizada");
    }

    // ─── CONSULTAR TRACKING ───────────────────────────────────────────────────

    public TrackingResponseDTO obtenerTracking(String numeroPedido) {
        Optional<TrackingDelivery> opt = trackingRepository
                .findTopByNumeroPedidoOrderByFechaCreacionDesc(numeroPedido);
        if (opt.isEmpty()) {
            TrackingResponseDTO err = new TrackingResponseDTO();
            err.setMensaje("No se encontró tracking para el pedido: " + numeroPedido);
            return err;
        }
        return toDTO(opt.get(), "OK");
    }

    // ─── COMPLETAR DELIVERY ───────────────────────────────────────────────────

    public TrackingResponseDTO completarDelivery(String numeroPedido) {
        Optional<TrackingDelivery> opt = trackingRepository
                .findByNumeroPedidoAndEstadoIn(numeroPedido, List.of("READY", "ACTIVE"));
        if (opt.isEmpty()) {
            TrackingResponseDTO err = new TrackingResponseDTO();
            err.setMensaje("No se encontró tracking activo para: " + numeroPedido);
            return err;
        }
        TrackingDelivery t = opt.get();
        t.setEstado("COMPLETED");
        t.setFechaCompletado(LocalDateTime.now());
        t.setEtaMinutos(0);
        t.setDistanciaKm(0.0);
        TrackingDelivery guardado = trackingRepository.save(t);
        log.info("Delivery COMPLETADO para pedido {}", numeroPedido);
        return toDTO(guardado, "Delivery completado exitosamente");
    }

    // ─── LÓGICA ETA Y DISTANCIA ───────────────────────────────────────────────

    private void calcularDistanciaYEta(TrackingDelivery t) {
        if (t.getLatCliente() == null || t.getLatRepartidor() == null) {
            t.setEtaMinutos(30);
            t.setDistanciaKm(5.0);
            return;
        }

        if (tieneApiKey()) {
            // Intentar con Google Distance Matrix
            boolean exito = calcularEtaConGoogle(t);
            if (exito) return;
        }

        // Fallback: Haversine + velocidad media urbana
        double distKm = haversineKm(
                t.getLatRepartidor(), t.getLngRepartidor(),
                t.getLatCliente(), t.getLngCliente()
        );
        // Factor de corrección por calles reales (la ruta real suele ser ~30% más larga)
        double distCorregida = distKm * 1.3;
        int etaMin = (int) Math.ceil((distCorregida / VELOCIDAD_CIUDAD_KMH) * 60);

        t.setDistanciaKm(Math.round(distCorregida * 10.0) / 10.0);
        t.setEtaMinutos(Math.max(etaMin, 1));
    }

    @SuppressWarnings("unchecked")
    private boolean calcularEtaConGoogle(TrackingDelivery t) {
        try {
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/distancematrix/json" +
                    "?origins=%s,%s&destinations=%s,%s&mode=driving&departure_time=now&key=%s",
                    t.getLatRepartidor(), t.getLngRepartidor(),
                    t.getLatCliente(), t.getLngCliente(),
                    googleMapsApiKey
            );
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null || !"OK".equals(resp.get("status"))) return false;

            List<Map<String, Object>> rows = (List<Map<String, Object>>) resp.get("rows");
            if (rows == null || rows.isEmpty()) return false;

            List<Map<String, Object>> elements = (List<Map<String, Object>>) rows.get(0).get("elements");
            if (elements == null || elements.isEmpty()) return false;

            Map<String, Object> el = elements.get(0);
            if (!"OK".equals(el.get("status"))) return false;

            Map<String, Object> duration = (Map<String, Object>) el.getOrDefault("duration_in_traffic", el.get("duration"));
            Map<String, Object> distance = (Map<String, Object>) el.get("distance");
            if (duration == null || distance == null) return false;

            int segundos = ((Number) duration.get("value")).intValue();
            int metros = ((Number) distance.get("value")).intValue();

            t.setEtaMinutos((int) Math.ceil(segundos / 60.0));
            t.setDistanciaKm(Math.round((metros / 1000.0) * 10.0) / 10.0);
            log.info("ETA calculado con Google Maps: {}min / {}km", t.getEtaMinutos(), t.getDistanciaKm());
            return true;

        } catch (Exception e) {
            log.warn("Google Distance Matrix no disponible, usando Haversine: {}", e.getMessage());
            return false;
        }
    }

    // ─── GEOCODING (solo si hay API key activa) ───────────────────────────────

    @SuppressWarnings("unchecked")
    private double[] geocodificarDireccion(String direccion) {
        try {
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/geocode/json?address=%s,Ica,Peru&key=%s",
                    direccion.replace(" ", "+"), googleMapsApiKey
            );
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null || !"OK".equals(resp.get("status"))) return null;

            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            if (results == null || results.isEmpty()) return null;

            Map<String, Object> location = (Map<String, Object>)
                    ((Map<String, Object>) results.get(0).get("geometry")).get("location");
            return new double[]{
                    ((Number) location.get("lat")).doubleValue(),
                    ((Number) location.get("lng")).doubleValue()
            };
        } catch (Exception e) {
            log.warn("Geocoding no disponible: {}", e.getMessage());
            return null;
        }
    }

    // ─── HAVERSINE ────────────────────────────────────────────────────────────

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private boolean tieneApiKey() {
        return googleMapsApiKey != null && !googleMapsApiKey.isBlank();
    }

    // ─── MAPPER ───────────────────────────────────────────────────────────────

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

        // URL de navegación para el repartidor (Google Maps gratis, sin API key)
        if (t.getLatCliente() != null && t.getLngCliente() != null) {
            String url = String.format(
                    "https://www.google.com/maps/dir/?api=1&destination=%s,%s&travelmode=driving",
                    t.getLatCliente(), t.getLngCliente()
            );
            dto.setUrlNavegacion(url);
        }

        return dto;
    }
}