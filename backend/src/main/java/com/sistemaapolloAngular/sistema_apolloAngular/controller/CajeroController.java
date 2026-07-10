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
import com.sistemaapolloAngular.sistema_apolloAngular.repository.ProductoFinalRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.service.PedidoService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cajero")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class CajeroController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ProductoFinalRepository productoFinalRepository; // ✅ Agregar

    /**
     * ✅ OBTENER PRODUCTOS PARA PEDIDOS PRESENCIALES
     */
    @GetMapping("/productos")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerProductos() {
        try {
            List<ProductoFinal> productos = productoFinalRepository.findAll();

            List<Map<String, Object>> productosDTO = new ArrayList<>();
            for (ProductoFinal producto : productos) {
                // Solo productos activos
                if (!producto.isActivo()) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>();
                item.put("id", producto.getId());
                item.put("nombre", producto.getNombre());
                item.put("precio", producto.getPrecio());
                item.put("tipo", producto.getTipo());
                productosDTO.add(item);
            }

            System.out.println("📦 Productos disponibles para cajero: " + productosDTO.size());

            return ResponseEntity.ok(productosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cargar productos: " + e.getMessage()));
        }
    }

    /**
     * ✅ OBTENER PEDIDOS PENDIENTES (PRESENCIALES)
     * Solo pedidos de CAJA/PRESENCIAL en estado PENDIENTE
     * Los pedidos WEB no aparecen aquí porque los confirma MercadoPago
     */
    @GetMapping("/pedidos-pendientes")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerPedidosPendientes() {
        try {
            // ✅ Solo pedidos PENDIENTES que son PRESENCIALES (CAJA)
            List<Pedido> pedidos = pedidoRepository.findAllWithItemsAndProducts()
                    .stream()
                    .filter(p -> "PENDIENTE".equals(p.getEstado()))
                    .filter(p -> "CAJA".equals(p.getCanal()) || "PRESENCIAL".equals(p.getCanal()))
                    .filter(p -> "PRESENCIAL".equals(p.getOrigen()))
                    .collect(Collectors.toList());

            System.out.println("💰 Pedidos presenciales pendientes: " + pedidos.size());

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
                pedidoDTO.put("canal", pedido.getCanal());
                pedidoDTO.put("origen", pedido.getOrigen());

                // ✅ Datos del cliente (si tiene usuario o es presencial)
                if (pedido.getUsuario() != null) {
                    Usuario usuario = pedido.getUsuario();
                    Map<String, String> usuarioDTO = new HashMap<>();
                    usuarioDTO.put("nombres", usuario.getNombres() != null ? usuario.getNombres() : "");
                    usuarioDTO.put("apellidos", usuario.getApellidos() != null ? usuario.getApellidos() : "");
                    usuarioDTO.put("telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "");
                    pedidoDTO.put("cliente", usuarioDTO);
                } else {
                    // ✅ Cliente presencial sin usuario
                    Map<String, String> clienteDTO = new HashMap<>();
                    clienteDTO.put("nombres", pedido.getNombreCliente() != null ? pedido.getNombreCliente() : "Cliente");
                    clienteDTO.put("apellidos", "");
                    clienteDTO.put("telefono", pedido.getTelefonoCliente() != null ? pedido.getTelefonoCliente() : "");
                    pedidoDTO.put("cliente", clienteDTO);
                }

                List<Map<String, Object>> itemsDTO = new ArrayList<>();
                if (pedido.getItems() != null) {
                    for (ItemPedido item : pedido.getItems()) {
                        Map<String, Object> itemDTO = new HashMap<>();
                        itemDTO.put("nombreProducto", item.getNombreProductoSeguro());
                        itemDTO.put("cantidad", item.getCantidad());
                        itemDTO.put("precio", item.getPrecio());
                        itemDTO.put("subtotal", item.getSubtotal());
                        itemsDTO.add(itemDTO);
                    }
                }
                pedidoDTO.put("items", itemsDTO);

                pedidosDTO.add(pedidoDTO);
            }

            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al cargar pedidos: " + e.getMessage()
            ));
        }
    }

    /**
     * ✅ CREAR PEDIDO PRESENCIAL
     * Cajero registra el pedido que el cliente dicta en mostrador
     */
    @PostMapping("/crear-pedido-presencial")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> crearPedidoPresencial(@RequestBody Map<String, Object> request,
                                                   Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                        "status", "ERROR",
                        "message", "No autenticado"
                ));
            }

            boolean hasCajeroRole = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority ->
                            grantedAuthority.getAuthority().equals("ROLE_CAJERO"));

            if (!hasCajeroRole) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "ERROR",
                        "message", "No tiene permisos de cajero"
                ));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsRequest = (List<Map<String, Object>>) request.get("items");

            if (itemsRequest == null || itemsRequest.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "El pedido debe tener al menos un producto"
                ));
            }

            String metodoPago = (String) request.getOrDefault("metodoPago", "EFECTIVO");
            String tipoEntrega = (String) request.getOrDefault("tipoEntrega", "LOCAL");
            String nombreCliente = (String) request.get("nombreCliente");
            String telefonoCliente = (String) request.get("telefonoCliente");
            String observaciones = (String) request.get("observaciones");

            // ✅ Crear pedido presencial (estado PENDIENTE, canal CAJA, origen PRESENCIAL)
            Pedido pedido = pedidoService.crearPedidoPresencial(
                    itemsRequest, metodoPago, tipoEntrega, nombreCliente, telefonoCliente, observaciones);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Pedido presencial creado correctamente");
            response.put("pedidoId", pedido.getId());
            response.put("numeroPedido", pedido.getNumeroPedido());
            response.put("total", pedido.getTotal());
            response.put("estado", pedido.getEstado());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * ✅ MÉTRICAS DEL DÍA
     */
    @GetMapping("/metricas-hoy")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerMetricasHoy() {
        try {
            // ✅ Pedidos presenciales pendientes
            List<Pedido> pedidosPendientes = pedidoRepository.findAllWithItemsAndProducts()
                    .stream()
                    .filter(p -> "PENDIENTE".equals(p.getEstado()))
                    .filter(p -> "CAJA".equals(p.getCanal()) || "PRESENCIAL".equals(p.getCanal()))
                    .filter(p -> "PRESENCIAL".equals(p.getOrigen()))
                    .collect(Collectors.toList());

            LocalDate hoy = LocalDate.now();
            LocalDateTime hoyInicio = hoy.atStartOfDay();
            LocalDateTime hoyFin = hoy.atTime(23, 59, 59);

            // ✅ Pedidos pagados hoy (presenciales y web)
            List<Pedido> pedidosPagadosHoy = pedidoRepository.findAllWithItemsAndProducts()
                    .stream()
                    .filter(p -> "PAGADO".equals(p.getEstado()))
                    .filter(p -> p.getFecha() != null)
                    .filter(p -> !p.getFecha().isBefore(hoyInicio) && !p.getFecha().isAfter(hoyFin))
                    .collect(Collectors.toList());

            double ingresosHoy = pedidosPagadosHoy.stream()
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            Map<String, Object> metricas = new HashMap<>();
            metricas.put("success", true);
            metricas.put("totalPedidosPendientes", pedidosPendientes.size());
            metricas.put("totalPedidosPagadosHoy", pedidosPagadosHoy.size());
            metricas.put("ingresosHoy", ingresosHoy);

            return ResponseEntity.ok(metricas);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * ✅ MARCAR PEDIDO COMO PAGADO
     * El cajero cobra en efectivo/tarjeta y el pedido queda PAGADO
     * ❌ NO va a cocina (es presencial, cliente retira en local)
     */
    @PostMapping("/marcar-pagado/{pedidoId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> marcarComoPagado(@PathVariable String pedidoId,
                                              Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                        "status", "ERROR",
                        "message", "No autenticado"
                ));
            }

            boolean hasCajeroRole = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority ->
                            grantedAuthority.getAuthority().equals("ROLE_CAJERO"));

            if (!hasCajeroRole) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "ERROR",
                        "message", "No tiene permisos de cajero"
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
            if (pedidoOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "Pedido no encontrado"
                ));
            }

            Pedido pedido = pedidoOpt.get();

            // ✅ Validar que sea un pedido presencial pendiente
            if (!"PENDIENTE".equals(pedido.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "El pedido no está en estado PENDIENTE. Estado actual: " + pedido.getEstado()
                ));
            }

            // ✅ Validar que sea presencial (no web)
            if ("WEB".equals(pedido.getCanal()) || "ONLINE".equals(pedido.getOrigen())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "Este es un pedido WEB, no se puede marcar como pagado desde caja"
                ));
            }

            String username = authentication.getName();
            Optional<Usuario> cajeroOpt = usuarioRepository.findByUsername(username);
            String nombreCajero = "Cajero";

            if (cajeroOpt.isPresent()) {
                Usuario cajero = cajeroOpt.get();
                if (cajero.getNombres() != null && cajero.getApellidos() != null) {
                    nombreCajero = cajero.getNombres() + " " + cajero.getApellidos();
                } else if (cajero.getNombres() != null) {
                    nombreCajero = cajero.getNombres();
                } else {
                    nombreCajero = cajero.getUsername();
                }
            }

            // ✅ Marcar como PAGADO
            pedido.setEstado("PAGADO");
            pedido.setFechaActualizacion(LocalDateTime.now());
            pedido.setListoParaCocina(false); // ✅ NO va a cocina (presencial)

            // Guardar método de pago si no tiene
            if (pedido.getMetodoPago() == null) {
                pedido.setMetodoPago("EFECTIVO");
            }

            Pedido pedidoGuardado = pedidoRepository.save(pedido);

            // ✅ Generar boleta PDF
            String boletaPath = generarBoletaPDF(pedidoGuardado, nombreCajero);

            System.out.println("💰 PEDIDO PAGADO EN CAJA: " + pedidoGuardado.getNumeroPedido());
            System.out.println("   - Canal: " + pedidoGuardado.getCanal());
            System.out.println("   - Origen: " + pedidoGuardado.getOrigen());
            System.out.println("   - Cliente: " + (pedidoGuardado.getNombreCliente() != null ?
                    pedidoGuardado.getNombreCliente() : "No especificado"));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Pedido marcado como PAGADO y boleta generada");
            response.put("boletaPath", boletaPath);
            response.put("numeroPedido", pedidoGuardado.getNumeroPedido());
            response.put("total", pedidoGuardado.getTotal());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * ✅ MARCAR PEDIDO COMO CANCELADO
     */
    @PostMapping("/marcar-cancelado/{pedidoId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> marcarComoCancelado(@PathVariable String pedidoId,
                                                 @RequestParam(required = false) String motivo,
                                                 Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                        "status", "ERROR",
                        "message", "Sesión expirada. Por favor, inicie sesión nuevamente."
                ));
            }

            boolean hasCajeroRole = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority ->
                            grantedAuthority.getAuthority().equals("ROLE_CAJERO"));

            if (!hasCajeroRole) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "ERROR",
                        "message", "No tiene permisos de cajero"
                ));
            }

            Long pedidoIdLong;
            try {
                pedidoIdLong = Long.parseLong(pedidoId);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "ID de pedido inválido: " + pedidoId
                ));
            }

            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoIdLong);

            if (pedidoOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "Pedido no encontrado con ID: " + pedidoId
                ));
            }

            Pedido pedido = pedidoOpt.get();

            if (!"PENDIENTE".equals(pedido.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "Solo se pueden cancelar pedidos PENDIENTES. Estado actual: " + pedido.getEstado()
                ));
            }

            pedido.setEstado("CANCELADO");
            pedido.setFechaActualizacion(LocalDateTime.now());
            if (motivo != null && !motivo.trim().isEmpty()) {
                pedido.setObservaciones("CANCELADO - Motivo: " + motivo);
            }
            pedidoRepository.save(pedido);

            System.out.println("🚫 PEDIDO CANCELADO: " + pedido.getNumeroPedido());

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Pedido cancelado exitosamente"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * ✅ GENERAR BOLETA PDF
     */
    private String generarBoletaPDF(Pedido pedido, String nombreCajero) {
        String directoryPath = "boletas/";
        String fileName = "boleta_" + pedido.getNumeroPedido() + ".pdf";

        try {
            Path directory = Paths.get(directoryPath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            String fullPath = directoryPath + fileName;

            PdfWriter writer = new PdfWriter(new FileOutputStream(fullPath));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            document.setMargins(20, 20, 20, 20);

            Paragraph titulo = new Paragraph("BOLETA DE VENTA")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(18);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("LUREN CHICKEN")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(14);
            document.add(subtitulo);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("_________________________________________")
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("N° BOLETA: " + pedido.getNumeroPedido()).setBold());
            document.add(new Paragraph("FECHA: " +
                    pedido.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
            document.add(new Paragraph("CAJERO: " + nombreCajero));

            // ✅ Cliente (presencial o usuario)
            if (pedido.getUsuario() != null) {
                String nombreCliente = pedido.getUsuario().getNombres() + " " +
                        pedido.getUsuario().getApellidos();
                document.add(new Paragraph("CLIENTE: " + nombreCliente.trim()));

                if (pedido.getUsuario().getTelefono() != null &&
                        !pedido.getUsuario().getTelefono().isEmpty()) {
                    document.add(new Paragraph("TELÉFONO: " + pedido.getUsuario().getTelefono()));
                }
            } else if (pedido.getNombreCliente() != null) {
                document.add(new Paragraph("CLIENTE: " + pedido.getNombreCliente()));
                if (pedido.getTelefonoCliente() != null && !pedido.getTelefonoCliente().isEmpty()) {
                    document.add(new Paragraph("TELÉFONO: " + pedido.getTelefonoCliente()));
                }
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("TIPO DE ENTREGA: " +
                    (pedido.getTipoEntrega() != null ? pedido.getTipoEntrega() : "LOCAL")));

            if ("DELIVERY".equals(pedido.getTipoEntrega()) &&
                    pedido.getDireccionEntrega() != null) {
                document.add(new Paragraph("DIRECCIÓN: " + pedido.getDireccionEntrega()));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("DETALLE DEL PEDIDO:").setBold());
            document.add(new Paragraph("_________________________________________"));

            float[] columnWidths = {3, 1, 2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginTop(10);
            table.setMarginBottom(10);

            table.addHeaderCell(new Paragraph("PRODUCTO").setBold());
            table.addHeaderCell(new Paragraph("CANT.").setBold());
            table.addHeaderCell(new Paragraph("P. UNIT.").setBold());
            table.addHeaderCell(new Paragraph("SUBTOTAL").setBold());

            if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
                for (ItemPedido item : pedido.getItems()) {
                    table.addCell(new Paragraph(item.getNombreProductoSeguro()));
                    table.addCell(new Paragraph(String.valueOf(item.getCantidad())));
                    table.addCell(new Paragraph("S/ " + String.format("%.2f", item.getPrecio())));
                    table.addCell(new Paragraph("S/ " + String.format("%.2f", item.getSubtotal())));
                }
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("_________________________________________"));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("SUBTOTAL: S/ " + String.format("%.2f", pedido.getSubtotal())));

            if (pedido.getCostoEnvio() != null && pedido.getCostoEnvio() > 0) {
                document.add(new Paragraph("COSTO ENVÍO: S/ " + String.format("%.2f", pedido.getCostoEnvio())));
            }

            if (pedido.getDescuento() != null && pedido.getDescuento() > 0) {
                document.add(new Paragraph("DESCUENTO: -S/ " + String.format("%.2f", pedido.getDescuento())));
            }

            document.add(new Paragraph("TOTAL: S/ " + String.format("%.2f", pedido.getTotal()))
                    .setBold()
                    .setFontSize(14));

            document.add(new Paragraph(" "));

            if (pedido.getMetodoPago() != null) {
                document.add(new Paragraph("MÉTODO DE PAGO: " + pedido.getMetodoPago()));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("_________________________________________"));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("¡Gracias por su compra!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic());

            document.add(new Paragraph("Luren Chicken - Calle Principal 123, Lima")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10));

            document.add(new Paragraph("Tel: 123-456-789")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10));

            document.close();

            return fileName;

        } catch (Exception e) {
            e.printStackTrace();
            return "error_generacion";
        }
    }

    /**
     * ✅ SERVIR BOLETA
     */
    @GetMapping("/boletas/{filename:.+}")
    public ResponseEntity<Resource> servirBoleta(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("boletas/" + filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}