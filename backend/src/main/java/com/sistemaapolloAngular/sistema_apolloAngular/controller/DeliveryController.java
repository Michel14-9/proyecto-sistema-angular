package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
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
@RequestMapping("/delivery")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class DeliveryController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/pedidos-para-entrega")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerPedidosParaEntrega() {
        try {
            List<Pedido> pedidos = pedidoRepository.findByEstadoWithItems("LISTO");

            if (pedidos == null || pedidos.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Map<String, Object>> pedidosDTO = mapearPedidosParaFrontend(pedidos);
            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error en pedidos-para-entrega: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/pedidos-en-camino")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerPedidosEnCamino() {
        try {
            List<Pedido> pedidos = pedidoRepository.findByEstadoWithItems("EN_CAMINO");

            if (pedidos == null || pedidos.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Map<String, Object>> pedidosDTO = mapearPedidosParaFrontend(pedidos);
            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error en pedidos-en-camino: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/pedidos-entregados-hoy")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerPedidosEntregadosHoy() {
        try {
            LocalDate hoy = LocalDate.now();
            LocalDateTime hoyInicio = hoy.atStartOfDay();
            LocalDateTime hoyFin = hoy.atTime(23, 59, 59);

            List<Pedido> pedidos = pedidoRepository.findByEstadoWithItems("ENTREGADO")
                    .stream()
                    .filter(p -> p.getFecha() != null)
                    .filter(p -> !p.getFecha().isBefore(hoyInicio) && !p.getFecha().isAfter(hoyFin))
                    .collect(Collectors.toList());

            List<Map<String, Object>> pedidosDTO = mapearPedidosParaFrontend(pedidos);
            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error en pedidos-entregados-hoy: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping("/iniciar-entrega/{pedidoId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> iniciarEntrega(@PathVariable Long pedidoId,
                                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("status", "ERROR");
                response.put("message", "No autenticado");
                return ResponseEntity.status(401).body(response);
            }

            boolean hasDeliveryRole = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority ->
                            grantedAuthority.getAuthority().equals("ROLE_DELIVERY"));

            if (!hasDeliveryRole) {
                response.put("status", "ERROR");
                response.put("message", "No tiene permisos de delivery");
                return ResponseEntity.status(403).body(response);
            }

            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
            if (pedidoOpt.isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "Pedido no encontrado");
                return ResponseEntity.badRequest().body(response);
            }

            Pedido pedido = pedidoOpt.get();

            if (!"LISTO".equals(pedido.getEstado())) {
                response.put("status", "ERROR");
                response.put("message", "El pedido no está en estado LISTO. Estado actual: " + pedido.getEstado());
                return ResponseEntity.badRequest().body(response);
            }

            pedido.setEstado("EN_CAMINO");
            pedido.setFechaActualizacion(LocalDateTime.now());
            pedidoRepository.save(pedido);

            response.put("status", "SUCCESS");
            response.put("message", "Entrega iniciada correctamente");
            response.put("numeroPedido", pedido.getNumeroPedido());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/marcar-entregado/{pedidoId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> marcarComoEntregado(@PathVariable Long pedidoId,
                                                                   Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("status", "ERROR");
                response.put("message", "No autenticado");
                return ResponseEntity.status(401).body(response);
            }

            boolean hasDeliveryRole = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority ->
                            grantedAuthority.getAuthority().equals("ROLE_DELIVERY"));

            if (!hasDeliveryRole) {
                response.put("status", "ERROR");
                response.put("message", "No tiene permisos de delivery");
                return ResponseEntity.status(403).body(response);
            }

            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
            if (pedidoOpt.isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "Pedido no encontrado");
                return ResponseEntity.badRequest().body(response);
            }

            Pedido pedido = pedidoOpt.get();

            if (!"EN_CAMINO".equals(pedido.getEstado())) {
                response.put("status", "ERROR");
                response.put("message", "El pedido no está en estado EN_CAMINO. Estado actual: " + pedido.getEstado());
                return ResponseEntity.badRequest().body(response);
            }

            pedido.setEstado("ENTREGADO");
            pedido.setFechaActualizacion(LocalDateTime.now());
            pedidoRepository.save(pedido);

            response.put("status", "SUCCESS");
            response.put("message", "Pedido marcado como ENTREGADO correctamente");
            response.put("numeroPedido", pedido.getNumeroPedido());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/metricas-delivery")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> obtenerMetricasDelivery() {
        Map<String, Object> metricas = new HashMap<>();

        try {
            List<Pedido> pedidosParaEntregar = pedidoRepository.findByEstadoWithItems("LISTO");
            List<Pedido> pedidosEnCamino = pedidoRepository.findByEstadoWithItems("EN_CAMINO");

            LocalDate hoy = LocalDate.now();
            LocalDateTime hoyInicio = hoy.atStartOfDay();
            LocalDateTime hoyFin = hoy.atTime(23, 59, 59);

            List<Pedido> pedidosEntregadosHoy = pedidoRepository.findByEstadoWithItems("ENTREGADO")
                    .stream()
                    .filter(p -> p.getFecha() != null)
                    .filter(p -> !p.getFecha().isBefore(hoyInicio) && !p.getFecha().isAfter(hoyFin))
                    .collect(Collectors.toList());

            metricas.put("success", true);
            metricas.put("totalParaEntregar", pedidosParaEntregar != null ? pedidosParaEntregar.size() : 0);
            metricas.put("totalEnCamino", pedidosEnCamino != null ? pedidosEnCamino.size() : 0);
            metricas.put("totalEntregadosHoy", pedidosEntregadosHoy.size());

        } catch (Exception e) {
            e.printStackTrace();
            metricas.put("success", false);
            metricas.put("error", e.getMessage());
            metricas.put("totalParaEntregar", 0);
            metricas.put("totalEnCamino", 0);
            metricas.put("totalEntregadosHoy", 0);
        }

        return ResponseEntity.ok(metricas);
    }

    @GetMapping("/pedido/{pedidoId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerDetallePedidoDelivery(@PathVariable Long pedidoId) {
        try {
            Optional<Pedido> pedidoOpt = pedidoRepository.findByIdWithItems(pedidoId);

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();
                Map<String, Object> pedidoDTO = crearPedidoDetalleDTO(pedido);
                return ResponseEntity.ok(pedidoDTO);
            } else {
                return ResponseEntity.status(404).body(Map.of(
                        "error", "Pedido no encontrado"
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al obtener detalle del pedido: " + e.getMessage()
            ));
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private List<Map<String, Object>> mapearPedidosParaFrontend(List<Pedido> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            return new ArrayList<>();
        }

        return pedidos.stream().map(pedido -> {
            Map<String, Object> pedidoMap = new HashMap<>();

            pedidoMap.put("id", pedido.getId());
            pedidoMap.put("numeroPedido", pedido.getNumeroPedido());
            pedidoMap.put("total", pedido.getTotal());
            pedidoMap.put("fecha", pedido.getFecha());
            pedidoMap.put("fechaPedido", pedido.getFecha());
            pedidoMap.put("estado", pedido.getEstado());
            pedidoMap.put("tipoEntrega", pedido.getTipoEntrega());
            pedidoMap.put("direccionEntrega", pedido.getDireccionEntrega());
            pedidoMap.put("referenciaDireccion", pedido.getInstrucciones());
            pedidoMap.put("observaciones", pedido.getObservaciones());
            pedidoMap.put("metodoPago", pedido.getMetodoPago());

            if (pedido.getUsuario() != null) {
                Usuario usuario = pedido.getUsuario();
                Map<String, Object> clienteMap = new HashMap<>();

                clienteMap.put("id", usuario.getId());
                clienteMap.put("nombres", usuario.getNombres() != null ? usuario.getNombres() : "");
                clienteMap.put("apellidos", usuario.getApellidos() != null ? usuario.getApellidos() : "");
                clienteMap.put("telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "No disponible");
                // ✅ CORREGIDO: usar getUsername() que es el correo
                clienteMap.put("email", usuario.getUsername() != null ? usuario.getUsername() : "");

                pedidoMap.put("cliente", clienteMap);
            } else {
                Map<String, Object> clienteMap = new HashMap<>();
                clienteMap.put("id", null);
                clienteMap.put("nombres", "Cliente");
                clienteMap.put("apellidos", "No especificado");
                clienteMap.put("telefono", "No disponible");
                clienteMap.put("email", "");
                pedidoMap.put("cliente", clienteMap);
            }

            pedidoMap.put("items", obtenerItemsDelPedido(pedido));

            return pedidoMap;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> obtenerItemsDelPedido(Pedido pedido) {
        List<Map<String, Object>> items = new ArrayList<>();

        if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            for (ItemPedido item : pedido.getItems()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("nombreProducto", item.getNombreProductoSeguro() != null ?
                        item.getNombreProductoSeguro() :
                        (item.getNombreProducto() != null ? item.getNombreProducto() : "Producto"));
                itemMap.put("cantidad", item.getCantidad());
                itemMap.put("precio", item.getPrecio());
                itemMap.put("subtotal", item.getSubtotal());
                items.add(itemMap);
            }
        }

        return items;
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

        if (pedido.getUsuario() != null) {
            Usuario usuario = pedido.getUsuario();
            Map<String, String> usuarioDTO = new HashMap<>();
            usuarioDTO.put("nombres", usuario.getNombres() != null ? usuario.getNombres() : "");
            usuarioDTO.put("apellidos", usuario.getApellidos() != null ? usuario.getApellidos() : "");
            usuarioDTO.put("telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "");
            // ✅ CORREGIDO: usar getUsername()
            usuarioDTO.put("email", usuario.getUsername() != null ? usuario.getUsername() : "");
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