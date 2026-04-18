package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/delivery")
public class DeliveryController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;


    @GetMapping("")
    public String vistaDelivery(Authentication authentication, Model model) {
        try {
            // Obtener información del usuario delivery
            String username = authentication.getName();
            Optional<Usuario> deliveryOpt = usuarioRepository.findByUsername(username);

            if (deliveryOpt.isPresent()) {
                Usuario delivery = deliveryOpt.get();
                model.addAttribute("usuario", delivery);
            }

            // Obtener métricas para el dashboard
            cargarMetricasDelivery(model);

            return "delivery";

        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la vista del delivery: " + e.getMessage());
            return "delivery";
        }
    }


    private void cargarMetricasDelivery(Model model) {
        try {
            // Pedidos LISTOS (para entregar)
            List<Pedido> pedidosParaEntregar = pedidoRepository.findByEstadoOrderByFechaAsc("LISTO");

            // Pedidos EN_CAMINO (en reparto)
            List<Pedido> pedidosEnCamino = pedidoRepository.findByEstadoOrderByFechaAsc("EN_CAMINO");

            // Pedidos ENTREGADOS hoy
            List<Pedido> pedidosEntregadosHoy = pedidoRepository.findAll().stream()
                    .filter(p -> "ENTREGADO".equals(p.getEstado()) &&
                            p.getFecha() != null &&
                            p.getFecha().toLocalDate().equals(java.time.LocalDate.now()))
                    .collect(Collectors.toList());

            // Calcular métricas de eficiencia
            double eficienciaDelivery = calcularEficienciaDelivery(pedidosEntregadosHoy);
            double tiempoPromedioEntrega = calcularTiempoPromedioEntrega(pedidosEntregadosHoy);

            model.addAttribute("pedidosParaEntregar", pedidosParaEntregar);
            model.addAttribute("pedidosEnCamino", pedidosEnCamino);
            model.addAttribute("pedidosEntregados", pedidosEntregadosHoy);
            model.addAttribute("totalParaEntregar", pedidosParaEntregar.size());
            model.addAttribute("totalEnCamino", pedidosEnCamino.size());
            model.addAttribute("totalEntregadosHoy", pedidosEntregadosHoy.size());
            model.addAttribute("eficienciaDelivery", eficienciaDelivery);
            model.addAttribute("tiempoPromedioEntrega", tiempoPromedioEntrega);

        } catch (Exception e) {
            model.addAttribute("errorMetricas", "Error al cargar métricas: " + e.getMessage());
        }
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

        // Suponemos que el tiempo máximo de entrega es 45 minutos
        LocalDateTime fechaPedido = pedido.getFecha();
        LocalDateTime fechaMaximaEntrega = fechaPedido.plusMinutes(45);


        return LocalDateTime.now().isBefore(fechaMaximaEntrega);
    }


    private double calcularTiempoPromedioEntrega(List<Pedido> pedidosEntregados) {
        if (pedidosEntregados.isEmpty()) return 0.0;

        double totalMinutos = 0;
        int contador = 0;

        for (Pedido pedido : pedidosEntregados) {
            // Buscar cuando cambió a EN_CAMINO y a ENTREGADO (necesitarías historial)
            // Por ahora, usamos la fecha del pedido como aproximación
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


    @PostMapping("/iniciar-entrega/{pedidoId}")
    @ResponseBody
    public String iniciarEntrega(@PathVariable Long pedidoId, Authentication authentication) {
        try {
            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();

                // Validar que el pedido esté en estado LISTO
                if (!"LISTO".equals(pedido.getEstado())) {
                    return "ERROR: El pedido no está en estado LISTO";
                }

                // Cambiar estado a EN_CAMINO
                pedido.setEstado("EN_CAMINO");
                pedidoRepository.save(pedido);

                return "SUCCESS: Entrega iniciada";
            } else {
                return "ERROR: Pedido no encontrado";
            }

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }


    @PostMapping("/marcar-entregado/{pedidoId}")
    @ResponseBody
    public String marcarComoEntregado(@PathVariable Long pedidoId, Authentication authentication) {
        try {
            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();

                // Validar que el pedido esté en estado EN_CAMINO
                if (!"EN_CAMINO".equals(pedido.getEstado())) {
                    return "ERROR: El pedido no está en estado EN_CAMINO";
                }

                // Cambiar estado a ENTREGADO
                pedido.setEstado("ENTREGADO");
                pedidoRepository.save(pedido);

                return "SUCCESS: Pedido marcado como ENTREGADO";
            } else {
                return "ERROR: Pedido no encontrado";
            }

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }


    @GetMapping("/pedidos-para-entrega")
    @ResponseBody
    public List<Map<String, Object>> obtenerPedidosParaEntrega() {
        try {
            // Usa el método que carga los items
            List<Pedido> pedidos = pedidoRepository.findByEstadoWithItems("LISTO");
            return mapearPedidosParaFrontend(pedidos);
        } catch (Exception e) {
            // Fallback si el método nuevo no existe
            List<Pedido> pedidos = pedidoRepository.findByEstadoOrderByFechaAsc("LISTO");
            return mapearPedidosParaFrontend(pedidos);
        }
    }


    @GetMapping("/pedidos-en-camino")
    @ResponseBody
    public List<Map<String, Object>> obtenerPedidosEnCamino() {
        try {
            List<Pedido> pedidos = pedidoRepository.findByEstadoWithItems("EN_CAMINO");
            return mapearPedidosParaFrontend(pedidos);
        } catch (Exception e) {
            // Fallback si el método nuevo no existe
            List<Pedido> pedidos = pedidoRepository.findByEstadoOrderByFechaAsc("EN_CAMINO");
            return mapearPedidosParaFrontend(pedidos);
        }
    }


    private List<Map<String, Object>> mapearPedidosParaFrontend(List<Pedido> pedidos) {
        return pedidos.stream().map(pedido -> {
            Map<String, Object> pedidoMap = new HashMap<>();

            // Información básica del pedido
            pedidoMap.put("id", pedido.getId());
            pedidoMap.put("numeroPedido", pedido.getNumeroPedido());
            pedidoMap.put("total", pedido.getTotal());
            pedidoMap.put("fecha", pedido.getFecha());
            pedidoMap.put("tipoEntrega", pedido.getTipoEntrega());
            pedidoMap.put("direccionEntrega", pedido.getDireccionEntrega());
            pedidoMap.put("referenciaDireccion", pedido.getInstrucciones()); // Usando instrucciones como referencia
            pedidoMap.put("observaciones", pedido.getObservaciones());

            // Información del cliente (desde usuario)
            if (pedido.getUsuario() != null) {
                Map<String, Object> clienteMap = new HashMap<>();
                clienteMap.put("nombres", pedido.getUsuario().getNombres());
                clienteMap.put("apellidos", pedido.getUsuario().getApellidos());
                clienteMap.put("telefono", pedido.getUsuario().getTelefono());
                pedidoMap.put("cliente", clienteMap);
            } else {
                // Fallback si no hay usuario
                Map<String, Object> clienteMap = new HashMap<>();
                clienteMap.put("nombres", "Cliente");
                clienteMap.put("apellidos", "No especificado");
                clienteMap.put("telefono", "No disponible");
                pedidoMap.put("cliente", clienteMap);
            }

            // Items del pedido
            pedidoMap.put("items", obtenerItemsDelPedido(pedido));

            return pedidoMap;
        }).collect(Collectors.toList());
    }


    private List<Map<String, Object>> obtenerItemsDelPedido(Pedido pedido) {
        List<Map<String, Object>> items = new ArrayList<>();

        // Verificar si el pedido tiene items cargados
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
            // Si no hay items, agregar un mensaje
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("nombreProducto", "Items no disponibles");
            itemMap.put("cantidad", 0);
            itemMap.put("precio", 0.0);
            itemMap.put("subtotal", 0.0);
            items.add(itemMap);
        }

        return items;
    }


    @GetMapping("/pedidos-entregados-hoy")
    @ResponseBody
    public List<Pedido> obtenerPedidosEntregadosHoy() {
        return pedidoRepository.findAll().stream()
                .filter(p -> "ENTREGADO".equals(p.getEstado()) &&
                        p.getFecha() != null &&
                        p.getFecha().toLocalDate().equals(java.time.LocalDate.now()))
                .collect(Collectors.toList());
    }


    @GetMapping("/metricas-delivery")
    @ResponseBody
    public java.util.Map<String, Object> obtenerMetricasDelivery() {
        java.util.Map<String, Object> metricas = new java.util.HashMap<>();

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

        return metricas;
    }


    @GetMapping("/pedido/{pedidoId}")
    @ResponseBody
    public Pedido obtenerDetallePedidoDelivery(@PathVariable Long pedidoId) {
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
        return pedidoOpt.orElse(null);
    }


    @GetMapping("/ruta-optimizada")
    @ResponseBody
    public java.util.Map<String, Object> obtenerRutaOptimizada() {
        java.util.Map<String, Object> ruta = new java.util.HashMap<>();

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

        return ruta;
    }
}
