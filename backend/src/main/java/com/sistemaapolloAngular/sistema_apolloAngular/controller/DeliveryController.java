package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/delivery")
public class DeliveryController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;


    @PostMapping("/iniciar-entrega/{pedidoId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> iniciarEntrega(@PathVariable Long pedidoId,
                                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("status", "ERROR");
                response.put("message", "No autenticado");
                return ResponseEntity.status(401).body(response);
            }

            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();

                if (!"LISTO".equals(pedido.getEstado())) {
                    response.put("status", "ERROR");
                    response.put("message", "El pedido no está en estado LISTO. Estado actual: " + pedido.getEstado());
                    return ResponseEntity.badRequest().body(response);
                }

                pedido.setEstado("EN_CAMINO");
                pedidoRepository.save(pedido);

                response.put("status", "SUCCESS");
                response.put("message", "Entrega iniciada correctamente");
                response.put("numeroPedido", pedido.getNumeroPedido());

                return ResponseEntity.ok(response);
            } else {
                response.put("status", "ERROR");
                response.put("message", "Pedido no encontrado");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


    @PostMapping("/marcar-entregado/{pedidoId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcarComoEntregado(@PathVariable Long pedidoId,
                                                                   Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("status", "ERROR");
                response.put("message", "No autenticado");
                return ResponseEntity.status(401).body(response);
            }

            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();

                if (!"EN_CAMINO".equals(pedido.getEstado())) {
                    response.put("status", "ERROR");
                    response.put("message", "El pedido no está en estado EN_CAMINO. Estado actual: " + pedido.getEstado());
                    return ResponseEntity.badRequest().body(response);
                }

                pedido.setEstado("ENTREGADO");
                pedidoRepository.save(pedido);

                response.put("status", "SUCCESS");
                response.put("message", "Pedido marcado como ENTREGADO correctamente");
                response.put("numeroPedido", pedido.getNumeroPedido());

                return ResponseEntity.ok(response);
            } else {
                response.put("status", "ERROR");
                response.put("message", "Pedido no encontrado");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


    @GetMapping("/pedidos-para-entrega")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerPedidosParaEntrega() {
        try {
            List<Pedido> pedidos = pedidoRepository.findByEstadoWithItems("LISTO");
            List<Map<String, Object>> pedidosDTO = mapearPedidosParaFrontend(pedidos);
            return ResponseEntity.ok(pedidosDTO);
        } catch (Exception e) {
            List<Pedido> pedidos = pedidoRepository.findByEstadoOrderByFechaAsc("LISTO");
            List<Map<String, Object>> pedidosDTO = mapearPedidosParaFrontend(pedidos);
            return ResponseEntity.ok(pedidosDTO);
        }
    }


    @GetMapping("/pedidos-en-camino")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerPedidosEnCamino() {
        try {
            List<Pedido> pedidos = pedidoRepository.findByEstadoWithItems("EN_CAMINO");
            List<Map<String, Object>> pedidosDTO = mapearPedidosParaFrontend(pedidos);
            return ResponseEntity.ok(pedidosDTO);
        } catch (Exception e) {
            List<Pedido> pedidos = pedidoRepository.findByEstadoOrderByFechaAsc("EN_CAMINO");
            List<Map<String, Object>> pedidosDTO = mapearPedidosParaFrontend(pedidos);
            return ResponseEntity.ok(pedidosDTO);
        }
    }


    @GetMapping("/pedidos-entregados-hoy")
    @ResponseBody
    public ResponseEntity<List<Pedido>> obtenerPedidosEntregadosHoy() {
        List<Pedido> pedidos = pedidoRepository.findAll().stream()
                .filter(p -> "ENTREGADO".equals(p.getEstado()) &&
                        p.getFecha() != null &&
                        p.getFecha().toLocalDate().equals(java.time.LocalDate.now()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(pedidos);
    }


    @GetMapping("/metricas-delivery")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerMetricasDelivery() {
        Map<String, Object> metricas = new HashMap<>();

        try {
            List<Pedido> pedidosParaEntregar = pedidoRepository.findByEstadoOrderByFechaAsc("LISTO");
            List<Pedido> pedidosEnCamino = pedidoRepository.findByEstadoOrderByFechaAsc("EN_CAMINO");
            List<Pedido> pedidosEntregadosHoy = pedidoRepository.findAll().stream()
                    .filter(p -> "ENTREGADO".equals(p.getEstado()) &&
                            p.getFecha() != null &&
                            p.getFecha().toLocalDate().equals(java.time.LocalDate.now()))
                    .collect(Collectors.toList());

            double eficiencia = calcularEficienciaDelivery(pedidosEntregadosHoy);
            double tiempoPromedio = calcularTiempoPromedioEntrega(pedidosEntregadosHoy);

            metricas.put("totalParaEntregar", pedidosParaEntregar.size());
            metricas.put("totalEnCamino", pedidosEnCamino.size());
            metricas.put("totalEntregadosHoy", pedidosEntregadosHoy.size());
            metricas.put("eficienciaDelivery", Math.round(eficiencia));
            metricas.put("tiempoPromedioEntrega", Math.round(tiempoPromedio));
            metricas.put("success", true);

        } catch (Exception e) {
            metricas.put("success", false);
            metricas.put("error", e.getMessage());
        }

        return ResponseEntity.ok(metricas);
    }


    @GetMapping("/pedido/{pedidoId}")
    @ResponseBody
    public ResponseEntity<Pedido> obtenerDetallePedidoDelivery(@PathVariable Long pedidoId) {
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
        return pedidoOpt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/ruta-optimizada")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerRutaOptimizada() {
        Map<String, Object> ruta = new HashMap<>();

        try {
            List<Pedido> pedidosParaEntregar = pedidoRepository.findByEstadoOrderByFechaAsc("LISTO");
            List<Pedido> pedidosEnCamino = pedidoRepository.findByEstadoOrderByFechaAsc("EN_CAMINO");

            ruta.put("pedidosParaEntregar", pedidosParaEntregar);
            ruta.put("pedidosEnCamino", pedidosEnCamino);
            ruta.put("totalParadas", pedidosParaEntregar.size() + pedidosEnCamino.size());
            ruta.put("success", true);

        } catch (Exception e) {
            ruta.put("success", false);
            ruta.put("error", e.getMessage());
        }

        return ResponseEntity.ok(ruta);
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private List<Map<String, Object>> mapearPedidosParaFrontend(List<Pedido> pedidos) {
        return pedidos.stream().map(pedido -> {
            Map<String, Object> pedidoMap = new HashMap<>();

            pedidoMap.put("id", pedido.getId());
            pedidoMap.put("numeroPedido", pedido.getNumeroPedido());
            pedidoMap.put("total", pedido.getTotal());
            pedidoMap.put("fecha", pedido.getFecha());
            pedidoMap.put("tipoEntrega", pedido.getTipoEntrega());
            pedidoMap.put("direccionEntrega", pedido.getDireccionEntrega());
            pedidoMap.put("referenciaDireccion", pedido.getInstrucciones());
            pedidoMap.put("observaciones", pedido.getObservaciones());

            if (pedido.getUsuario() != null) {
                Map<String, Object> clienteMap = new HashMap<>();
                clienteMap.put("nombres", pedido.getUsuario().getNombres());
                clienteMap.put("apellidos", pedido.getUsuario().getApellidos());
                clienteMap.put("telefono", pedido.getUsuario().getTelefono());
                pedidoMap.put("cliente", clienteMap);
            } else {
                Map<String, Object> clienteMap = new HashMap<>();
                clienteMap.put("nombres", "Cliente");
                clienteMap.put("apellidos", "No especificado");
                clienteMap.put("telefono", "No disponible");
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
                itemMap.put("nombreProducto", item.getNombreProductoSeguro());
                itemMap.put("cantidad", item.getCantidad());
                itemMap.put("precio", item.getPrecio());
                itemMap.put("subtotal", item.getSubtotal());
                items.add(itemMap);
            }
        } else {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("nombreProducto", "Items no disponibles");
            itemMap.put("cantidad", 0);
            itemMap.put("precio", 0.0);
            itemMap.put("subtotal", 0.0);
            items.add(itemMap);
        }

        return items;
    }

    private double calcularEficienciaDelivery(List<Pedido> pedidosEntregados) {
        if (pedidosEntregados.isEmpty()) return 0.0;

        long entregadosATiempo = pedidosEntregados.stream()
                .filter(this::fueEntregadoATiempo)
                .count();

        return (double) entregadosATiempo / pedidosEntregados.size() * 100;
    }

    private boolean fueEntregadoATiempo(Pedido pedido) {
        if (pedido.getFecha() == null) return false;
        LocalDateTime fechaPedido = pedido.getFecha();
        LocalDateTime fechaMaximaEntrega = fechaPedido.plusMinutes(45);
        return LocalDateTime.now().isBefore(fechaMaximaEntrega);
    }

    private double calcularTiempoPromedioEntrega(List<Pedido> pedidosEntregados) {
        if (pedidosEntregados.isEmpty()) return 0.0;

        double totalMinutos = 0;
        int contador = 0;

        for (Pedido pedido : pedidosEntregados) {
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