package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cocinero")
public class CocineroController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;


    @PostMapping("/iniciar-preparacion/{pedidoId}")
    @ResponseBody
    public ResponseEntity<?> iniciarPreparacion(@PathVariable String pedidoId,
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

            Long pedidoIdLong;
            try {
                pedidoIdLong = Long.parseLong(pedidoId);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "ID de pedido inválido"
                ));
            }

            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoIdLong);
            if (!pedidoOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "Pedido no encontrado"
                ));
            }

            Pedido pedido = pedidoOpt.get();

            if (!"PAGADO".equals(pedido.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "El pedido no está en estado PAGADO. Estado actual: " + pedido.getEstado()
                ));
            }

            String nombreCocinero = obtenerNombreUsuario(authentication.getName());

            pedido.setEstado("PREPARACION");
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


    @PostMapping("/marcar-listo/{pedidoId}")
    @ResponseBody
    public ResponseEntity<?> marcarComoListo(@PathVariable String pedidoId,
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

            Long pedidoIdLong;
            try {
                pedidoIdLong = Long.parseLong(pedidoId);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "ID de pedido inválido"
                ));
            }

            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoIdLong);
            if (!pedidoOpt.isPresent()) {
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

            String nombreCocinero = obtenerNombreUsuario(authentication.getName());

            pedido.setEstado("LISTO");
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


    @GetMapping("/pedidos-por-preparar")
    @ResponseBody
    public ResponseEntity<?> obtenerPedidosPorPreparar() {
        try {
            List<Pedido> pedidos = pedidoRepository.findByEstadoOrderByFechaAsc("PAGADO");
            List<Map<String, Object>> pedidosDTO = crearPedidosDTO(pedidos);
            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al cargar pedidos por preparar: " + e.getMessage()
            ));
        }
    }


    @GetMapping("/pedidos-en-preparacion")
    @ResponseBody
    public ResponseEntity<?> obtenerPedidosEnPreparacion() {
        try {
            List<Pedido> pedidos = pedidoRepository.findByEstadoOrderByFechaAsc("PREPARACION");
            List<Map<String, Object>> pedidosDTO = crearPedidosDTO(pedidos);
            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al cargar pedidos en preparación: " + e.getMessage()
            ));
        }
    }


    @GetMapping("/pedidos-listos-hoy")
    @ResponseBody
    public ResponseEntity<?> obtenerPedidosListosHoy() {
        try {
            LocalDate hoy = LocalDate.now();
            List<Pedido> pedidos = pedidoRepository.findAll().stream()
                    .filter(p -> "LISTO".equals(p.getEstado()) &&
                            p.getFecha() != null &&
                            p.getFecha().toLocalDate().equals(hoy))
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


    @GetMapping("/metricas-cocina")
    @ResponseBody
    public Map<String, Object> obtenerMetricasCocina() {
        Map<String, Object> metricas = new HashMap<>();

        try {
            List<Pedido> pedidosPorPreparar = pedidoRepository.findByEstadoOrderByFechaAsc("PAGADO");
            List<Pedido> pedidosEnPreparacion = pedidoRepository.findByEstadoOrderByFechaAsc("PREPARACION");

            LocalDate hoy = LocalDate.now();
            List<Pedido> pedidosListosHoy = pedidoRepository.findAll().stream()
                    .filter(p -> "LISTO".equals(p.getEstado()) &&
                            p.getFecha() != null &&
                            p.getFecha().toLocalDate().equals(hoy))
                    .collect(Collectors.toList());

            double tiempoPromedio = calcularTiempoPromedioPreparacion(pedidosListosHoy);

            metricas.put("totalPorPreparar", pedidosPorPreparar.size());
            metricas.put("totalEnPreparacion", pedidosEnPreparacion.size());
            metricas.put("totalListosHoy", pedidosListosHoy.size());
            metricas.put("tiempoPromedio", Math.round(tiempoPromedio));
            metricas.put("success", true);

        } catch (Exception e) {
            metricas.put("success", false);
            metricas.put("error", e.getMessage());
        }

        return metricas;
    }


    @GetMapping("/pedido/{pedidoId}")
    @ResponseBody
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
            pedidoDTO.put("estado", pedido.getEstado());
            pedidoDTO.put("metodoPago", pedido.getMetodoPago());
            pedidoDTO.put("tipoEntrega", pedido.getTipoEntrega());
            pedidoDTO.put("observaciones", pedido.getObservaciones());

            if (pedido.getUsuario() != null) {
                Map<String, String> usuarioDTO = new HashMap<>();
                usuarioDTO.put("nombres", pedido.getUsuario().getNombres());
                usuarioDTO.put("apellidos", pedido.getUsuario().getApellidos());
                usuarioDTO.put("telefono", pedido.getUsuario().getTelefono());
                pedidoDTO.put("cliente", usuarioDTO);
            } else {
                pedidoDTO.put("cliente", null);
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

        if (pedido.getUsuario() != null) {
            Map<String, String> usuarioDTO = new HashMap<>();
            usuarioDTO.put("nombres", pedido.getUsuario().getNombres());
            usuarioDTO.put("apellidos", pedido.getUsuario().getApellidos());
            usuarioDTO.put("telefono", pedido.getUsuario().getTelefono());
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

    private String obtenerNombreUsuario(String username) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (usuario.getNombres() != null && usuario.getApellidos() != null) {
                return usuario.getNombres() + " " + usuario.getApellidos();
            } else if (usuario.getNombres() != null) {
                return usuario.getNombres();
            }
        }
        return username;
    }

    private double calcularTiempoPromedioPreparacion(List<Pedido> pedidosListos) {
        if (pedidosListos.isEmpty()) return 0.0;

        double totalMinutos = 0;
        int contador = 0;

        for (Pedido pedido : pedidosListos) {
            if (pedido.getFecha() != null) {
                LocalDateTime fechaPedido = pedido.getFecha();
                LocalDateTime ahora = LocalDateTime.now();

                long minutos = java.time.Duration.between(fechaPedido, ahora).toMinutes();
                if (minutos > 0) {
                    totalMinutos += minutos;
                    contador++;
                }
            }
        }

        return contador > 0 ? totalMinutos / contador : 0.0;
    }
}