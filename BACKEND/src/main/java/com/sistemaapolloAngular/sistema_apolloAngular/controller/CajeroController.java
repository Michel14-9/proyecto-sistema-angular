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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cajero")
public class CajeroController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;


    @GetMapping("")
    public String vistaCajero(Authentication authentication, Model model) {
        try {
            // Obtener información del usuario cajero
            String username = authentication.getName();
            Optional<Usuario> cajeroOpt = usuarioRepository.findByUsername(username);

            if (cajeroOpt.isPresent()) {
                Usuario cajero = cajeroOpt.get();
                model.addAttribute("usuario", cajero);
            }

            // Obtener pedidos PENDIENTES para el cajero
            List<Pedido> pedidosPendientes = pedidoRepository.findByEstadoOrderByFechaDesc("PENDIENTE");
            model.addAttribute("pedidosPendientes", pedidosPendientes);

            // Obtener pedidos PAGADOS de hoy (para métricas)
            List<Pedido> pedidosPagadosHoy = pedidoRepository.findAll().stream()
                    .filter(p -> "PAGADO".equals(p.getEstado()) &&
                            p.getFecha() != null &&
                            p.getFecha().toLocalDate().equals(java.time.LocalDate.now()))
                    .collect(Collectors.toList());

            // Calcular métricas
            double ingresosHoy = pedidosPagadosHoy.stream()
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            model.addAttribute("totalPedidosPendientes", pedidosPendientes.size());
            model.addAttribute("totalPedidosPagadosHoy", pedidosPagadosHoy.size());
            model.addAttribute("ingresosHoy", ingresosHoy);

            return "cajero";

        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la vista del cajero: " + e.getMessage());
            return "cajero";
        }
    }


    @PostMapping("/marcar-pagado/{pedidoId}")
    @ResponseBody
    public ResponseEntity<?> marcarComoPagado(@PathVariable String pedidoId,
                                              Authentication authentication) {
        try {
            System.out.println("=== INICIO marcarComoPagado ===");
            System.out.println(" Usuario: " + (authentication != null ? authentication.getName() : "NULL"));


            if (authentication == null) {
                System.out.println(" ERROR: Authentication es NULL");
                return ResponseEntity.status(401).body("ERROR: No autenticado");
            }

            if (!authentication.isAuthenticated()) {
                System.out.println(" ERROR: Usuario no autenticado");
                return ResponseEntity.status(401).body("ERROR: No autenticado");
            }

            //  Verificar rol CAJERO
            boolean hasCajeroRole = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority ->
                            grantedAuthority.getAuthority().equals("ROLE_CAJERO"));

            System.out.println(" Tiene rol CAJERO: " + hasCajeroRole);

            if (!hasCajeroRole) {
                System.out.println(" ERROR: Usuario sin rol CAJERO. Roles: " +
                        authentication.getAuthorities());
                return ResponseEntity.status(403).body("ERROR: No tiene permisos de cajero");
            }

            //  Convertir ID
            Long pedidoIdLong;
            try {
                pedidoIdLong = Long.parseLong(pedidoId);
                System.out.println("ID convertido: " + pedidoIdLong);
            } catch (NumberFormatException e) {
                System.out.println(" ERROR: ID inválido");
                return ResponseEntity.badRequest().body("ERROR: ID de pedido inválido");
            }

            //  Buscar pedido
            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoIdLong);
            if (!pedidoOpt.isPresent()) {
                System.out.println(" ERROR: Pedido no encontrado");
                return ResponseEntity.badRequest().body("ERROR: Pedido no encontrado");
            }

            Pedido pedido = pedidoOpt.get();
            System.out.println(" Estado actual del pedido: " + pedido.getEstado());

            //  Validar estado
            if (!"PENDIENTE".equals(pedido.getEstado())) {
                System.out.println(" ERROR: Pedido no está PENDIENTE");
                return ResponseEntity.badRequest()
                        .body("ERROR: El pedido no está en estado PENDIENTE. Estado actual: " + pedido.getEstado());
            }


            String username = authentication.getName();
            Optional<Usuario> cajeroOpt = usuarioRepository.findByUsername(username);
            String nombreCajero = "Cajero"; // Valor por defecto

            if (cajeroOpt.isPresent()) {
                Usuario cajero = cajeroOpt.get();
                // Usar nombre completo o solo nombres si está disponible
                if (cajero.getNombres() != null && cajero.getApellidos() != null) {
                    nombreCajero = cajero.getNombres() + " " + cajero.getApellidos();
                } else if (cajero.getNombres() != null) {
                    nombreCajero = cajero.getNombres();
                } else {
                    nombreCajero = cajero.getUsername(); // Fallback al username
                }
                System.out.println("👤 Cajero que atiende: " + nombreCajero);
            }

            //  Actualizar estado
            pedido.setEstado("PAGADO");
            Pedido pedidoGuardado = pedidoRepository.save(pedido);

            //  GENERAR BOLETA AUTOMÁTICAMENTE CON NOMBRE DEL CAJERO
            String boletaPath = generarBoletaPDF(pedidoGuardado, nombreCajero);

            System.out.println(" ÉXITO: Pedido " + pedidoId + " marcado como PAGADO");
            System.out.println(" Boleta generada: " + boletaPath);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Pedido marcado como PAGADO y boleta generada");
            response.put("boletaPath", boletaPath);
            response.put("numeroPedido", pedidoGuardado.getNumeroPedido());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println(" EXCEPCIÓN en marcarComoPagado: " + e.getMessage());
            e.printStackTrace();

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Error: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    private String generarBoletaPDF(Pedido pedido, String nombreCajero) {
        // Usar directorio en raíz del proyecto
        String directoryPath = "boletas/";
        String fileName = "boleta_" + pedido.getNumeroPedido() + ".pdf";

        try {
            // Crear directorio si no existe
            Path directory = Paths.get(directoryPath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            String fullPath = directoryPath + fileName;

            // Crear PDF
            PdfWriter writer = new PdfWriter(new FileOutputStream(fullPath));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Configurar página
            document.setMargins(20, 20, 20, 20);

            // Título principal
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

            document.add(new Paragraph(" ")); // Espacio

            // Línea separadora
            document.add(new Paragraph("_________________________________________")
                    .setTextAlignment(TextAlignment.CENTER));

            // Información del pedido
            document.add(new Paragraph(" "));
            document.add(new Paragraph("N° BOLETA: " + pedido.getNumeroPedido()).setBold());
            document.add(new Paragraph("FECHA: " +
                    pedido.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
            document.add(new Paragraph("CAJERO: " + nombreCajero));

            // Información del cliente
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

            // Tipo de entrega
            document.add(new Paragraph("TIPO DE ENTREGA: " +
                    (pedido.getTipoEntrega() != null ? pedido.getTipoEntrega() : "NO ESPECIFICADO")));

            if ("DELIVERY".equals(pedido.getTipoEntrega()) &&
                    pedido.getDireccionEntrega() != null) {
                document.add(new Paragraph("DIRECCIÓN: " + pedido.getDireccionEntrega()));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("DETALLE DEL PEDIDO:").setBold());
            document.add(new Paragraph("_________________________________________"));

            // Tabla de productos
            float[] columnWidths = {3, 1, 2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setMarginTop(10);
            table.setMarginBottom(10);

            // Encabezados de tabla
            table.addHeaderCell(new Paragraph("PRODUCTO").setBold());
            table.addHeaderCell(new Paragraph("CANT.").setBold());
            table.addHeaderCell(new Paragraph("P. UNIT.").setBold());
            table.addHeaderCell(new Paragraph("SUBTOTAL").setBold());

            // Items del pedido
            if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
                for (ItemPedido item : pedido.getItems()) {
                    table.addCell(new Paragraph(item.getNombreProducto() != null ?
                            item.getNombreProducto() : "Producto"));
                    table.addCell(new Paragraph(String.valueOf(item.getCantidad())));
                    table.addCell(new Paragraph("S/ " + String.format("%.2f", item.getPrecio())));
                    table.addCell(new Paragraph("S/ " + String.format("%.2f", item.getSubtotal())));
                }
            } else {

                table.addCell(new Paragraph("No hay items en este pedido"));
                table.addCell(new Paragraph("")); // Celda vacía
                table.addCell(new Paragraph("")); // Celda vacía
                table.addCell(new Paragraph("")); // Celda vacía
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // Línea separadora
            document.add(new Paragraph("_________________________________________"));

            // Totales
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

            // Pie de página
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

            System.out.println(" PDF generado exitosamente: " + fullPath);
            return fileName; // Solo el nombre del archivo

        } catch (Exception e) {
            System.err.println("Error generando boleta PDF: " + e.getMessage());
            e.printStackTrace();
            return "error_generacion";
        }
    }


    @GetMapping("/boletas/{filename:.+}")
    public ResponseEntity<Resource> servirBoleta(@PathVariable String filename) {
        try {
            System.out.println(" Solicitando archivo: " + filename);

            Path filePath = Paths.get("boletas/" + filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                System.out.println(" Archivo encontrado, sirviendo: " + filename);

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                System.out.println(" Archivo no encontrado: " + filePath.toString());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println(" Error sirviendo archivo: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/metricas-hoy")
    @ResponseBody
    public Map<String, Object> obtenerMetricasHoy() {
        Map<String, Object> metricas = new HashMap<>();

        try {
            // Pedidos PENDIENTES
            List<Pedido> pedidosPendientes = pedidoRepository.findByEstadoOrderByFechaDesc("PENDIENTE");

            // Pedidos PAGADOS hoy
            LocalDate hoy = java.time.LocalDate.now();
            List<Pedido> pedidosPagadosHoy = pedidoRepository.findAll().stream()
                    .filter(p -> "PAGADO".equals(p.getEstado()) &&
                            p.getFecha() != null &&
                            p.getFecha().toLocalDate().equals(hoy))
                    .collect(Collectors.toList());

            // Calcular ingresos
            double ingresosHoy = pedidosPagadosHoy.stream()
                    .mapToDouble(Pedido::getTotal)
                    .sum();

            metricas.put("totalPedidosPendientes", pedidosPendientes.size());
            metricas.put("totalPedidosPagadosHoy", pedidosPagadosHoy.size());
            metricas.put("ingresosHoy", ingresosHoy);
            metricas.put("success", true);

        } catch (Exception e) {
            metricas.put("success", false);
            metricas.put("error", e.getMessage());
        }

        return metricas;
    }


    @PostMapping("/marcar-cancelado/{pedidoId}")
    @ResponseBody
    public ResponseEntity<?> marcarComoCancelado(@PathVariable String pedidoId,
                                                 @RequestParam(required = false) String motivo,
                                                 Authentication authentication) {
        try {
            //   Verificar que el usuario está autenticado
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("ERROR: Sesión expirada. Por favor, inicie sesión nuevamente.");
            }


            Long pedidoIdLong;
            try {
                pedidoIdLong = Long.parseLong(pedidoId);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("ERROR: ID de pedido inválido: " + pedidoId);
            }

            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoIdLong);

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();

                // Validar que el pedido esté en estado PENDIENTE
                if (!"PENDIENTE".equals(pedido.getEstado())) {
                    return ResponseEntity.badRequest().body("ERROR: Solo se pueden cancelar pedidos PENDIENTES. Estado actual: " + pedido.getEstado());
                }

                // Cambiar estado a CANCELADO
                pedido.setEstado("CANCELADO");
                if (motivo != null && !motivo.trim().isEmpty()) {
                    pedido.setObservaciones("CANCELADO - Motivo: " + motivo);
                }
                pedidoRepository.save(pedido);

                System.out.println(" Pedido " + pedidoId + " cancelado por " + authentication.getName());

                Map<String, String> response = new HashMap<>();
                response.put("status", "SUCCESS");
                response.put("message", "Pedido cancelado exitosamente");

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("ERROR: Pedido no encontrado con ID: " + pedidoId);
            }

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Error: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }


    @GetMapping("/pedidos-pendientes")
    @ResponseBody
    public ResponseEntity<?> obtenerPedidosPendientes() {
        try {
            List<Pedido> pedidos = pedidoRepository.findByEstadoOrderByFechaDesc("PENDIENTE");

            // Crear DTOs para evitar problemas de serialización
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
                pedidoDTO.put("direccionEntrega", pedido.getDireccionEntrega());
                pedidoDTO.put("observaciones", pedido.getObservaciones());

                // Información del cliente (manejar proxy Hibernate)
                if (pedido.getUsuario() != null) {
                    Map<String, String> usuarioDTO = new HashMap<>();
                    usuarioDTO.put("nombres", pedido.getUsuario().getNombres());
                    usuarioDTO.put("apellidos", pedido.getUsuario().getApellidos());
                    usuarioDTO.put("telefono", pedido.getUsuario().getTelefono());
                    pedidoDTO.put("cliente", usuarioDTO);
                } else {
                    pedidoDTO.put("cliente", null);
                }

                // Información de items del pedido
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

            return ResponseEntity.ok(pedidosDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al cargar pedidos: " + e.getMessage()
            ));
        }
    }


    @GetMapping("/pedido/{pedidoId}")
    public String gestionarPedido(@PathVariable Long pedidoId, Model model, Authentication authentication) {
        try {
            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();
                model.addAttribute("pedido", pedido);

                // Obtener información del cajero
                String username = authentication.getName();
                Optional<Usuario> cajeroOpt = usuarioRepository.findByUsername(username);
                cajeroOpt.ifPresent(usuario -> model.addAttribute("usuario", usuario));

                return "cajero-pedido"; // Vista específica para gestionar un pedido
            } else {
                return "redirect:/cajero?error=Pedido no encontrado";
            }

        } catch (Exception e) {
            return "redirect:/cajero?error=" + e.getMessage();
        }
    }
}
