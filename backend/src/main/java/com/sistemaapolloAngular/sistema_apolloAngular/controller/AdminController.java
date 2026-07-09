package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.service.ProductoFinalService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin-menu")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AdminController {

    private final ProductoFinalService productoFinalService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AdminController(ProductoFinalService productoFinalService) {
        this.productoFinalService = productoFinalService;
    }

    @GetMapping("/estadisticas-dashboard")
    @ResponseBody
    @Transactional
    public Map<String, Object> obtenerEstadisticasDashboard() {
        Map<String, Object> estadisticas = new HashMap<>();

        try {
            LocalDate hoy = LocalDate.now();
            LocalDateTime hoyInicio = hoy.atStartOfDay();
            LocalDateTime hoyFin = hoy.atTime(23, 59, 59);

            LocalDate primerDiaMes = hoy.withDayOfMonth(1);
            LocalDateTime mesInicio = primerDiaMes.atStartOfDay();
            LocalDateTime mesFin = hoy.atTime(23, 59, 59);

            List<ProductoFinal> productos = productoFinalService.obtenerTodos();
            List<Usuario> usuarios = usuarioRepository.findAll();
            List<Pedido> todosLosPedidos = pedidoRepository.findAll();

            List<Pedido> pedidosHoy = todosLosPedidos.stream()
                    .filter(pedido -> {
                        if (pedido.getFecha() == null) return false;
                        LocalDateTime fechaPedido = pedido.getFecha();
                        return !fechaPedido.isBefore(hoyInicio) && !fechaPedido.isAfter(hoyFin);
                    })
                    .collect(Collectors.toList());

            double ingresosHoy = pedidosHoy.stream()
                    .filter(p -> "ENTREGADO".equals(p.getEstado()))
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            List<Pedido> pedidosMes = todosLosPedidos.stream()
                    .filter(pedido -> {
                        if (pedido.getFecha() == null) return false;
                        LocalDateTime fechaPedido = pedido.getFecha();
                        return !fechaPedido.isBefore(mesInicio) && !fechaPedido.isAfter(mesFin);
                    })
                    .collect(Collectors.toList());

            double ventasMesTotal = pedidosMes.stream()
                    .filter(p -> "ENTREGADO".equals(p.getEstado()))
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            long totalPedidosMes = pedidosMes.size();

            double ventaMaxima = pedidosMes.stream()
                    .filter(p -> "ENTREGADO".equals(p.getEstado()))
                    .mapToDouble(Pedido::getTotal)
                    .max()
                    .orElse(0.0);

            double promedioDiario = hoy.getDayOfMonth() > 0 ? ventasMesTotal / hoy.getDayOfMonth() : 0.0;

            estadisticas.put("totalProductos", productos.size());
            estadisticas.put("totalUsuarios", usuarios.size());
            estadisticas.put("pedidosHoy", pedidosHoy.size());
            estadisticas.put("ingresosHoy", ingresosHoy);
            estadisticas.put("ventasMesTotal", ventasMesTotal);
            estadisticas.put("totalPedidos", totalPedidosMes);
            estadisticas.put("ventaMaxima", ventaMaxima);
            estadisticas.put("promedioDiario", promedioDiario);
            estadisticas.put("success", true);

        } catch (Exception e) {
            estadisticas.put("success", false);
            estadisticas.put("error", e.getMessage());
            e.printStackTrace();
        }

        return estadisticas;
    }

    @GetMapping("/ventas-recientes")
    @ResponseBody
    @Transactional
    public List<Map<String, Object>> obtenerVentasRecientes() {
        try {
            List<Pedido> pedidos = pedidoRepository.findAllWithItemsAndProducts()
                    .stream()
                    .limit(10)
                    .collect(Collectors.toList());

            List<Map<String, Object>> ventas = new ArrayList<>();

            for (Pedido pedido : pedidos) {
                Map<String, Object> venta = new HashMap<>();
                venta.put("id", pedido.getId());
                venta.put("numeroPedido", pedido.getNumeroPedido());
                venta.put("total", pedido.getTotal());
                venta.put("fecha", pedido.getFecha());
                venta.put("estado", pedido.getEstado());

                if (pedido.getUsuario() != null) {
                    Usuario usuario = pedido.getUsuario();
                    venta.put("cliente", usuario.getNombres() + " " + usuario.getApellidos());
                    venta.put("usuario", Map.of(
                            "nombres", usuario.getNombres(),
                            "apellidos", usuario.getApellidos()
                    ));
                } else {
                    venta.put("cliente", "Cliente general");
                    venta.put("usuario", null);
                }

                List<Map<String, Object>> itemsData = new ArrayList<>();
                if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
                    for (ItemPedido item : pedido.getItems()) {
                        Map<String, Object> itemData = new HashMap<>();
                        itemData.put("nombreProducto", item.getNombreProducto());
                        itemData.put("nombreProductoSeguro", item.getNombreProductoSeguro());
                        itemData.put("cantidad", item.getCantidad());
                        itemData.put("precio", item.getPrecio());
                        itemData.put("subtotal", item.getSubtotal());
                        itemsData.add(itemData);
                    }
                }
                venta.put("items", itemsData);
                ventas.add(venta);
            }

            return ventas;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @GetMapping("/estadisticas-ventas")
    @ResponseBody
    @Transactional
    public Map<String, Object> obtenerEstadisticasVentas() {
        Map<String, Object> estadisticas = new HashMap<>();

        try {
            LocalDate hoy = LocalDate.now();
            Map<String, Double> ventasUltimaSemana = new LinkedHashMap<>();
            List<Pedido> todosLosPedidos = pedidoRepository.findAll();

            for (int i = 6; i >= 0; i--) {
                LocalDate fecha = hoy.minusDays(i);
                LocalDateTime inicioDia = fecha.atStartOfDay();
                LocalDateTime finDia = fecha.atTime(23, 59, 59);

                double ventasDia = todosLosPedidos.stream()
                        .filter(pedido -> {
                            if (pedido.getFecha() == null) return false;
                            LocalDateTime fechaPedido = pedido.getFecha();
                            return !fechaPedido.isBefore(inicioDia) &&
                                    !fechaPedido.isAfter(finDia) &&
                                    "ENTREGADO".equals(pedido.getEstado());
                        })
                        .mapToDouble(Pedido::getTotal)
                        .sum();

                ventasUltimaSemana.put(
                        fecha.format(DateTimeFormatter.ofPattern("dd/MM")),
                        ventasDia
                );
            }

            estadisticas.put("ventasPorDia", ventasUltimaSemana);
            estadisticas.put("success", true);

        } catch (Exception e) {
            estadisticas.put("success", false);
            estadisticas.put("error", e.getMessage());
            e.printStackTrace();
        }

        return estadisticas;
    }

    @GetMapping("/exportar-dashboard-excel")
    @Transactional
    public void exportarDashboardExcel(HttpServletResponse response) throws IOException {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "dashboard_apollo_" + timestamp + ".xlsx";
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);

            List<ProductoFinal> productos = productoFinalService.obtenerTodos();
            Workbook workbook = new XSSFWorkbook();

            // Resumen Dashboard
            Sheet resumenSheet = workbook.createSheet("Resumen Dashboard");

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);

            CellStyle metricStyle = workbook.createCellStyle();
            metricStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            metricStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font metricFont = workbook.createFont();
            metricFont.setBold(true);
            metricStyle.setFont(metricFont);

            Row titleRow = resumenSheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DASHBOARD - SISTEMA APOLLO");
            titleCell.setCellStyle(titleStyle);

            int rowNum = 2;
            String[][] metrics = {
                    {"Total de Productos", String.valueOf(productos.size())},
                    {"Precio Promedio", "S/. " + String.format("%.2f", productos.stream().mapToDouble(ProductoFinal::getPrecio).average().orElse(0.0))},
                    {"Producto Más Caro", "S/. " + String.format("%.2f", productos.stream().mapToDouble(ProductoFinal::getPrecio).max().orElse(0.0))},
                    {"Producto Más Económico", "S/. " + String.format("%.2f", productos.stream().mapToDouble(ProductoFinal::getPrecio).min().orElse(0.0))},
                    {"Valor Total del Inventario", "S/. " + String.format("%.2f", productos.stream().mapToDouble(ProductoFinal::getPrecio).sum())},
                    {"Fecha de Generación", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}
            };

            for (String[] metric : metrics) {
                Row metricRow = resumenSheet.createRow(rowNum++);
                metricRow.createCell(0).setCellValue(metric[0]);
                Cell valueCell = metricRow.createCell(1);
                valueCell.setCellValue(metric[1]);
                valueCell.setCellStyle(metricStyle);
            }

            // Estadísticas por Categoría
            Sheet statsSheet = workbook.createSheet("Estadísticas por Categoría");
            Row statsHeader = statsSheet.createRow(0);
            String[] statsHeaders = {"Categoría", "Cantidad", "Precio Promedio", "Precio Máx", "Precio Mín", "Valor Total"};

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < statsHeaders.length; i++) {
                Cell cell = statsHeader.createCell(i);
                cell.setCellValue(statsHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            Map<String, List<ProductoFinal>> productosPorCategoria = productos.stream()
                    .collect(Collectors.groupingBy(ProductoFinal::getTipo));

            int statsRowNum = 1;
            for (Map.Entry<String, List<ProductoFinal>> entry : productosPorCategoria.entrySet()) {
                String categoria = entry.getKey();
                List<ProductoFinal> productosCategoria = entry.getValue();

                double promedio = productosCategoria.stream().mapToDouble(ProductoFinal::getPrecio).average().orElse(0.0);
                double maxPrecio = productosCategoria.stream().mapToDouble(ProductoFinal::getPrecio).max().orElse(0.0);
                double minPrecio = productosCategoria.stream().mapToDouble(ProductoFinal::getPrecio).min().orElse(0.0);
                double valorTotal = productosCategoria.stream().mapToDouble(ProductoFinal::getPrecio).sum();

                Row statsRow = statsSheet.createRow(statsRowNum++);
                statsRow.createCell(0).setCellValue(categoria);
                statsRow.createCell(1).setCellValue(productosCategoria.size());
                statsRow.createCell(2).setCellValue(promedio);
                statsRow.createCell(3).setCellValue(maxPrecio);
                statsRow.createCell(4).setCellValue(minPrecio);
                statsRow.createCell(5).setCellValue(valorTotal);
            }

            // Lista de Productos
            Sheet productosSheet = workbook.createSheet("Todos los Productos");
            Row productosHeader = productosSheet.createRow(0);
            String[] productosHeaders = {"ID", "Nombre", "Categoría", "Precio", "Descripción"};

            for (int i = 0; i < productosHeaders.length; i++) {
                Cell cell = productosHeader.createCell(i);
                cell.setCellValue(productosHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int productosRowNum = 1;
            for (ProductoFinal producto : productos) {
                Row row = productosSheet.createRow(productosRowNum++);
                row.createCell(0).setCellValue(producto.getId());
                row.createCell(1).setCellValue(producto.getNombre());
                row.createCell(2).setCellValue(producto.getTipo());
                row.createCell(3).setCellValue(producto.getPrecio());
                row.createCell(4).setCellValue(producto.getDescripcion() != null ? producto.getDescripcion() : "");
            }

            for (int i = 0; i < 6; i++) {
                resumenSheet.autoSizeColumn(i);
                statsSheet.autoSizeColumn(i);
                if (i < 5) productosSheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
            workbook.close();

            System.out.println(" Reporte Dashboard Excel generado: " + filename);

        } catch (Exception e) {
            System.err.println(" Error al generar reporte dashboard: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al generar el reporte del dashboard");
        }
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarProducto(
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam Double precio,
            @RequestParam String tipo,
            @RequestParam(required = false) String imagenUrl) {

        try {
            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "El nombre del producto es requerido"));
            }

            if (precio == null || precio <= 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "El precio debe ser mayor a 0"));
            }

            if (tipo == null || tipo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "El tipo/categoría es requerido"));
            }

            ProductoFinal producto = new ProductoFinal();
            producto.setNombre(nombre.trim());
            producto.setDescripcion(descripcion != null ? descripcion.trim() : "");
            producto.setPrecio(precio);
            producto.setTipo(tipo.trim());

            if (imagenUrl != null && !imagenUrl.trim().isEmpty()) {
                producto.setImagenUrl(imagenUrl.trim());
            } else {
                producto.setImagenUrl("/imagenes/default-product.jpg");
            }

            ProductoFinal productoCreado = productoFinalService.guardar(producto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Producto guardado exitosamente!",
                    "producto", productoCreado
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Error al guardar el producto: " + e.getMessage()));
        }
    }

    @PostMapping("/actualizar/{id}")
    @ResponseBody
    public ResponseEntity<?> actualizarProducto(
            @PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam Double precio,
            @RequestParam String tipo,
            @RequestParam(required = false) String imagenUrl) {

        try {
            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "El nombre del producto es requerido"));
            }

            if (precio == null || precio <= 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "El precio debe ser mayor a 0"));
            }

            if (tipo == null || tipo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "El tipo/categoría es requerido"));
            }

            Optional<ProductoFinal> productoOpt = productoFinalService.obtenerPorId(id);
            if (productoOpt.isPresent()) {
                ProductoFinal producto = productoOpt.get();
                producto.setNombre(nombre.trim());
                producto.setDescripcion(descripcion != null ? descripcion.trim() : "");
                producto.setPrecio(precio);
                producto.setTipo(tipo.trim());

                if (imagenUrl != null && !imagenUrl.trim().isEmpty()) {
                    producto.setImagenUrl(imagenUrl.trim());
                } else {
                    producto.setImagenUrl("/imagenes/default-product.jpg");
                }

                ProductoFinal productoActualizado = productoFinalService.guardar(producto);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Producto actualizado exitosamente!",
                        "producto", productoActualizado
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Producto no encontrado"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Error al actualizar el producto: " + e.getMessage()));
        }
    }

    @PostMapping("/eliminar/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> eliminarProducto(
            @PathVariable Long id,
            @RequestParam(required = false) String redirectSection) {

        try {
            Optional<ProductoFinal> productoOpt = productoFinalService.obtenerPorId(id);
            if (productoOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "Producto no encontrado"
                ));
            }

            ProductoFinal producto = productoOpt.get();
            String nombreProducto = producto.getNombre();

            // Verificar si tiene pedidos históricos
            boolean tieneItemsPedido = pedidoRepository.existsByItemsProductoId(id);

            if (tieneItemsPedido) {
                // Solo desactivar, conservar historial
                producto.setActivo(false);
                productoFinalService.guardar(producto);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Producto '" + nombreProducto + "' desactivado (tiene pedidos históricos).",
                        "desactivado", true
                ));
            } else {
                // Sin historial, eliminar físicamente
                productoFinalService.eliminar(id);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Producto '" + nombreProducto + "' eliminado exitosamente!",
                        "desactivado", false
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Error al eliminar el producto: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/estadisticas")
    @ResponseBody
    public Map<String, Object> obtenerEstadisticas() {
        Map<String, Object> estadisticas = new HashMap<>();

        try {
            List<ProductoFinal> productos = productoFinalService.obtenerTodos();
            List<Usuario> usuarios = usuarioRepository.findAll();

            estadisticas.put("totalProductos", productos.size());
            estadisticas.put("precioPromedio", productos.stream().mapToDouble(ProductoFinal::getPrecio).average().orElse(0.0));
            estadisticas.put("precioMaximo", productos.stream().mapToDouble(ProductoFinal::getPrecio).max().orElse(0.0));
            estadisticas.put("precioMinimo", productos.stream().mapToDouble(ProductoFinal::getPrecio).min().orElse(0.0));
            estadisticas.put("totalUsuarios", usuarios.size());

            Map<String, Long> productosPorCategoria = productos.stream()
                    .collect(Collectors.groupingBy(ProductoFinal::getTipo, Collectors.counting()));
            estadisticas.put("productosPorCategoria", productosPorCategoria);
            estadisticas.put("pedidosHoy", 0);
            estadisticas.put("ingresosHoy", 0.0);
            estadisticas.put("success", true);

        } catch (Exception e) {
            estadisticas.put("success", false);
            estadisticas.put("error", e.getMessage());
        }

        return estadisticas;
    }

    @GetMapping("/productos")
    @ResponseBody
    public List<ProductoFinal> obtenerTodosProductos() {
        return productoFinalService.obtenerTodos();
    }

    @GetMapping("/productos/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerProductoPorId(@PathVariable Long id) {
        try {
            Optional<ProductoFinal> producto = productoFinalService.obtenerPorId(id);
            if (producto.isPresent()) {
                return ResponseEntity.ok(producto.get());
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Producto no encontrado"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/usuarios")
    @ResponseBody
    public List<Usuario> obtenerTodosUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        System.out.println("👥 Enviando " + usuarios.size() + " usuarios al frontend");
        return usuarios;
    }

    @PostMapping("/usuarios/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarUsuario(
            @RequestParam String nombres,
            @RequestParam String apellidos,
            @RequestParam String tipoDocumento,
            @RequestParam String numeroDocumento,
            @RequestParam String telefono,
            @RequestParam String fechaNacimiento,
            @RequestParam String email,
            @RequestParam String rol,
            @RequestParam String password) {

        try {
            List<String> rolesValidos = Arrays.asList("admin", "cajero", "cocinero", "delivery");
            if (!rolesValidos.contains(rol.toLowerCase())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Rol no válido. Roles permitidos: admin, cajero, cocinero, delivery"
                ));
            }

            if (usuarioRepository.findByUsername(email).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El correo electrónico ya está registrado"
                ));
            }

            if (password == null || password.trim().length() < 6) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "La contraseña debe tener al menos 6 caracteres"
                ));
            }

            Usuario usuario = new Usuario();
            usuario.setNombres(nombres.trim());
            usuario.setApellidos(apellidos.trim());
            usuario.setTipoDocumento(tipoDocumento);
            usuario.setNumeroDocumento(numeroDocumento.trim());
            usuario.setTelefono(telefono.trim());
            usuario.setFechaNacimiento(LocalDate.parse(fechaNacimiento));
            usuario.setUsername(email.trim());
            usuario.setRol(rol.toUpperCase());
            usuario.setPassword(passwordEncoder.encode(password));

            Usuario usuarioCreado = usuarioRepository.save(usuario);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Usuario creado exitosamente!",
                    "usuario", usuarioCreado
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Error al guardar el usuario: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/usuarios/eliminar/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "Usuario no encontrado"
                ));
            }

            Usuario usuario = usuarioOpt.get();

            if ("admin@luren.com".equals(usuario.getUsername())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "No se puede eliminar al administrador principal"
                ));
            }

            String nombreUsuario = usuario.getNombres() + " " + usuario.getApellidos();

            // Verificar si tiene pedidos asociados
            boolean tienePedidos = pedidoRepository.existsByUsuarioId(id);

            if (tienePedidos) {
                // Solo desactivar, conservar historial de pedidos
                usuario.setActivo(false);
                usuarioRepository.save(usuario);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Usuario '" + nombreUsuario + "' desactivado (tiene pedidos históricos).",
                        "desactivado", true
                ));
            } else {
                // Sin historial, eliminar físicamente
                usuarioRepository.deleteById(id);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Usuario '" + nombreUsuario + "' eliminado exitosamente!",
                        "desactivado", false
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Error al eliminar el usuario: " + e.getMessage()
            ));
        }
    }

    // ============ REPORTES ============

    @GetMapping("/reportes/ventas")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerReporteVentas(
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {

        try {
            LocalDateTime inicio = LocalDate.parse(fechaInicio).atStartOfDay();
            LocalDateTime fin = LocalDate.parse(fechaFin).atTime(23, 59, 59);

            List<Pedido> pedidos = pedidoRepository.findAll().stream()
                    .filter(p -> p.getFecha() != null)
                    .filter(p -> !p.getFecha().isBefore(inicio) && !p.getFecha().isAfter(fin))
                    .filter(p -> "ENTREGADO".equals(p.getEstado()))
                    .collect(Collectors.toList());

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("success", true);

            // Métricas
            Map<String, Object> metricas = new HashMap<>();
            metricas.put("totalVentas", pedidos.stream().mapToDouble(Pedido::getTotal).sum());
            metricas.put("totalPedidos", pedidos.size());
            metricas.put("productosVendidos", pedidos.stream()
                    .flatMap(p -> p.getItems().stream())
                    .mapToInt(ItemPedido::getCantidad)
                    .sum());
            metricas.put("crecimiento", 0.0);
            resultado.put("metricas", metricas);

            // Datos del gráfico
            Map<String, Object> datosGrafico = new HashMap<>();
            List<String> labels = new ArrayList<>();
            List<Double> datos = new ArrayList<>();

            Map<LocalDate, Double> ventasPorDia = pedidos.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.getFecha().toLocalDate(),
                            Collectors.summingDouble(Pedido::getTotal)
                    ));

            LocalDate inicioDate = LocalDate.parse(fechaInicio);
            LocalDate finDate = LocalDate.parse(fechaFin);

            for (LocalDate date = inicioDate; !date.isAfter(finDate); date = date.plusDays(1)) {
                labels.add(date.format(DateTimeFormatter.ofPattern("dd/MM")));
                datos.add(ventasPorDia.getOrDefault(date, 0.0));
            }

            datosGrafico.put("labels", labels);
            datosGrafico.put("datos", datos);

            // Categorías para gráfico de pastel
            Map<String, Double> categorias = new HashMap<>();
            for (Pedido p : pedidos) {
                for (ItemPedido item : p.getItems()) {
                    String categoria = item.getNombreProducto() != null ?
                            item.getNombreProducto().substring(0, Math.min(10, item.getNombreProducto().length())) : "Otros";
                    categorias.merge(categoria, item.getSubtotal(), Double::sum);
                }
            }

            Map<String, Object> categoriasData = new HashMap<>();
            categoriasData.put("labels", new ArrayList<>(categorias.keySet()));
            categoriasData.put("datos", new ArrayList<>(categorias.values()));
            datosGrafico.put("categorias", categoriasData);

            resultado.put("datosGrafico", datosGrafico);

            // Tabla de datos
            List<Map<String, Object>> tablaDatos = new ArrayList<>();
            for (Pedido p : pedidos.stream().limit(50).collect(Collectors.toList())) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("id", p.getId());
                fila.put("fecha", p.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                fila.put("cliente", p.getUsuario() != null ?
                        p.getUsuario().getNombres() + " " + p.getUsuario().getApellidos() : "Cliente general");
                fila.put("productos", p.getItems().stream().map(ItemPedido::getNombreProducto).collect(Collectors.joining(", ")));
                fila.put("total", p.getTotal());
                fila.put("estado", p.getEstado());
                tablaDatos.add(fila);
            }
            resultado.put("tablaDatos", tablaDatos);
            resultado.put("columnas", List.of("id", "fecha", "cliente", "productos", "total", "estado"));

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ============ EXPORTAR PDF ============

    @GetMapping("/exportar-pdf")
    @ResponseBody
    public void exportarPDF(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) String tipo,
            HttpServletResponse response) throws IOException {
        try {
            response.setContentType("application/pdf");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "reporte_" + (tipo != null ? tipo : "ventas") + "_" + timestamp + ".pdf";
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);

            // Obtener datos para el reporte
            List<Pedido> pedidos = obtenerPedidosFiltrados(fechaInicio, fechaFin);

            // Crear PDF con iText
            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.setMargins(20, 20, 20, 20);

            // Título
            Paragraph titulo = new Paragraph("REPORTE DE " + (tipo != null ? tipo.toUpperCase() : "VENTAS"))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(18);
            document.add(titulo);
            document.add(new Paragraph(" "));

            Paragraph subtitulo = new Paragraph("Generado: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(subtitulo);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("_________________________________________"));
            document.add(new Paragraph(" "));

            // Filtros aplicados
            if (fechaInicio != null && fechaFin != null) {
                document.add(new Paragraph("Período: " + fechaInicio + " al " + fechaFin));
            }
            document.add(new Paragraph("Total de registros: " + pedidos.size()));
            document.add(new Paragraph(" "));

            // Tabla de pedidos
            float[] columnWidths = {2, 3, 2, 2, 2, 3};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginTop(10);
            table.setMarginBottom(10);

            table.addHeaderCell(new Paragraph("N° Pedido").setBold());
            table.addHeaderCell(new Paragraph("Cliente").setBold());
            table.addHeaderCell(new Paragraph("Fecha").setBold());
            table.addHeaderCell(new Paragraph("Total").setBold());
            table.addHeaderCell(new Paragraph("Estado").setBold());
            table.addHeaderCell(new Paragraph("Tipo Entrega").setBold());

            for (Pedido pedido : pedidos.stream().limit(100).collect(Collectors.toList())) {
                table.addCell(new Paragraph(pedido.getNumeroPedido() != null ? pedido.getNumeroPedido() : "N/A"));

                String cliente = "N/A";
                if (pedido.getUsuario() != null) {
                    cliente = (pedido.getUsuario().getNombres() != null ? pedido.getUsuario().getNombres() : "") + " " +
                            (pedido.getUsuario().getApellidos() != null ? pedido.getUsuario().getApellidos() : "");
                    cliente = cliente.trim().isEmpty() ? pedido.getUsuario().getUsername() : cliente;
                }
                table.addCell(new Paragraph(cliente));

                table.addCell(new Paragraph(pedido.getFecha() != null ?
                        pedido.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A"));
                table.addCell(new Paragraph("S/ " + String.format("%.2f", pedido.getTotal())));
                table.addCell(new Paragraph(pedido.getEstado() != null ? pedido.getEstado() : "N/A"));
                table.addCell(new Paragraph(pedido.getTipoEntrega() != null ? pedido.getTipoEntrega() : "N/A"));
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("_________________________________________"));
            document.add(new Paragraph(" "));

            // Totales
            double totalGeneral = pedidos.stream().mapToDouble(Pedido::getTotal).sum();
            document.add(new Paragraph("TOTAL GENERAL: S/ " + String.format("%.2f", totalGeneral)).setBold());
            document.add(new Paragraph("TOTAL PEDIDOS: " + pedidos.size()));

            document.close();
            System.out.println("📄 Reporte PDF generado: " + filename);

        } catch (Exception e) {
            System.err.println("❌ Error al generar PDF: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al generar el PDF");
        }
    }
    @GetMapping("/api/menu/combos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerCombosApi() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Obtener productos que son combos (tipo = "COMBO" o "combo")
            List<ProductoFinal> todosLosProductos = productoFinalService.obtenerTodos();

            // Filtrar solo los que tienen tipo "COMBO" (o "combo")
            List<ProductoFinal> combos = todosLosProductos.stream()
                    .filter(p -> p.getTipo() != null &&
                            (p.getTipo().equalsIgnoreCase("COMBO") ||
                                    p.getTipo().equalsIgnoreCase("combo")))
                    .filter(ProductoFinal::isActivo) // Solo activos
                    .collect(Collectors.toList());

            // Si no hay combos, devolver los primeros 5 productos como destacados
            if (combos.isEmpty()) {
                combos = todosLosProductos.stream()
                        .filter(ProductoFinal::isActivo)
                        .limit(5)
                        .collect(Collectors.toList());
            }

            // Convertir a la estructura que espera el frontend
            List<Map<String, Object>> combosData = new ArrayList<>();
            for (ProductoFinal producto : combos) {
                Map<String, Object> combo = new HashMap<>();
                combo.put("id", producto.getId());
                combo.put("nombre", producto.getNombre());
                combo.put("descripcion", producto.getDescripcion() != null ? producto.getDescripcion() : "");
                combo.put("precio", producto.getPrecio());
                combo.put("imagenUrl", producto.getImagenUrl() != null ? producto.getImagenUrl() : "/assets/images/default-combo.jpg");
                combosData.add(combo);
            }

            response.put("success", true);
            response.put("data", combosData);
            response.put("total", combosData.size());

            System.out.println("✅ Combos cargados: " + combosData.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error cargando combos: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al cargar combos: " + e.getMessage());
            response.put("data", new ArrayList<>());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    // ============ EXPORTAR EXCEL ============

    @GetMapping("/exportar-excel")
    @ResponseBody
    public void exportarExcel(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) String tipo,
            HttpServletResponse response) throws IOException {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "reporte_" + (tipo != null ? tipo : "ventas") + "_" + timestamp + ".xlsx";
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);

            // Obtener datos para el reporte
            List<Pedido> pedidos = obtenerPedidosFiltrados(fechaInicio, fechaFin);

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Reporte");

            // Estilo para headers
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Headers
            String[] headers = {"N° Pedido", "Cliente", "Email", "Teléfono", "Fecha", "Subtotal", "Costo Envío", "Total", "Estado", "Tipo Entrega", "Dirección", "Items"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Datos
            int rowNum = 1;
            for (Pedido pedido : pedidos) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(pedido.getNumeroPedido() != null ? pedido.getNumeroPedido() : "N/A");

                String cliente = "N/A";
                String email = "";
                String telefono = "";
                if (pedido.getUsuario() != null) {
                    cliente = (pedido.getUsuario().getNombres() != null ? pedido.getUsuario().getNombres() : "") + " " +
                            (pedido.getUsuario().getApellidos() != null ? pedido.getUsuario().getApellidos() : "");
                    cliente = cliente.trim().isEmpty() ? pedido.getUsuario().getUsername() : cliente;
                    email = pedido.getUsuario().getUsername() != null ? pedido.getUsuario().getUsername() : "";
                    telefono = pedido.getUsuario().getTelefono() != null ? pedido.getUsuario().getTelefono() : "";
                }
                row.createCell(1).setCellValue(cliente);
                row.createCell(2).setCellValue(email);
                row.createCell(3).setCellValue(telefono);

                row.createCell(4).setCellValue(pedido.getFecha() != null ?
                        pedido.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
                row.createCell(5).setCellValue(pedido.getSubtotal() != null ? pedido.getSubtotal() : 0.0);
                row.createCell(6).setCellValue(pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : 0.0);
                row.createCell(7).setCellValue(pedido.getTotal());
                row.createCell(8).setCellValue(pedido.getEstado() != null ? pedido.getEstado() : "N/A");
                row.createCell(9).setCellValue(pedido.getTipoEntrega() != null ? pedido.getTipoEntrega() : "N/A");
                row.createCell(10).setCellValue(pedido.getDireccionEntrega() != null ? pedido.getDireccionEntrega() : "N/A");

                // Items
                String itemsStr = "";
                if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
                    itemsStr = pedido.getItems().stream()
                            .map(item -> item.getNombreProducto() + " x" + item.getCantidad())
                            .collect(Collectors.joining(", "));
                }
                row.createCell(11).setCellValue(itemsStr);
            }

            // Agregar fila de totales
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(6).setCellValue("TOTAL GENERAL:");
            double totalGeneral = pedidos.stream().mapToDouble(Pedido::getTotal).sum();
            totalRow.createCell(7).setCellValue(totalGeneral);
            totalRow.createCell(8).setCellValue("Total Pedidos: " + pedidos.size());

            // Auto-ajustar columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
            workbook.close();

            System.out.println("📊 Reporte Excel generado: " + filename);

        } catch (Exception e) {
            System.err.println("❌ Error al generar Excel: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al generar el Excel");
        }
    }

    // ============ MÉTODO AUXILIAR PARA FILTRAR PEDIDOS ============

    private List<Pedido> obtenerPedidosFiltrados(String fechaInicio, String fechaFin) {
        List<Pedido> pedidos = pedidoRepository.findAllWithItemsAndProducts();

        if (fechaInicio != null && fechaFin != null) {
            try {
                LocalDateTime inicio = LocalDate.parse(fechaInicio).atStartOfDay();
                LocalDateTime fin = LocalDate.parse(fechaFin).atTime(23, 59, 59);

                pedidos = pedidos.stream()
                        .filter(p -> p.getFecha() != null)
                        .filter(p -> !p.getFecha().isBefore(inicio) && !p.getFecha().isAfter(fin))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                System.err.println("⚠️ Error al parsear fechas, usando todos los pedidos");
            }
        }

        return pedidos;
    }
}