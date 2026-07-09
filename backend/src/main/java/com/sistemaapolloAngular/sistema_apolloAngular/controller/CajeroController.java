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
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.UsuarioRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.service.PedidoService; // ✅ NUEVO
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
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
    private PedidoService pedidoService; // ✅ NUEVO


    @GetMapping("/pedidos-pendientes")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerPedidosPendientes() {
        try {

            List<Pedido> pedidos = pedidoRepository.findAllWithItemsAndProducts()
                    .stream()
                    .filter(p -> "PENDIENTE".equals(p.getEstado()))
                    .filter(p -> !"WEB".equals(p.getCanal())) // ✅ excluye pedidos web (los confirma MercadoPago solo)
                    .collect(Collectors.toList());

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
                pedidoDTO.put("canal", pedido.getCanal()); // ✅ útil para el frontend distinguir

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

            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al cargar pedidos: " + e.getMessage()
            ));
        }
    }

    // ✅ NUEVO: CREAR PEDIDO PRESENCIAL (cliente dicta su pedido en mostrador)
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

            Pedido pedido = pedidoService.crearPedidoPresencial(
                    itemsRequest, metodoPago, tipoEntrega, nombreCliente, telefonoCliente, observaciones);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Pedido presencial creado correctamente");
            response.put("pedidoId", pedido.getId());
            response.put("numeroPedido", pedido.getNumeroPedido());
            response.put("total", pedido.getTotal());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    // ✅ OBTENER MÉTRICAS DEL DÍA - Con @Transactional
    @GetMapping("/metricas-hoy")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> obtenerMetricasHoy() {
        try {
            List<Pedido> pedidosPendientes = pedidoRepository.findByEstadoOrderByFechaDesc("PENDIENTE")
                    .stream()
                    .filter(p -> !"WEB".equals(p.getCanal())) // ✅ consistente con la bandeja
                    .collect(Collectors.toList());

            LocalDate hoy = LocalDate.now();
            LocalDateTime hoyInicio = hoy.atStartOfDay();
            LocalDateTime hoyFin = hoy.atTime(23, 59, 59);

            List<Pedido> pedidosPagadosHoy = pedidoRepository.findAll().stream()
                    .filter(p -> "PAGADO".equals(p.getEstado()) &&
                            p.getFecha() != null &&
                            !p.getFecha().isBefore(hoyInicio) &&
                            !p.getFecha().isAfter(hoyFin))
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

    // ✅ MARCAR PEDIDO COMO PAGADO - Con @Transactional
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

            if (!"PENDIENTE".equals(pedido.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "El pedido no está en estado PENDIENTE. Estado actual: " + pedido.getEstado()
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

            pedido.setEstado("PAGADO");
            pedido.setFechaActualizacion(LocalDateTime.now());
            Pedido pedidoGuardado = pedidoRepository.save(pedido);

            String boletaPath = generarBoletaPDF(pedidoGuardado, nombreCajero);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Pedido marcado como PAGADO y boleta generada");
            response.put("boletaPath", boletaPath);
            response.put("numeroPedido", pedidoGuardado.getNumeroPedido());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    // ✅ GENERAR BOLETA PDF
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

            if (pedido.getUsuario() != null) {
                String nombreCliente = pedido.getUsuario().getNombres() + " " +
                        pedido.getUsuario().getApellidos();
                document.add(new Paragraph("CLIENTE: " + nombreCliente.trim()));

                if (pedido.getUsuario().getTelefono() != null &&
                        !pedido.getUsuario().getTelefono().isEmpty()) {
                    document.add(new Paragraph("TELÉFONO: " + pedido.getUsuario().getTelefono()));
                }
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("TIPO DE ENTREGA: " +
                    (pedido.getTipoEntrega() != null ? pedido.getTipoEntrega() : "NO ESPECIFICADO")));

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
                    table.addCell(new Paragraph(item.getNombreProducto() != null ?
                            item.getNombreProducto() : "Producto"));
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

    // ✅ SERVIR BOLETA
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

    // ✅ MARCAR PEDIDO COMO CANCELADO - Con @Transactional
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

            if (pedidoOpt.isPresent()) {
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

                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "Pedido cancelado exitosamente"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "Pedido no encontrado con ID: " + pedidoId
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Error: " + e.getMessage()
            ));
        }
    }
}