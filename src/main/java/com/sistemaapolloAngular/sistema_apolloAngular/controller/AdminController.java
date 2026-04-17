package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.service.ProductoFinalService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin-menu")
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

    @GetMapping("")
    public String adminMenu(Model model) {
        List<ProductoFinal> productos = productoFinalService.obtenerTodos();
        model.addAttribute("productos", productos);
        model.addAttribute("pagina", "dashboard");
        return "admin-menu";
    }



    @GetMapping("/estadisticas-dashboard")
    @ResponseBody
    public Map<String, Object> obtenerEstadisticasDashboard() {
        Map<String, Object> estadisticas = new HashMap<>();

        try {
            // Obtener fechas para hoy
            LocalDate hoy = LocalDate.now();
            LocalDateTime hoyInicio = hoy.atStartOfDay();
            LocalDateTime hoyFin = hoy.atTime(23, 59, 59);

            // Obtener fechas para el mes actual
            LocalDate primerDiaMes = hoy.withDayOfMonth(1);
            LocalDateTime mesInicio = primerDiaMes.atStartOfDay();
            LocalDateTime mesFin = hoy.atTime(23, 59, 59);

            // Estadísticas básicas
            List<ProductoFinal> productos = productoFinalService.obtenerTodos();
            List<Usuario> usuarios = usuarioRepository.findAll();

            // Obtener todos los pedidos
            List<Pedido> todosLosPedidos = pedidoRepository.findAll();

            // Pedidos de hoy
            List<Pedido> pedidosHoy = todosLosPedidos.stream()
                    .filter(pedido -> {
                        if (pedido.getFecha() == null) return false;
                        LocalDateTime fechaPedido = pedido.getFecha();
                        return !fechaPedido.isBefore(hoyInicio) && !fechaPedido.isAfter(hoyFin);
                    })
                    .collect(Collectors.toList());

            // Calcular ingresos de hoy (solo pedidos entregados)
            double ingresosHoy = pedidosHoy.stream()
                    .filter(p -> "ENTREGADO".equals(p.getEstado()))
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            // Pedidos del mes
            List<Pedido> pedidosMes = todosLosPedidos.stream()
                    .filter(pedido -> {
                        if (pedido.getFecha() == null) return false;
                        LocalDateTime fechaPedido = pedido.getFecha();
                        return !fechaPedido.isBefore(mesInicio) && !fechaPedido.isAfter(mesFin);
                    })
                    .collect(Collectors.toList());

            // Estadísticas del mes
            double ventasMesTotal = pedidosMes.stream()
                    .filter(p -> "ENTREGADO".equals(p.getEstado()))
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            long totalPedidosMes = pedidosMes.size();

            // Venta más alta del mes
            double ventaMaxima = pedidosMes.stream()
                    .filter(p -> "ENTREGADO".equals(p.getEstado()))
                    .mapToDouble(Pedido::getTotal)
                    .max()
                    .orElse(0.0);

            // Promedio diario
            double promedioDiario = hoy.getDayOfMonth() > 0 ? ventasMesTotal / hoy.getDayOfMonth() : 0.0;

            // Llenar las estadísticas
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
    public List<Map<String, Object>> obtenerVentasRecientes() {
        try {
            // Usar la nueva consulta que carga items y productos
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

                // Información del cliente
                if (pedido.getUsuario() != null) {
                    venta.put("usuario", Map.of(
                            "nombres", pedido.getUsuario().getNombres(),
                            "apellidos", pedido.getUsuario().getApellidos()
                    ));
                } else {
                    venta.put("usuario", null);
                }

                // === PARTE CRÍTICA: INCLUIR ITEMS CON PRODUCTOS ===
                List<Map<String, Object>> itemsData = new ArrayList<>();
                if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
                    for (ItemPedido item : pedido.getItems()) {
                        Map<String, Object> itemData = new HashMap<>();
                        itemData.put("nombreProducto", item.getNombreProducto());
                        itemData.put("nombreProductoSeguro", item.getNombreProductoSeguro()); // NUEVO MÉTODO
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
    public Map<String, Object> obtenerEstadisticasVentas() {
        Map<String, Object> estadisticas = new HashMap<>();

        try {
            // Últimos 7 días para el gráfico
            LocalDate hoy = LocalDate.now();
            Map<String, Double> ventasUltimaSemana = new LinkedHashMap<>();

            List<Pedido> todosLosPedidos = pedidoRepository.findAll();

            for (int i = 6; i >= 0; i--) {
                LocalDate fecha = hoy.minusDays(i);
                LocalDateTime inicioDia = fecha.atStartOfDay();
                LocalDateTime finDia = fecha.atTime(23, 59, 59);

                // Filtrar pedidos del día
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
    public void exportarDashboardExcel(HttpServletResponse response) throws IOException {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "dashboard_apollo_" + timestamp + ".xlsx";
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);

            List<ProductoFinal> productos = productoFinalService.obtenerTodos();

            Workbook workbook = new XSSFWorkbook();

            //  RESUMEN DEL DASHBOARD
            Sheet resumenSheet = workbook.createSheet("Resumen Dashboard");

            // Estilo para títulos
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);

            // Estilo para métricas
            CellStyle metricStyle = workbook.createCellStyle();
            metricStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            metricStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font metricFont = workbook.createFont();
            metricFont.setBold(true);
            metricStyle.setFont(metricFont);

            // Título del reporte
            Row titleRow = resumenSheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DASHBOARD - SISTEMA APOLLO");
            titleCell.setCellStyle(titleStyle);

            // Métricas principales
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

            //  ESTADÍSTICAS POR CATEGORÍA
            Sheet statsSheet = workbook.createSheet("Estadísticas por Categoría");

            // Encabezados
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

            // Agrupar por categoría y calcular estadísticas
            Map<String, List<ProductoFinal>> productosPorCategoria = productos.stream()
                    .collect(Collectors.groupingBy(ProductoFinal::getTipo));

            int statsRowNum = 1;
            for (Map.Entry<String, List<ProductoFinal>> entry : productosPorCategoria.entrySet()) {
                String categoria = entry.getKey();
                List<ProductoFinal> productosCategoria = entry.getValue();

                double promedio = productosCategoria.stream()
                        .mapToDouble(ProductoFinal::getPrecio)
                        .average()
                        .orElse(0.0);
                double maxPrecio = productosCategoria.stream()
                        .mapToDouble(ProductoFinal::getPrecio)
                        .max()
                        .orElse(0.0);
                double minPrecio = productosCategoria.stream()
                        .mapToDouble(ProductoFinal::getPrecio)
                        .min()
                        .orElse(0.0);
                double valorTotal = productosCategoria.stream()
                        .mapToDouble(ProductoFinal::getPrecio)
                        .sum();

                Row statsRow = statsSheet.createRow(statsRowNum++);
                statsRow.createCell(0).setCellValue(categoria);
                statsRow.createCell(1).setCellValue(productosCategoria.size());
                statsRow.createCell(2).setCellValue(promedio);
                statsRow.createCell(3).setCellValue(maxPrecio);
                statsRow.createCell(4).setCellValue(minPrecio);
                statsRow.createCell(5).setCellValue(valorTotal);
            }

            // LISTA COMPLETA DE PRODUCTOS
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


    @GetMapping("/nuevo")
    public String nuevoProducto(Model model) {
        model.addAttribute("pagina", "nuevo-producto");
        model.addAttribute("producto", new ProductoFinal());
        return "nuevo-producto";
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
            System.out.println(" Recibiendo producto para guardar:");
            System.out.println("Nombre: " + nombre);
            System.out.println("Tipo: " + tipo);
            System.out.println("Precio: " + precio);

            // Validar datos básicos
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

            // Guardar en la base de datos
            ProductoFinal productoCreado = productoFinalService.guardar(producto);

            System.out.println(" NUEVO PRODUCTO GUARDADO EN BD");
            System.out.println("ID: " + productoCreado.getId());
            System.out.println("Nombre: " + productoCreado.getNombre());
            System.out.println("Precio: S/." + productoCreado.getPrecio());
            System.out.println("Tipo: " + productoCreado.getTipo());
            System.out.println("Imagen: " + productoCreado.getImagenUrl());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Producto guardado exitosamente!",
                    "producto", productoCreado
            ));

        } catch (Exception e) {
            System.err.println(" Error al guardar producto: " + e.getMessage());
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
            System.out.println(" Actualizando producto ID: " + id);

            // Validaciones
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

                // Manejar imagen URL
                if (imagenUrl != null && !imagenUrl.trim().isEmpty()) {
                    producto.setImagenUrl(imagenUrl.trim());
                } else {
                    producto.setImagenUrl("/imagenes/default-product.jpg");
                }

                ProductoFinal productoActualizado = productoFinalService.guardar(producto);

                System.out.println(" PRODUCTO ACTUALIZADO EN BD");
                System.out.println("ID: " + productoActualizado.getId());
                System.out.println("Nombre: " + productoActualizado.getNombre());

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Producto actualizado exitosamente!",
                        "producto", productoActualizado
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Producto no encontrado"));
            }

        } catch (Exception e) {
            System.err.println(" Error al actualizar producto: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Error al actualizar el producto: " + e.getMessage()));
        }
    }


    @PostMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarProducto(
            @PathVariable Long id,
            @RequestParam(required = false) String redirectSection) {

        try {
            System.out.println(" Eliminando producto ID: " + id);

            Optional<ProductoFinal> productoOpt = productoFinalService.obtenerPorId(id);
            if (productoOpt.isPresent()) {
                productoFinalService.eliminar(id);
                System.out.println(" PRODUCTO ELIMINADO: ID " + id);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Producto eliminado exitosamente!"
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Producto no encontrado"));
            }

        } catch (Exception e) {
            System.err.println("Error al eliminar producto: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Error al eliminar el producto: " + e.getMessage()));
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
            estadisticas.put("precioPromedio", productos.stream()
                    .mapToDouble(ProductoFinal::getPrecio)
                    .average().orElse(0.0));
            estadisticas.put("precioMaximo", productos.stream()
                    .mapToDouble(ProductoFinal::getPrecio)
                    .max().orElse(0.0));
            estadisticas.put("precioMinimo", productos.stream()
                    .mapToDouble(ProductoFinal::getPrecio)
                    .min().orElse(0.0));


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
            System.out.println(" Recibiendo usuario para guardar:");
            System.out.println("Nombres: " + nombres);
            System.out.println("Email: " + email);
            System.out.println("Rol: " + rol);

            // Validar que el rol sea válido (AGREGAR "delivery")
            List<String> rolesValidos = Arrays.asList("admin", "cajero", "cocinero", "delivery");
            if (!rolesValidos.contains(rol.toLowerCase())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Rol no válido. Roles permitidos: admin, cajero, cocinero, delivery"
                ));
            }

            // Validar que el email no exista
            if (usuarioRepository.findByUsername(email).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El correo electrónico ya está registrado"
                ));
            }

            // Validar contraseña
            if (password == null || password.trim().length() < 6) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "La contraseña debe tener al menos 6 caracteres"
                ));
            }

            // Crear nuevo usuario
            Usuario usuario = new Usuario();
            usuario.setNombres(nombres.trim());
            usuario.setApellidos(apellidos.trim());
            usuario.setTipoDocumento(tipoDocumento);
            usuario.setNumeroDocumento(numeroDocumento.trim());
            usuario.setTelefono(telefono.trim());
            usuario.setFechaNacimiento(LocalDate.parse(fechaNacimiento));
            usuario.setUsername(email.trim());
            usuario.setRol(rol.toUpperCase()); // Esto convertirá "delivery" a "DELIVERY"
            usuario.setPassword(passwordEncoder.encode(password));

            // Guardar en la base de datos
            Usuario usuarioCreado = usuarioRepository.save(usuario);

            System.out.println(" NUEVO USUARIO GUARDADO EN BD");
            System.out.println("ID: " + usuarioCreado.getId());
            System.out.println("Nombre: " + usuarioCreado.getNombres() + " " + usuarioCreado.getApellidos());
            System.out.println("Rol: " + usuarioCreado.getRol());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Usuario creado exitosamente!",
                    "usuario", usuarioCreado
            ));

        } catch (Exception e) {
            System.err.println(" Error al guardar usuario: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Error al guardar el usuario: " + e.getMessage()
            ));
        }
    }


    @PostMapping("/usuarios/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            System.out.println(" Eliminando usuario ID: " + id);

            Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
            if (usuarioOpt.isPresent()) {
                usuarioRepository.deleteById(id);
                System.out.println(" USUARIO ELIMINADO: ID " + id);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Usuario eliminado exitosamente!"
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "Usuario no encontrado"
                ));
            }

        } catch (Exception e) {
            System.err.println(" Error al eliminar usuario: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Error al eliminar el usuario: " + e.getMessage()
            ));
        }
    }
}
