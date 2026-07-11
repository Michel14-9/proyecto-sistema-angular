package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.sistemaapolloAngular.sistema_apolloAngular.exception.BusinessException;
import com.sistemaapolloAngular.sistema_apolloAngular.exception.ResourceNotFoundException;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Favorito;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.FavoritoRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.service.ProductoFinalService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AdminController {

    private final ProductoFinalService productoFinalService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FavoritoRepository favoritoRepository;

    public AdminController(ProductoFinalService productoFinalService) {
        this.productoFinalService = productoFinalService;
    }

    // ============================================================
    // 1. ENDPOINTS PÚBLICOS PARA PYTHON
    // ============================================================

    @GetMapping("/data/productos")
    @ResponseBody
    public List<ProductoFinal> getProductosForPython() {
        return productoFinalService.obtenerTodos();
    }

    @GetMapping("/data/usuarios")
    @ResponseBody
    public List<Usuario> getUsuariosForPython() {
        return usuarioRepository.findAll();
    }

    @GetMapping("/data/pedidos")
    @ResponseBody
    @Transactional
    public List<Pedido> getPedidosForPython() {
        return pedidoRepository.findAllWithItemsAndProducts();
    }

    @GetMapping("/data/pedidos/entregados")
    @ResponseBody
    @Transactional
    public List<Pedido> getPedidosEntregadosForPython() {
        return pedidoRepository.findAllWithItemsAndProducts()
                .stream()
                .filter(p -> "ENTREGADO".equals(p.getEstado()))
                .collect(Collectors.toList());
    }

    @GetMapping("/data/pedidos/pagados")
    @ResponseBody
    @Transactional
    public List<Pedido> getPedidosPagadosForPython() {
        return pedidoRepository.findAllWithItemsAndProducts()
                .stream()
                .filter(p -> "PAGADO".equals(p.getEstado()))
                .collect(Collectors.toList());
    }

    @GetMapping("/data/favoritos")
    @ResponseBody
    @Transactional
    public List<Map<String, Object>> getFavoritosForPython() {
        List<Map<String, Object>> response = new ArrayList<>();

        try {
            List<Favorito> favoritos = favoritoRepository.findAllWithUsuarioAndProducto();

            for (Favorito f : favoritos) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", f.getId());
                item.put("fechaAgregado", f.getFechaAgregado());
                item.put("activo", f.isActivo());

                if (f.getUsuario() != null) {
                    item.put("usuarioId", f.getUsuario().getId());
                    String nombreCompleto = (f.getUsuario().getNombres() != null ? f.getUsuario().getNombres() : "") + " " +
                            (f.getUsuario().getApellidos() != null ? f.getUsuario().getApellidos() : "");
                    item.put("usuarioNombre", nombreCompleto.trim().isEmpty() ? f.getUsuario().getUsername() : nombreCompleto.trim());
                    item.put("usuarioEmail", f.getUsuario().getUsername());
                }

                if (f.getProducto() != null) {
                    item.put("productoId", f.getProducto().getId());
                    item.put("productoNombre", f.getProducto().getNombre());
                    item.put("productoCategoria", f.getProducto().getTipo() != null ? f.getProducto().getTipo() : "Sin categoría");
                    item.put("productoPrecio", f.getProducto().getPrecio());
                }

                response.add(item);
            }

            log.info("📤 Favoritos enviados a Python: {}", response.size());

        } catch (Exception e) {
            log.error("❌ Error obteniendo favoritos: {}", e.getMessage(), e);
        }

        return response;
    }

    // ============================================================
    // 2. CRUD DE PRODUCTOS
    // ============================================================

    @GetMapping("/productos")
    @ResponseBody
    public List<ProductoFinal> obtenerTodosProductos() {
        return productoFinalService.obtenerTodos();
    }

    @GetMapping("/productos/{id}")
    @ResponseBody
    public ResponseEntity<ProductoFinal> obtenerProductoPorId(@PathVariable Long id) {
        ProductoFinal producto = productoFinalService.obtenerPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
        return ResponseEntity.ok(producto);
    }

    @PostMapping("/productos/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarProducto(
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam Double precio,
            @RequestParam String tipo,
            @RequestParam(required = false) String imagenUrl) {

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new BusinessException("El nombre del producto es requerido");
        }

        if (precio == null || precio <= 0) {
            throw new BusinessException("El precio debe ser mayor a 0");
        }

        if (tipo == null || tipo.trim().isEmpty()) {
            throw new BusinessException("El tipo/categoría es requerido");
        }

        ProductoFinal producto = new ProductoFinal();
        producto.setNombre(nombre.trim());
        producto.setDescripcion(descripcion != null ? descripcion.trim() : "");
        producto.setPrecio(precio);
        producto.setTipo(tipo.trim());
        producto.setImagenUrl(imagenUrl != null && !imagenUrl.trim().isEmpty()
                ? imagenUrl.trim()
                : "/imagenes/default-product.jpg");

        ProductoFinal productoCreado = productoFinalService.guardar(producto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Producto guardado exitosamente!");
        response.put("producto", productoCreado);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/productos/actualizar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarProducto(
            @PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam Double precio,
            @RequestParam String tipo,
            @RequestParam(required = false) String imagenUrl) {

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new BusinessException("El nombre del producto es requerido");
        }

        if (precio == null || precio <= 0) {
            throw new BusinessException("El precio debe ser mayor a 0");
        }

        if (tipo == null || tipo.trim().isEmpty()) {
            throw new BusinessException("El tipo/categoría es requerido");
        }

        ProductoFinal producto = productoFinalService.obtenerPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        producto.setNombre(nombre.trim());
        producto.setDescripcion(descripcion != null ? descripcion.trim() : "");
        producto.setPrecio(precio);
        producto.setTipo(tipo.trim());
        producto.setImagenUrl(imagenUrl != null && !imagenUrl.trim().isEmpty()
                ? imagenUrl.trim()
                : "/imagenes/default-product.jpg");

        ProductoFinal productoActualizado = productoFinalService.guardar(producto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Producto actualizado exitosamente!");
        response.put("producto", productoActualizado);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/productos/eliminar/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> eliminarProducto(@PathVariable Long id) {
        ProductoFinal producto = productoFinalService.obtenerPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        String nombreProducto = producto.getNombre();
        boolean tieneItemsPedido = pedidoRepository.existsByItemsProductoId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        if (tieneItemsPedido) {
            producto.setActivo(false);
            productoFinalService.guardar(producto);
            response.put("message", "Producto '" + nombreProducto + "' desactivado (tiene pedidos históricos).");
            response.put("desactivado", true);
        } else {
            productoFinalService.eliminar(id);
            response.put("message", "Producto '" + nombreProducto + "' eliminado exitosamente!");
            response.put("desactivado", false);
        }

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 3. CRUD DE USUARIOS
    // ============================================================

    @GetMapping("/usuarios")
    @ResponseBody
    public List<Usuario> obtenerTodosUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        log.info("👥 Enviando {} usuarios al frontend", usuarios.size());
        return usuarios;
    }

    @PostMapping("/usuarios/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarUsuario(
            @RequestParam String nombres,
            @RequestParam String apellidos,
            @RequestParam String tipoDocumento,
            @RequestParam String numeroDocumento,
            @RequestParam String telefono,
            @RequestParam String fechaNacimiento,
            @RequestParam String email,
            @RequestParam String rol,
            @RequestParam String password) {

        List<String> rolesValidos = Arrays.asList("admin", "cajero", "cocinero", "delivery");
        if (!rolesValidos.contains(rol.toLowerCase())) {
            throw new BusinessException("Rol no válido. Roles permitidos: admin, cajero, cocinero, delivery");
        }

        if (usuarioRepository.findByUsername(email).isPresent()) {
            throw new BusinessException("El correo electrónico ya está registrado");
        }

        if (password == null || password.trim().length() < 6) {
            throw new BusinessException("La contraseña debe tener al menos 6 caracteres");
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

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario creado exitosamente!");
        response.put("usuario", usuarioCreado);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/usuarios/eliminar/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> eliminarUsuario(@PathVariable Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        if ("admin@luren.com".equals(usuario.getUsername())) {
            throw new BusinessException("No se puede eliminar al administrador principal");
        }

        String nombreUsuario = usuario.getNombres() + " " + usuario.getApellidos();
        boolean tienePedidos = pedidoRepository.existsByUsuarioId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        if (tienePedidos) {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
            response.put("message", "Usuario '" + nombreUsuario + "' desactivado (tiene pedidos históricos).");
            response.put("desactivado", true);
        } else {
            usuarioRepository.deleteById(id);
            response.put("message", "Usuario '" + nombreUsuario + "' eliminado exitosamente!");
            response.put("desactivado", false);
        }

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 4. COMBOS
    // ============================================================

    @GetMapping("/menu/combos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerCombosApi() {
        List<ProductoFinal> todosLosProductos = productoFinalService.obtenerTodos();

        List<ProductoFinal> combos = todosLosProductos.stream()
                .filter(p -> p.getTipo() != null &&
                        (p.getTipo().equalsIgnoreCase("COMBO") ||
                                p.getTipo().equalsIgnoreCase("combo")))
                .filter(ProductoFinal::isActivo)
                .collect(Collectors.toList());

        if (combos.isEmpty()) {
            combos = todosLosProductos.stream()
                    .filter(ProductoFinal::isActivo)
                    .limit(5)
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> combosData = new ArrayList<>();
        for (ProductoFinal producto : combos) {
            Map<String, Object> combo = new HashMap<>();
            combo.put("id", producto.getId());
            combo.put("nombre", producto.getNombre());
            combo.put("descripcion", producto.getDescripcion() != null ? producto.getDescripcion() : "");
            combo.put("precio", producto.getPrecio());
            combo.put("imagenUrl", producto.getImagenUrl() != null ?
                    producto.getImagenUrl() : "/assets/images/default-combo.jpg");
            combosData.add(combo);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", combosData);
        response.put("total", combosData.size());

        log.info(" Combos cargados: {}", combosData.size());

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 5. EXPORTACIONES PROFESIONALES (PDF Y EXCEL)
    // ============================================================

    @GetMapping("/exportar-pdf")
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

            List<Pedido> pedidos = obtenerPedidosFiltrados(fechaInicio, fechaFin);
            List<ProductoFinal> productos = productoFinalService.obtenerTodos();
            List<Usuario> usuarios = usuarioRepository.findAll();

            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.setMargins(30, 30, 30, 30);

            String tituloReporte = getTituloReporte(tipo);
            Paragraph titulo = new Paragraph(tituloReporte)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(20);
            document.add(titulo);
            document.add(new Paragraph(" "));

            Paragraph subtitulo = new Paragraph("Generado: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10);
            document.add(subtitulo);
            document.add(new Paragraph(" "));

            if (fechaInicio != null && fechaFin != null) {
                Paragraph periodo = new Paragraph("Período: " + fechaInicio + " al " + fechaFin)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10);
                document.add(periodo);
            }
            document.add(new Paragraph(" "));
            document.add(new Paragraph("_________________________________________"));
            document.add(new Paragraph(" "));

            switch (tipo != null ? tipo : "ventas") {
                case "productos":
                    generarPDFProductos(document, pedidos, productos);
                    break;
                case "usuarios":
                    generarPDFUsuarios(document, pedidos, usuarios);
                    break;
                case "pedidos":
                    generarPDFPedidos(document, pedidos);
                    break;
                case "metodos-pago":
                    generarPDFMetodosPago(document, pedidos);
                    break;
                case "tipos-entrega":
                    generarPDFTiposEntrega(document, pedidos);
                    break;
                case "horarios":
                    generarPDFHorarios(document, pedidos);
                    break;
                case "favoritos":
                    generarPDFFavoritos(document);
                    break;
                default:
                    generarPDFVentas(document, pedidos);
                    break;
            }

            document.close();
            log.info(" Reporte PDF generado: {}", filename);

        } catch (Exception e) {
            log.error(" Error al generar PDF: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al generar el PDF");
        }
    }

    @GetMapping("/exportar-excel")
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

            List<Pedido> pedidos = obtenerPedidosFiltrados(fechaInicio, fechaFin);
            List<ProductoFinal> productos = productoFinalService.obtenerTodos();
            List<Usuario> usuarios = usuarioRepository.findAll();

            Workbook workbook = new XSSFWorkbook();

            switch (tipo != null ? tipo : "ventas") {
                case "productos":
                    generarExcelProductos(workbook, pedidos, productos);
                    break;
                case "usuarios":
                    generarExcelUsuarios(workbook, pedidos, usuarios);
                    break;
                case "pedidos":
                    generarExcelPedidos(workbook, pedidos);
                    break;
                case "metodos-pago":
                    generarExcelMetodosPago(workbook, pedidos);
                    break;
                case "tipos-entrega":
                    generarExcelTiposEntrega(workbook, pedidos);
                    break;
                case "horarios":
                    generarExcelHorarios(workbook, pedidos);
                    break;
                case "favoritos":
                    generarExcelFavoritos(workbook);
                    break;
                default:
                    generarExcelVentas(workbook, pedidos);
                    break;
            }

            workbook.write(response.getOutputStream());
            workbook.close();

            log.info("✅ Reporte Excel generado: {}", filename);

        } catch (Exception e) {
            log.error("💥 Error al generar Excel: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al generar el Excel");
        }
    }

    // ============================================================
    // MÉTODOS AUXILIARES PARA PDF
    // ============================================================

    private String getTituloReporte(String tipo) {
        Map<String, String> titulos = new HashMap<>();
        titulos.put("ventas", "REPORTE DE VENTAS");
        titulos.put("productos", "REPORTE DE PRODUCTOS MÁS VENDIDOS");
        titulos.put("usuarios", "REPORTE DE ACTIVIDAD DE USUARIOS");
        titulos.put("pedidos", "REPORTE DE ESTADÍSTICAS DE PEDIDOS");
        titulos.put("metodos-pago", "REPORTE DE MÉTODOS DE PAGO");
        titulos.put("tipos-entrega", "REPORTE DE TIPOS DE ENTREGA");
        titulos.put("horarios", "REPORTE DE HORARIOS PICO");
        titulos.put("favoritos", "REPORTE DE PRODUCTOS FAVORITOS");
        return titulos.getOrDefault(tipo, "REPORTE");
    }

    private void generarPDFVentas(Document document, List<Pedido> pedidos) {
        try {
            document.add(new Paragraph("📊 RESUMEN DE VENTAS").setBold().setFontSize(12));
            document.add(new Paragraph(" "));

            double totalVentas = pedidos.stream().mapToDouble(Pedido::getTotal).sum();
            document.add(new Paragraph("Total de Ventas: S/ " + String.format("%.2f", totalVentas)));
            document.add(new Paragraph("Total de Pedidos: " + pedidos.size()));
            document.add(new Paragraph(" "));

            float[] columnWidths = {2, 3, 2, 2, 2, 3};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(new Paragraph("N° Pedido").setBold());
            table.addHeaderCell(new Paragraph("Cliente").setBold());
            table.addHeaderCell(new Paragraph("Fecha").setBold());
            table.addHeaderCell(new Paragraph("Total").setBold());
            table.addHeaderCell(new Paragraph("Estado").setBold());
            table.addHeaderCell(new Paragraph("Tipo Entrega").setBold());

            for (Pedido pedido : pedidos.stream().limit(100).collect(Collectors.toList())) {
                table.addCell(new Paragraph(pedido.getNumeroPedido() != null ? pedido.getNumeroPedido() : "N/A"));
                table.addCell(new Paragraph(getNombreCliente(pedido)));
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
            document.add(new Paragraph("TOTAL GENERAL: S/ " + String.format("%.2f", totalVentas)).setBold());

        } catch (Exception e) {
            log.error("❌ Error generando PDF Ventas: {}", e.getMessage(), e);
        }
    }

    private void generarPDFProductos(Document document, List<Pedido> pedidos, List<ProductoFinal> productos) {
        try {
            document.add(new Paragraph("🏆 TOP PRODUCTOS MÁS VENDIDOS").setBold().setFontSize(12));
            document.add(new Paragraph(" "));

            Map<String, Integer> productosVendidos = new HashMap<>();
            Map<String, Double> montosPorProducto = new HashMap<>();

            for (Pedido pedido : pedidos) {
                for (ItemPedido item : pedido.getItems()) {
                    String nombre = item.getNombreProductoSeguro();
                    productosVendidos.put(nombre, productosVendidos.getOrDefault(nombre, 0) + item.getCantidad());
                    montosPorProducto.put(nombre, montosPorProducto.getOrDefault(nombre, 0.0) + item.getSubtotal());
                }
            }

            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(productosVendidos.entrySet());
            sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            float[] columnWidths = {1, 3, 2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(new Paragraph("#").setBold());
            table.addHeaderCell(new Paragraph("Producto").setBold());
            table.addHeaderCell(new Paragraph("Cantidad Vendida").setBold());
            table.addHeaderCell(new Paragraph("Monto Total").setBold());

            int i = 1;
            for (Map.Entry<String, Integer> entry : sorted.stream().limit(20).collect(Collectors.toList())) {
                table.addCell(new Paragraph(String.valueOf(i++)));
                table.addCell(new Paragraph(entry.getKey()));
                table.addCell(new Paragraph(String.valueOf(entry.getValue())));
                table.addCell(new Paragraph("S/ " + String.format("%.2f", montosPorProducto.get(entry.getKey()))));
            }

            document.add(table);

        } catch (Exception e) {
            log.error("❌ Error generando PDF Productos: {}", e.getMessage(), e);
        }
    }

    private void generarPDFUsuarios(Document document, List<Pedido> pedidos, List<Usuario> usuarios) {
        try {
            document.add(new Paragraph("👥 ACTIVIDAD DE USUARIOS").setBold().setFontSize(12));
            document.add(new Paragraph(" "));

            Map<String, Integer> pedidosPorUsuario = new HashMap<>();
            for (Pedido pedido : pedidos) {
                if (pedido.getUsuario() != null) {
                    String nombre = getNombreCliente(pedido);
                    pedidosPorUsuario.put(nombre, pedidosPorUsuario.getOrDefault(nombre, 0) + 1);
                }
            }

            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(pedidosPorUsuario.entrySet());
            sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            float[] columnWidths = {1, 3, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(new Paragraph("#").setBold());
            table.addHeaderCell(new Paragraph("Usuario").setBold());
            table.addHeaderCell(new Paragraph("Pedidos Realizados").setBold());

            int i = 1;
            for (Map.Entry<String, Integer> entry : sorted.stream().limit(20).collect(Collectors.toList())) {
                table.addCell(new Paragraph(String.valueOf(i++)));
                table.addCell(new Paragraph(entry.getKey()));
                table.addCell(new Paragraph(String.valueOf(entry.getValue())));
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total de Usuarios Activos: " + pedidosPorUsuario.size()));

        } catch (Exception e) {
            log.error("❌ Error generando PDF Usuarios: {}", e.getMessage(), e);
        }
    }

    private void generarPDFPedidos(Document document, List<Pedido> pedidos) {
        try {
            document.add(new Paragraph("📦 ESTADÍSTICAS DE PEDIDOS").setBold().setFontSize(12));
            document.add(new Paragraph(" "));

            Map<String, Integer> estados = new HashMap<>();
            for (Pedido pedido : pedidos) {
                String estado = pedido.getEstado() != null ? pedido.getEstado() : "DESCONOCIDO";
                estados.put(estado, estados.getOrDefault(estado, 0) + 1);
            }

            float[] columnWidths = {2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(new Paragraph("Estado").setBold());
            table.addHeaderCell(new Paragraph("Cantidad").setBold());

            for (Map.Entry<String, Integer> entry : estados.entrySet()) {
                table.addCell(new Paragraph(entry.getKey()));
                table.addCell(new Paragraph(String.valueOf(entry.getValue())));
            }

            document.add(table);

        } catch (Exception e) {
            log.error("❌ Error generando PDF Pedidos: {}", e.getMessage(), e);
        }
    }

    private void generarPDFMetodosPago(Document document, List<Pedido> pedidos) {
        try {
            document.add(new Paragraph("💳 MÉTODOS DE PAGO").setBold().setFontSize(12));
            document.add(new Paragraph(" "));

            Map<String, Integer> metodos = new HashMap<>();
            Map<String, Double> montos = new HashMap<>();

            for (Pedido pedido : pedidos) {
                String metodo = pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado";
                metodos.put(metodo, metodos.getOrDefault(metodo, 0) + 1);
                montos.put(metodo, montos.getOrDefault(metodo, 0.0) + pedido.getTotal());
            }

            float[] columnWidths = {2, 2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(new Paragraph("Método de Pago").setBold());
            table.addHeaderCell(new Paragraph("Cantidad de Pedidos").setBold());
            table.addHeaderCell(new Paragraph("Monto Total").setBold());

            for (Map.Entry<String, Integer> entry : metodos.entrySet()) {
                table.addCell(new Paragraph(entry.getKey()));
                table.addCell(new Paragraph(String.valueOf(entry.getValue())));
                table.addCell(new Paragraph("S/ " + String.format("%.2f", montos.get(entry.getKey()))));
            }

            document.add(table);

        } catch (Exception e) {
            log.error("❌ Error generando PDF Métodos de Pago: {}", e.getMessage(), e);
        }
    }

    private void generarPDFTiposEntrega(Document document, List<Pedido> pedidos) {
        try {
            document.add(new Paragraph("🚚 TIPOS DE ENTREGA").setBold().setFontSize(12));
            document.add(new Paragraph(" "));

            Map<String, Integer> tipos = new HashMap<>();
            Map<String, Double> montos = new HashMap<>();

            for (Pedido pedido : pedidos) {
                String tipo = pedido.getTipoEntrega() != null ? pedido.getTipoEntrega() : "No especificado";
                tipos.put(tipo, tipos.getOrDefault(tipo, 0) + 1);
                montos.put(tipo, montos.getOrDefault(tipo, 0.0) + pedido.getTotal());
            }

            float[] columnWidths = {2, 2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(new Paragraph("Tipo de Entrega").setBold());
            table.addHeaderCell(new Paragraph("Cantidad de Pedidos").setBold());
            table.addHeaderCell(new Paragraph("Monto Total").setBold());

            for (Map.Entry<String, Integer> entry : tipos.entrySet()) {
                table.addCell(new Paragraph(entry.getKey()));
                table.addCell(new Paragraph(String.valueOf(entry.getValue())));
                table.addCell(new Paragraph("S/ " + String.format("%.2f", montos.get(entry.getKey()))));
            }

            document.add(table);

        } catch (Exception e) {
            log.error("❌ Error generando PDF Tipos de Entrega: {}", e.getMessage(), e);
        }
    }

    private void generarPDFHorarios(Document document, List<Pedido> pedidos) {
        try {
            document.add(new Paragraph("🕐 HORARIOS PICO").setBold().setFontSize(12));
            document.add(new Paragraph(" "));

            Map<String, Integer> horas = new LinkedHashMap<>();
            for (int i = 0; i < 24; i++) {
                horas.put(String.format("%02d:00 - %02d:00", i, i + 1), 0);
            }

            for (Pedido pedido : pedidos) {
                if (pedido.getFecha() != null) {
                    int hora = pedido.getFecha().getHour();
                    String rango = String.format("%02d:00 - %02d:00", hora, hora + 1);
                    horas.put(rango, horas.getOrDefault(rango, 0) + 1);
                }
            }

            Map<String, Integer> horasConPedidos = horas.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

            float[] columnWidths = {2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(new Paragraph("Hora").setBold());
            table.addHeaderCell(new Paragraph("Pedidos").setBold());

            for (Map.Entry<String, Integer> entry : horasConPedidos.entrySet()) {
                table.addCell(new Paragraph(entry.getKey()));
                table.addCell(new Paragraph(String.valueOf(entry.getValue())));
            }

            document.add(table);

            if (!horasConPedidos.isEmpty()) {
                Map.Entry<String, Integer> horaPico = horasConPedidos.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .orElse(null);
                if (horaPico != null) {
                    document.add(new Paragraph(" "));
                    document.add(new Paragraph("🔥 HORA PICO: " + horaPico.getKey() + " (" + horaPico.getValue() + " pedidos)").setBold());
                }
            }

        } catch (Exception e) {
            log.error("❌ Error generando PDF Horarios: {}", e.getMessage(), e);
        }
    }

    private void generarPDFFavoritos(Document document) {
        try {
            document.add(new Paragraph("❤️ PRODUCTOS FAVORITOS").setBold().setFontSize(12));
            document.add(new Paragraph(" "));

            List<Favorito> favoritos = favoritoRepository.findAllWithUsuarioAndProducto();

            Map<String, Integer> productosFavoritos = new HashMap<>();
            for (Favorito f : favoritos) {
                if (f.getProducto() != null) {
                    String nombre = f.getProducto().getNombre();
                    productosFavoritos.put(nombre, productosFavoritos.getOrDefault(nombre, 0) + 1);
                }
            }

            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(productosFavoritos.entrySet());
            sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            float[] columnWidths = {1, 3, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(new Paragraph("#").setBold());
            table.addHeaderCell(new Paragraph("Producto").setBold());
            table.addHeaderCell(new Paragraph("Veces Favorito").setBold());

            int i = 1;
            for (Map.Entry<String, Integer> entry : sorted.stream().limit(20).collect(Collectors.toList())) {
                table.addCell(new Paragraph(String.valueOf(i++)));
                table.addCell(new Paragraph(entry.getKey()));
                table.addCell(new Paragraph(String.valueOf(entry.getValue())));
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total de Favoritos: " + favoritos.size()));

        } catch (Exception e) {
            log.error("❌ Error generando PDF Favoritos: {}", e.getMessage(), e);
        }
    }

    // ============================================================
    // MÉTODOS AUXILIARES PARA EXCEL
    // ============================================================

    private void generarExcelVentas(Workbook workbook, List<Pedido> pedidos) {
        Sheet sheet = workbook.createSheet("Ventas");
        CellStyle headerStyle = crearEstiloCabecera(workbook);

        String[] headers = {"N° Pedido", "Cliente", "Fecha", "Total", "Estado", "Tipo Entrega"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        int rowNum = 1;
        double totalGeneral = 0;
        for (Pedido pedido : pedidos) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(pedido.getNumeroPedido() != null ? pedido.getNumeroPedido() : "N/A");
            row.createCell(1).setCellValue(getNombreCliente(pedido));
            row.createCell(2).setCellValue(pedido.getFecha() != null ?
                    pedido.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
            row.createCell(3).setCellValue(pedido.getTotal());
            row.createCell(4).setCellValue(pedido.getEstado() != null ? pedido.getEstado() : "N/A");
            row.createCell(5).setCellValue(pedido.getTipoEntrega() != null ? pedido.getTipoEntrega() : "N/A");
            totalGeneral += pedido.getTotal();
        }

        Row totalRow = sheet.createRow(rowNum);
        totalRow.createCell(3).setCellValue("TOTAL:");
        totalRow.createCell(4).setCellValue(totalGeneral);
        totalRow.createCell(5).setCellValue("Total Pedidos: " + pedidos.size());
    }

    private void generarExcelProductos(Workbook workbook, List<Pedido> pedidos, List<ProductoFinal> productos) {
        Sheet sheet = workbook.createSheet("Productos Más Vendidos");
        CellStyle headerStyle = crearEstiloCabecera(workbook);

        Map<String, Integer> productosVendidos = new HashMap<>();
        Map<String, Double> montosPorProducto = new HashMap<>();

        for (Pedido pedido : pedidos) {
            for (ItemPedido item : pedido.getItems()) {
                String nombre = item.getNombreProductoSeguro();
                productosVendidos.put(nombre, productosVendidos.getOrDefault(nombre, 0) + item.getCantidad());
                montosPorProducto.put(nombre, montosPorProducto.getOrDefault(nombre, 0.0) + item.getSubtotal());
            }
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(productosVendidos.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        String[] headers = {"#", "Producto", "Cantidad Vendida", "Monto Total"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        int rowNum = 1;
        for (Map.Entry<String, Integer> entry : sorted) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(entry.getKey());
            row.createCell(2).setCellValue(entry.getValue());
            row.createCell(3).setCellValue(montosPorProducto.get(entry.getKey()));
        }
    }

    private void generarExcelUsuarios(Workbook workbook, List<Pedido> pedidos, List<Usuario> usuarios) {
        Sheet sheet = workbook.createSheet("Actividad de Usuarios");
        CellStyle headerStyle = crearEstiloCabecera(workbook);

        Map<String, Integer> pedidosPorUsuario = new HashMap<>();
        for (Pedido pedido : pedidos) {
            if (pedido.getUsuario() != null) {
                String nombre = getNombreCliente(pedido);
                pedidosPorUsuario.put(nombre, pedidosPorUsuario.getOrDefault(nombre, 0) + 1);
            }
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(pedidosPorUsuario.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        String[] headers = {"#", "Usuario", "Pedidos Realizados"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        int rowNum = 1;
        for (Map.Entry<String, Integer> entry : sorted) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(entry.getKey());
            row.createCell(2).setCellValue(entry.getValue());
        }
    }

    private void generarExcelPedidos(Workbook workbook, List<Pedido> pedidos) {
        Sheet sheet = workbook.createSheet("Estadísticas de Pedidos");
        CellStyle headerStyle = crearEstiloCabecera(workbook);

        Map<String, Integer> estados = new HashMap<>();
        for (Pedido pedido : pedidos) {
            String estado = pedido.getEstado() != null ? pedido.getEstado() : "DESCONOCIDO";
            estados.put(estado, estados.getOrDefault(estado, 0) + 1);
        }

        String[] headers = {"Estado", "Cantidad"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        int rowNum = 1;
        for (Map.Entry<String, Integer> entry : estados.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }
    }

    private void generarExcelMetodosPago(Workbook workbook, List<Pedido> pedidos) {
        Sheet sheet = workbook.createSheet("Métodos de Pago");
        CellStyle headerStyle = crearEstiloCabecera(workbook);

        Map<String, Integer> metodos = new HashMap<>();
        Map<String, Double> montos = new HashMap<>();

        for (Pedido pedido : pedidos) {
            String metodo = pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado";
            metodos.put(metodo, metodos.getOrDefault(metodo, 0) + 1);
            montos.put(metodo, montos.getOrDefault(metodo, 0.0) + pedido.getTotal());
        }

        String[] headers = {"Método de Pago", "Cantidad de Pedidos", "Monto Total"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        int rowNum = 1;
        for (Map.Entry<String, Integer> entry : metodos.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
            row.createCell(2).setCellValue(montos.get(entry.getKey()));
        }
    }

    private void generarExcelTiposEntrega(Workbook workbook, List<Pedido> pedidos) {
        Sheet sheet = workbook.createSheet("Tipos de Entrega");
        CellStyle headerStyle = crearEstiloCabecera(workbook);

        Map<String, Integer> tipos = new HashMap<>();
        Map<String, Double> montos = new HashMap<>();

        for (Pedido pedido : pedidos) {
            String tipo = pedido.getTipoEntrega() != null ? pedido.getTipoEntrega() : "No especificado";
            tipos.put(tipo, tipos.getOrDefault(tipo, 0) + 1);
            montos.put(tipo, montos.getOrDefault(tipo, 0.0) + pedido.getTotal());
        }

        String[] headers = {"Tipo de Entrega", "Cantidad de Pedidos", "Monto Total"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        int rowNum = 1;
        for (Map.Entry<String, Integer> entry : tipos.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
            row.createCell(2).setCellValue(montos.get(entry.getKey()));
        }
    }

    private void generarExcelHorarios(Workbook workbook, List<Pedido> pedidos) {
        Sheet sheet = workbook.createSheet("Horarios Pico");
        CellStyle headerStyle = crearEstiloCabecera(workbook);

        Map<String, Integer> horas = new LinkedHashMap<>();
        for (int i = 0; i < 24; i++) {
            horas.put(String.format("%02d:00 - %02d:00", i, i + 1), 0);
        }

        for (Pedido pedido : pedidos) {
            if (pedido.getFecha() != null) {
                int hora = pedido.getFecha().getHour();
                String rango = String.format("%02d:00 - %02d:00", hora, hora + 1);
                horas.put(rango, horas.getOrDefault(rango, 0) + 1);
            }
        }

        Map<String, Integer> horasConPedidos = horas.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        String[] headers = {"Hora", "Pedidos"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        int rowNum = 1;
        for (Map.Entry<String, Integer> entry : horasConPedidos.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }
    }

    private void generarExcelFavoritos(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Productos Favoritos");
        CellStyle headerStyle = crearEstiloCabecera(workbook);

        List<Favorito> favoritos = favoritoRepository.findAllWithUsuarioAndProducto();

        Map<String, Integer> productosFavoritos = new HashMap<>();
        for (Favorito f : favoritos) {
            if (f.getProducto() != null) {
                String nombre = f.getProducto().getNombre();
                productosFavoritos.put(nombre, productosFavoritos.getOrDefault(nombre, 0) + 1);
            }
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(productosFavoritos.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        String[] headers = {"#", "Producto", "Veces Favorito"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        int rowNum = 1;
        for (Map.Entry<String, Integer> entry : sorted) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(entry.getKey());
            row.createCell(2).setCellValue(entry.getValue());
        }
    }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================

    private String getNombreCliente(Pedido pedido) {
        if (pedido.getUsuario() != null) {
            String nombre = (pedido.getUsuario().getNombres() != null ? pedido.getUsuario().getNombres() : "") + " " +
                    (pedido.getUsuario().getApellidos() != null ? pedido.getUsuario().getApellidos() : "");
            return nombre.trim().isEmpty() ? pedido.getUsuario().getUsername() : nombre.trim();
        }

        return pedido.getNombreCliente() != null && !pedido.getNombreCliente().isEmpty()
                ? pedido.getNombreCliente()
                : "Cliente general";
    }

    private CellStyle crearEstiloCabecera(Workbook workbook) {
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
        return headerStyle;
    }

    // ============================================================
    // MÉTODO AUXILIAR PARA FILTRAR PEDIDOS
    // ============================================================

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
                log.warn(" Error al parsear fechas, usando todos los pedidos");
            }
        }

        return pedidos;
    }
}