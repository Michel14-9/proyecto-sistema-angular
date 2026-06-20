package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Obtener reporte de ventas por rango de fechas
     */
    @GetMapping("/ventas")
    public ResponseEntity<Map<String, Object>> obtenerReporteVentas(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("Generando reporte de ventas - Fechas: " + fechaInicio + " a " + fechaFin);

            LocalDateTime[] rangoFechas = obtenerRangoFechas(fechaInicio, fechaFin);
            LocalDateTime inicio = rangoFechas[0];
            LocalDateTime fin = rangoFechas[1];

            List<Pedido> todosPedidos = pedidoRepository.findAllWithItemsAndProducts();
            System.out.println(" Total de pedidos encontrados: " + todosPedidos.size());

            List<Pedido> pedidosFiltrados = todosPedidos.stream()
                    .filter(p -> p.getFecha() != null &&
                            !p.getFecha().isBefore(inicio) &&
                            !p.getFecha().isAfter(fin))
                    .collect(Collectors.toList());

            System.out.println(" Pedidos filtrados: " + pedidosFiltrados.size());

            double totalVentas = pedidosFiltrados.stream()
                    .filter(p -> "ENTREGADO".equals(p.getEstado()))
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            long totalPedidos = pedidosFiltrados.size();

            long productosVendidos = pedidosFiltrados.stream()
                    .flatMap(p -> p.getItems().stream())
                    .mapToLong(item -> item.getCantidad() != null ? item.getCantidad().longValue() : 0L)
                    .sum();

            double crecimiento = calcularCrecimientoVentas(inicio, fin);

            Map<String, Double> ventasPorDia = calcularVentasPorDia(pedidosFiltrados, inicio, fin);
            Map<String, Double> ventasPorCategoria = calcularVentasPorCategoria(pedidosFiltrados);
            List<Map<String, Object>> tablaDatos = generarTablaVentas(pedidosFiltrados);

            response.put("success", true);
            response.put("metricas", Map.of(
                    "totalVentas", totalVentas,
                    "totalPedidos", totalPedidos,
                    "productosVendidos", productosVendidos,
                    "crecimiento", Math.round(crecimiento * 100.0) / 100.0
            ));
            response.put("datosGrafico", Map.of(
                    "labels", new ArrayList<>(ventasPorDia.keySet()),
                    "datos", new ArrayList<>(ventasPorDia.values())
            ));
            response.put("datosCategoria", Map.of(
                    "labels", new ArrayList<>(ventasPorCategoria.keySet()),
                    "datos", new ArrayList<>(ventasPorCategoria.values())
            ));
            response.put("tablaDatos", tablaDatos);

            System.out.println(" Reporte de ventas generado exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println(" Error en reporte de ventas: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al generar reporte: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Obtener reporte de productos más vendidos
     */
    @GetMapping("/productos")
    public ResponseEntity<Map<String, Object>> obtenerReporteProductos(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println(" Generando reporte de productos - Fechas: " + fechaInicio + " a " + fechaFin);

            LocalDateTime[] rangoFechas = obtenerRangoFechas(fechaInicio, fechaFin);
            LocalDateTime inicio = rangoFechas[0];
            LocalDateTime fin = rangoFechas[1];

            List<Pedido> todosPedidos = pedidoRepository.findAllWithItemsAndProducts();
            List<Pedido> pedidosFiltrados = todosPedidos.stream()
                    .filter(p -> p.getFecha() != null &&
                            !p.getFecha().isBefore(inicio) &&
                            !p.getFecha().isAfter(fin))
                    .collect(Collectors.toList());

            Map<String, ProductoVendido> productosMap = new HashMap<>();

            for (Pedido pedido : pedidosFiltrados) {
                for (ItemPedido item : pedido.getItems()) {
                    String nombreProducto = item.getNombreProductoSeguro();
                    String categoria = "General";
                    if (item.getProductoFinal() != null && item.getProductoFinal().getTipo() != null) {
                        categoria = item.getProductoFinal().getTipo();
                    } else {
                        categoria = inferirCategoriaDeNombre(nombreProducto);
                    }

                    ProductoVendido producto = productosMap.getOrDefault(nombreProducto,
                            new ProductoVendido(nombreProducto, categoria));
                    producto.agregarVenta(item.getCantidad(), item.getSubtotal());
                    productosMap.put(nombreProducto, producto);
                }
            }

            List<ProductoVendido> productosVendidos = new ArrayList<>(productosMap.values());
            productosVendidos.sort((a, b) -> Long.compare(b.getCantidad(), a.getCantidad()));

            double totalVentas = productosVendidos.stream()
                    .mapToDouble(ProductoVendido::getIngresos)
                    .sum();

            long totalPedidos = pedidosFiltrados.size();
            long totalProductosVendidos = productosVendidos.stream()
                    .mapToLong(ProductoVendido::getCantidad)
                    .sum();

            double crecimiento = calcularCrecimientoProductos(inicio, fin);

            List<Map<String, Object>> tablaDatos = new ArrayList<>();
            for (ProductoVendido p : productosVendidos.stream().limit(50).collect(Collectors.toList())) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("producto", p.getNombre());
                fila.put("categoria", p.getCategoria());
                fila.put("vendidos", p.getCantidad());
                fila.put("ingresos", Math.round(p.getIngresos() * 100.0) / 100.0);
                tablaDatos.add(fila);
            }

            Map<String, Double> topProductos = new LinkedHashMap<>();
            for (ProductoVendido p : productosVendidos.stream().limit(8).collect(Collectors.toList())) {
                topProductos.put(p.getNombre(), Math.round(p.getIngresos() * 100.0) / 100.0);
            }

            response.put("success", true);
            response.put("metricas", Map.of(
                    "totalVentas", Math.round(totalVentas * 100.0) / 100.0,
                    "totalPedidos", totalPedidos,
                    "productosVendidos", totalProductosVendidos,
                    "crecimiento", Math.round(crecimiento * 100.0) / 100.0
            ));
            response.put("datosGrafico", Map.of(
                    "labels", new ArrayList<>(topProductos.keySet()),
                    "datos", new ArrayList<>(topProductos.values())
            ));
            response.put("tablaDatos", tablaDatos);

            System.out.println(" Reporte de productos generado exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error en reporte de productos: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al generar reporte: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Obtener reporte de actividad de usuarios
     */
    @GetMapping("/usuarios")
    public ResponseEntity<Map<String, Object>> obtenerReporteUsuarios(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("👥 Generando reporte de usuarios - Fechas: " + fechaInicio + " a " + fechaFin);

            LocalDateTime[] rangoFechas = obtenerRangoFechas(fechaInicio, fechaFin);
            LocalDateTime inicio = rangoFechas[0];
            LocalDateTime fin = rangoFechas[1];

            List<Pedido> todosPedidos = pedidoRepository.findAllWithItemsAndProducts();
            List<Usuario> todosUsuarios = usuarioRepository.findAll();

            List<Pedido> pedidosFiltrados = todosPedidos.stream()
                    .filter(p -> p.getFecha() != null &&
                            !p.getFecha().isBefore(inicio) &&
                            !p.getFecha().isAfter(fin))
                    .collect(Collectors.toList());

            Map<Long, ActividadUsuario> actividadMap = new HashMap<>();

            for (Pedido pedido : pedidosFiltrados) {
                if (pedido.getUsuario() != null) {
                    Long usuarioId = pedido.getUsuario().getId();
                    ActividadUsuario actividad = actividadMap.getOrDefault(usuarioId,
                            new ActividadUsuario(pedido.getUsuario()));
                    actividad.agregarPedido(pedido.getTotal());
                    actividadMap.put(usuarioId, actividad);
                }
            }

            List<ActividadUsuario> actividadUsuarios = new ArrayList<>(actividadMap.values());
            actividadUsuarios.sort((a, b) -> Long.compare(b.getTotalPedidos(), a.getTotalPedidos()));

            long totalUsuariosActivos = actividadUsuarios.size();
            long totalPedidos = pedidosFiltrados.size();
            double totalVentas = pedidosFiltrados.stream()
                    .filter(p -> "ENTREGADO".equals(p.getEstado()))
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            List<Map<String, Object>> tablaDatos = new ArrayList<>();
            for (ActividadUsuario a : actividadUsuarios) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("usuario", a.getNombreCompleto());
                fila.put("rol", a.getRol());
                fila.put("pedidos", a.getTotalPedidos());
                fila.put("totalGastado", Math.round(a.getTotalGastado() * 100.0) / 100.0);
                tablaDatos.add(fila);
            }

            Map<String, Long> actividadPorRol = new HashMap<>();
            for (ActividadUsuario a : actividadUsuarios) {
                String rol = a.getRol();
                actividadPorRol.put(rol, actividadPorRol.getOrDefault(rol, 0L) + a.getTotalPedidos());
            }

            response.put("success", true);
            response.put("metricas", Map.of(
                    "totalVentas", Math.round(totalVentas * 100.0) / 100.0,
                    "totalPedidos", totalPedidos,
                    "usuariosActivos", totalUsuariosActivos,
                    "crecimiento", 0.0
            ));
            response.put("datosGrafico", Map.of(
                    "labels", new ArrayList<>(actividadPorRol.keySet()),
                    "datos", new ArrayList<>(actividadPorRol.values())
            ));
            response.put("tablaDatos", tablaDatos);

            System.out.println(" Reporte de usuarios generado exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println(" Error en reporte de usuarios: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al generar reporte: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ================== MÉTODOS AUXILIARES ==================

    private LocalDateTime[] obtenerRangoFechas(String fechaInicio, String fechaFin) {
        LocalDateTime inicio;
        LocalDateTime fin;

        if (fechaInicio != null && fechaFin != null && !fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
            inicio = LocalDate.parse(fechaInicio).atStartOfDay();
            fin = LocalDate.parse(fechaFin).atTime(23, 59, 59);
        } else {
            fin = LocalDateTime.now();
            inicio = fin.minusMonths(1);
        }

        System.out.println(" Rango de fechas: " + inicio + " a " + fin);
        return new LocalDateTime[]{inicio, fin};
    }

    private double calcularCrecimientoVentas(LocalDateTime inicio, LocalDateTime fin) {
        try {
            List<Pedido> pedidosActual = pedidoRepository.findAll().stream()
                    .filter(p -> p.getFecha() != null &&
                            !p.getFecha().isBefore(inicio) &&
                            !p.getFecha().isAfter(fin) &&
                            "ENTREGADO".equals(p.getEstado()))
                    .collect(Collectors.toList());

            double ventasActual = pedidosActual.stream()
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            long dias = java.time.Duration.between(inicio, fin).toDays();
            LocalDateTime inicioAnterior = inicio.minusDays(dias + 1);
            LocalDateTime finAnterior = inicio.minusSeconds(1);

            List<Pedido> pedidosAnterior = pedidoRepository.findAll().stream()
                    .filter(p -> p.getFecha() != null &&
                            !p.getFecha().isBefore(inicioAnterior) &&
                            !p.getFecha().isAfter(finAnterior) &&
                            "ENTREGADO".equals(p.getEstado()))
                    .collect(Collectors.toList());

            double ventasAnterior = pedidosAnterior.stream()
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            if (ventasAnterior == 0) return ventasActual > 0 ? 100.0 : 0.0;

            return ((ventasActual - ventasAnterior) / ventasAnterior) * 100;

        } catch (Exception e) {
            System.err.println(" Error calculando crecimiento: " + e.getMessage());
            return 0.0;
        }
    }

    private double calcularCrecimientoProductos(LocalDateTime inicio, LocalDateTime fin) {
        return calcularCrecimientoVentas(inicio, fin);
    }

    private Map<String, Double> calcularVentasPorDia(List<Pedido> pedidos, LocalDateTime inicio, LocalDateTime fin) {
        Map<String, Double> ventasPorDia = new LinkedHashMap<>();

        LocalDate fechaActual = inicio.toLocalDate();
        LocalDate fechaFin = fin.toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        while (!fechaActual.isAfter(fechaFin)) {
            LocalDateTime inicioDia = fechaActual.atStartOfDay();
            LocalDateTime finDia = fechaActual.atTime(23, 59, 59);
            String clave = fechaActual.format(formatter);

            double ventasDia = pedidos.stream()
                    .filter(p -> p.getFecha() != null &&
                            !p.getFecha().isBefore(inicioDia) &&
                            !p.getFecha().isAfter(finDia) &&
                            "ENTREGADO".equals(p.getEstado()))
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            ventasPorDia.put(clave, Math.round(ventasDia * 100.0) / 100.0);
            fechaActual = fechaActual.plusDays(1);
        }

        return ventasPorDia;
    }

    private Map<String, Double> calcularVentasPorCategoria(List<Pedido> pedidos) {
        Map<String, Double> ventasPorCategoria = new HashMap<>();

        for (Pedido pedido : pedidos) {
            if ("ENTREGADO".equals(pedido.getEstado())) {
                for (ItemPedido item : pedido.getItems()) {
                    String categoria = "General";
                    if (item.getProductoFinal() != null && item.getProductoFinal().getTipo() != null) {
                        categoria = item.getProductoFinal().getTipo();
                    } else {
                        categoria = inferirCategoriaDeNombre(item.getNombreProductoSeguro());
                    }

                    double subtotal = item.getSubtotal() != null ? item.getSubtotal() : 0.0;
                    ventasPorCategoria.put(categoria,
                            ventasPorCategoria.getOrDefault(categoria, 0.0) + subtotal);
                }
            }
        }

        // Redondear valores
        ventasPorCategoria.replaceAll((k, v) -> Math.round(v * 100.0) / 100.0);

        return ventasPorCategoria;
    }

    private String inferirCategoriaDeNombre(String nombreProducto) {
        if (nombreProducto == null) return "General";

        String nombreLower = nombreProducto.toLowerCase();

        if (nombreLower.contains("pollo") || nombreLower.contains("broaster") || nombreLower.contains("crispy")) {
            return "Pollos";
        } else if (nombreLower.contains("parrilla") || nombreLower.contains("lomo") || nombreLower.contains("asado")) {
            return "Parrillas";
        } else if (nombreLower.contains("chicharrón") || nombreLower.contains("chicharron")) {
            return "Chicharrón";
        } else if (nombreLower.contains("hamburguesa") || nombreLower.contains("burger")) {
            return "Hamburguesas";
        } else if (nombreLower.contains("combo") || nombreLower.contains("promo")) {
            return "Combos";
        } else if (nombreLower.contains("criollo") || nombreLower.contains("seco") || nombreLower.contains("arroz")) {
            return "Criollos";
        } else {
            return "General";
        }
    }

    private List<Map<String, Object>> generarTablaVentas(List<Pedido> pedidos) {
        List<Map<String, Object>> tabla = new ArrayList<>();

        for (Pedido p : pedidos.stream()
                .sorted((a, b) -> {
                    if (a.getFecha() == null) return 1;
                    if (b.getFecha() == null) return -1;
                    return b.getFecha().compareTo(a.getFecha());
                })
                .limit(100)
                .collect(Collectors.toList())) {

            String productos = p.getItems().stream()
                    .map(item -> item.getNombreProductoSeguro() + " (x" + item.getCantidad() + ")")
                    .collect(Collectors.joining(", "));

            String cliente = p.getUsuario() != null ?
                    p.getUsuario().getNombres() + " " + p.getUsuario().getApellidos() :
                    "Cliente general";

            Map<String, Object> fila = new HashMap<>();
            fila.put("id", p.getId());
            fila.put("fecha", p.getFecha() != null ?
                    p.getFecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
            fila.put("cliente", cliente);
            fila.put("productos", productos);
            fila.put("total", Math.round(p.getTotal() * 100.0) / 100.0);
            fila.put("estado", p.getEstado());

            tabla.add(fila);
        }

        return tabla;
    }

    // ================== CLASES AUXILIARES ==================

    private static class ProductoVendido {
        private String nombre;
        private String categoria;
        private long cantidad;
        private double ingresos;

        public ProductoVendido(String nombre, String categoria) {
            this.nombre = nombre;
            this.categoria = categoria;
            this.cantidad = 0;
            this.ingresos = 0;
        }

        public void agregarVenta(Integer cantidad, Double subtotal) {
            this.cantidad += cantidad != null ? cantidad : 0;
            this.ingresos += subtotal != null ? subtotal : 0.0;
        }

        public String getNombre() { return nombre; }
        public String getCategoria() { return categoria; }
        public long getCantidad() { return cantidad; }
        public double getIngresos() { return ingresos; }
    }

    private static class ActividadUsuario {
        private Usuario usuario;
        private long totalPedidos;
        private double totalGastado;

        public ActividadUsuario(Usuario usuario) {
            this.usuario = usuario;
            this.totalPedidos = 0;
            this.totalGastado = 0;
        }

        public void agregarPedido(Double total) {
            this.totalPedidos++;
            this.totalGastado += total != null ? total : 0.0;
        }

        public String getNombreCompleto() {
            return usuario.getNombres() + " " + usuario.getApellidos();
        }

        public String getRol() {
            return usuario.getRol();
        }

        public long getTotalPedidos() { return totalPedidos; }
        public double getTotalGastado() { return totalGastado; }
    }
}