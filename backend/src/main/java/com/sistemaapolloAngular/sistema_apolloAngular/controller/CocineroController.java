package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cocinero")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class CocineroController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ✅ FIX: los pedidos PRESENCIALES (creados y cobrados en caja) nunca deben
    // aparecer en las bandejas de cocina.
    private boolean noEsPresencial(Pedido p) {
        return !"PRESENCIAL".equals(p.getOrigen());
    }

    // ✅ OBTENER PEDIDOS POR PREPARAR (PAGADOS)
    @GetMapping("/pedidos-por-preparar")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerPedidosPorPreparar() {
        try {
            List<Pedido> pedidos = pedidoRepository.findAllWithItemsAndProducts()
                    .stream()
                    .filter(p -> "PAGADO".equals(p.getEstado()) || "CONFIRMADO".equals(p.getEstado()))
                    .filter(this::noEsPresencial)
                    .collect(Collectors.toList());

            List<Map<String, Object>> pedidosDTO = crearPedidosDTO(pedidos);
            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al cargar pedidos por preparar: " + e.getMessage()
            ));
        }
    }

    // ✅ OBTENER PEDIDOS EN PREPARACIÓN
    @GetMapping("/pedidos-en-preparacion")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerPedidosEnPreparacion() {
        try {
            List<Pedido> pedidos = pedidoRepository.findAllWithItemsAndProducts()
                    .stream()
                    .filter(p -> "PREPARACION".equals(p.getEstado()))
                    .filter(this::noEsPresencial)
                    .collect(Collectors.toList());

            List<Map<String, Object>> pedidosDTO = crearPedidosDTO(pedidos);
            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al cargar pedidos en preparación: " + e.getMessage()
            ));
        }
    }

    // ✅ OBTENER PEDIDOS LISTOS PARA ENTREGA (SOLO estado LISTO)
    // Este endpoint es para la COLUMNA de pedidos listos en el frontend
    @GetMapping("/pedidos-listos-hoy")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerPedidosListosHoy() {
        try {
            // ✅ SOLO pedidos que están en estado LISTO actualmente
            // No incluir EN_CAMINO ni ENTREGADO porque ya fueron tomados por delivery
            List<Pedido> pedidos = pedidoRepository.findAllWithItemsAndProducts()
                    .stream()
                    .filter(p -> "LISTO".equals(p.getEstado()))
                    .filter(this::noEsPresencial)
                    .collect(Collectors.toList());

            List<Map<String, Object>> pedidosDTO = crearPedidosDTO(pedidos);
            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al cargar pedidos listos: " + e.getMessage()
            ));
        }
    }

    // ✅ INICIAR PREPARACIÓN
    @PostMapping("/iniciar-preparacion/{pedidoId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> iniciarPreparacion(@PathVariable Long pedidoId,
                                                Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                        "status", "ERROR",
                        "message", "No autenticado"
                ));
            }

            boolean hasCocineroRole = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority ->
                            grantedAuthority.getAuthority().equals("ROLE_COCINERO"));

            if (!hasCocineroRole) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "ERROR",
                        "message", "No tiene permisos de cocinero"
                ));
            }

            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
            if (pedidoOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "Pedido no encontrado"
                ));
            }

            Pedido pedido = pedidoOpt.get();

            if ("PRESENCIAL".equals(pedido.getOrigen())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "Este pedido es presencial (de caja) y no pasa por cocina"
                ));
            }

            if (!"PAGADO".equals(pedido.getEstado()) && !"CONFIRMADO".equals(pedido.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "El pedido no está en estado PAGADO o CONFIRMADO. Estado actual: " + pedido.getEstado()
                ));
            }

            pedido.setEstado("PREPARACION");
            pedido.setFechaActualizacion(LocalDateTime.now());
            Pedido pedidoActualizado = pedidoRepository.save(pedido);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Preparación iniciada correctamente");
            response.put("numeroPedido", pedidoActualizado.getNumeroPedido());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    // ✅ MARCAR COMO LISTO
    @PostMapping("/marcar-listo/{pedidoId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> marcarComoListo(@PathVariable Long pedidoId,
                                             Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                        "status", "ERROR",
                        "message", "No autenticado"
                ));
            }

            boolean hasCocineroRole = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority ->
                            grantedAuthority.getAuthority().equals("ROLE_COCINERO"));

            if (!hasCocineroRole) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "ERROR",
                        "message", "No tiene permisos de cocinero"
                ));
            }

            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
            if (pedidoOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "Pedido no encontrado"
                ));
            }

            Pedido pedido = pedidoOpt.get();

            if (!"PREPARACION".equals(pedido.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "El pedido no está en estado PREPARACION. Estado actual: " + pedido.getEstado()
                ));
            }

            pedido.setEstado("LISTO");
            pedido.setFechaActualizacion(LocalDateTime.now());
            Pedido pedidoActualizado = pedidoRepository.save(pedido);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Pedido marcado como LISTO correctamente");
            response.put("numeroPedido", pedidoActualizado.getNumeroPedido());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    // ✅ MÉTRICAS DE COCINA - CORREGIDO
    @GetMapping("/metricas-cocina")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerMetricasCocina() {
        try {
            List<Pedido> todosLosPedidos = pedidoRepository.findAllWithItemsAndProducts()
                    .stream()
                    .filter(this::noEsPresencial)
                    .collect(Collectors.toList());

            long porPreparar = todosLosPedidos.stream()
                    .filter(p -> "PAGADO".equals(p.getEstado()) || "CONFIRMADO".equals(p.getEstado()))
                    .count();

            long enPreparacion = todosLosPedidos.stream()
                    .filter(p -> "PREPARACION".equals(p.getEstado()))
                    .count();

            LocalDate hoy = LocalDate.now();
            LocalDateTime hoyInicio = hoy.atStartOfDay();
            LocalDateTime hoyFin = hoy.atTime(23, 59, 59);

            // ✅ CORREGIDO: Contar pedidos que fueron LISTO hoy
            // Incluye pedidos en estado LISTO, EN_CAMINO y ENTREGADO
            // que hayan sido actualizados hoy (cuando pasaron a LISTO)
            long listosHoy = todosLosPedidos.stream()
                    .filter(p -> {
                        // ✅ Está en LISTO actualmente y fue creado hoy
                        if ("LISTO".equals(p.getEstado())) {
                            return p.getFecha() != null &&
                                    !p.getFecha().isBefore(hoyInicio) &&
                                    !p.getFecha().isAfter(hoyFin);
                        }
                        // ✅ Está en EN_CAMINO o ENTREGADO (pasó por LISTO)
                        // Verificar si la fecha de actualización es de hoy
                        if ("EN_CAMINO".equals(p.getEstado()) || "ENTREGADO".equals(p.getEstado())) {
                            if (p.getFechaActualizacion() != null) {
                                return !p.getFechaActualizacion().isBefore(hoyInicio) &&
                                        !p.getFechaActualizacion().isAfter(hoyFin);
                            }
                            if (p.getFecha() != null) {
                                return !p.getFecha().isBefore(hoyInicio) &&
                                        !p.getFecha().isAfter(hoyFin);
                            }
                        }
                        return false;
                    })
                    .count();

            // Calcular tiempo promedio - CORREGIDO
            double tiempoPromedio = 0;

            // ✅ Obtener todos los pedidos que pasaron por LISTO hoy
            List<Pedido> pedidosListos = todosLosPedidos.stream()
                    .filter(p -> {
                        if ("LISTO".equals(p.getEstado()) || "EN_CAMINO".equals(p.getEstado()) || "ENTREGADO".equals(p.getEstado())) {
                            if (p.getFechaActualizacion() != null) {
                                return !p.getFechaActualizacion().isBefore(hoyInicio) &&
                                        !p.getFechaActualizacion().isAfter(hoyFin);
                            }
                            return false;
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            if (!pedidosListos.isEmpty()) {
                long totalMinutos = 0;
                int contador = 0;
                for (Pedido p : pedidosListos) {
                    if (p.getFecha() != null && p.getFechaActualizacion() != null) {
                        long minutos = ChronoUnit.MINUTES.between(p.getFecha(), p.getFechaActualizacion());
                        if (minutos > 0 && minutos < 480) {
                            totalMinutos += minutos;
                            contador++;
                        }
                    }
                }
                if (contador > 0) {
                    tiempoPromedio = (double) totalMinutos / contador;
                }
            }

            Map<String, Object> metricas = new HashMap<>();
            metricas.put("success", true);
            metricas.put("totalPorPreparar", porPreparar);
            metricas.put("totalEnPreparacion", enPreparacion);
            metricas.put("totalListosHoy", listosHoy);
            metricas.put("tiempoPromedio", Math.round(tiempoPromedio));

            return ResponseEntity.ok(metricas);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ✅ DETALLE DE PEDIDO
    @GetMapping("/pedido/{pedidoId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerDetallePedido(@PathVariable Long pedidoId) {
        try {
            Optional<Pedido> pedidoOpt = pedidoRepository.findByIdWithItems(pedidoId);

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();
                Map<String, Object> pedidoDTO = crearPedidoDetalleDTO(pedido);
                return ResponseEntity.ok(pedidoDTO);
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Pedido no encontrado"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private List<Map<String, Object>> crearPedidosDTO(List<Pedido> pedidos) {
        List<Map<String, Object>> pedidosDTO = new ArrayList<>();

        for (Pedido pedido : pedidos) {
            Map<String, Object> pedidoDTO = new HashMap<>();
            pedidoDTO.put("id", pedido.getId());
            pedidoDTO.put("numeroPedido", pedido.getNumeroPedido());
            pedidoDTO.put("total", pedido.getTotal());
            pedidoDTO.put("fecha", pedido.getFecha());
            pedidoDTO.put("fechaPedido", pedido.getFecha());
            pedidoDTO.put("estado", pedido.getEstado());
            pedidoDTO.put("metodoPago", pedido.getMetodoPago());
            pedidoDTO.put("tipoEntrega", pedido.getTipoEntrega());
            pedidoDTO.put("direccionEntrega", pedido.getDireccionEntrega());
            pedidoDTO.put("observaciones", pedido.getObservaciones());
            pedidoDTO.put("origen", pedido.getOrigen());
            pedidoDTO.put("fechaActualizacion", pedido.getFechaActualizacion());

            if (pedido.getUsuario() != null) {
                Usuario usuario = pedido.getUsuario();
                Map<String, String> usuarioDTO = new HashMap<>();
                usuarioDTO.put("nombres", usuario.getNombres() != null ? usuario.getNombres() : "");
                usuarioDTO.put("apellidos", usuario.getApellidos() != null ? usuario.getApellidos() : "");
                usuarioDTO.put("telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "");
                pedidoDTO.put("cliente", usuarioDTO);
            } else {
                pedidoDTO.put("cliente", null);
            }

            List<Map<String, Object>> itemsDTO = new ArrayList<>();
            if (pedido.getItems() != null) {
                for (ItemPedido item : pedido.getItems()) {
                    Map<String, Object> itemDTO = new HashMap<>();
                    itemDTO.put("nombreProducto", item.getNombreProducto());
                    itemDTO.put("nombreProductoSeguro", item.getNombreProductoSeguro());
                    itemDTO.put("cantidad", item.getCantidad());
                    itemDTO.put("precio", item.getPrecio());
                    itemDTO.put("subtotal", item.getSubtotal());
                    itemsDTO.add(itemDTO);
                }
            }
            pedidoDTO.put("items", itemsDTO);

            pedidosDTO.add(pedidoDTO);
        }

        return pedidosDTO;
    }

    private Map<String, Object> crearPedidoDetalleDTO(Pedido pedido) {
        Map<String, Object> pedidoDTO = new HashMap<>();
        pedidoDTO.put("id", pedido.getId());
        pedidoDTO.put("numeroPedido", pedido.getNumeroPedido());
        pedidoDTO.put("total", pedido.getTotal());
        pedidoDTO.put("fecha", pedido.getFecha());
        pedidoDTO.put("estado", pedido.getEstado());
        pedidoDTO.put("metodoPago", pedido.getMetodoPago());
        pedidoDTO.put("tipoEntrega", pedido.getTipoEntrega());
        pedidoDTO.put("direccionEntrega", pedido.getDireccionEntrega());
        pedidoDTO.put("observaciones", pedido.getObservaciones());
        pedidoDTO.put("instrucciones", pedido.getInstrucciones());
        pedidoDTO.put("origen", pedido.getOrigen());

        if (pedido.getUsuario() != null) {
            Usuario usuario = pedido.getUsuario();
            Map<String, String> usuarioDTO = new HashMap<>();
            usuarioDTO.put("nombres", usuario.getNombres() != null ? usuario.getNombres() : "");
            usuarioDTO.put("apellidos", usuario.getApellidos() != null ? usuario.getApellidos() : "");
            usuarioDTO.put("telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "");
            pedidoDTO.put("cliente", usuarioDTO);
        }

        List<Map<String, Object>> itemsDTO = new ArrayList<>();
        if (pedido.getItems() != null) {
            for (ItemPedido item : pedido.getItems()) {
                Map<String, Object> itemDTO = new HashMap<>();
                itemDTO.put("nombreProducto", item.getNombreProducto());
                itemDTO.put("cantidad", item.getCantidad());
                itemDTO.put("precio", item.getPrecio());
                itemDTO.put("subtotal", item.getSubtotal());
                itemsDTO.add(itemDTO);
            }
        }
        pedidoDTO.put("items", itemsDTO);

        return pedidoDTO;
    }
}