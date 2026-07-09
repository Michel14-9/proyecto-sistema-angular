package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.colors.ColorConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/sigue-tu-pedido")
public class SigueTuPedidoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    /**
     * Busca un pedido por número de pedido
     */
    @GetMapping("/buscar")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> buscarPedido(
            @RequestParam("numeroPedido") String numeroPedido,
            @RequestParam(value = "canalPedido", required = false, defaultValue = "WEB") String canalPedido) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== DEBUG SIGUE TU PEDIDO API ===");
            System.out.println("Buscando: " + numeroPedido);
            System.out.println("Canal: " + canalPedido);

            String numeroLimpio = numeroPedido.trim().toUpperCase();

            Optional<Pedido> pedidoOpt = pedidoRepository.findByNumeroPedidoWithUsuarioAndItems(numeroLimpio);

            if (!pedidoOpt.isPresent()) {
                pedidoOpt = pedidoRepository.findByNumeroWithUsuarioAndItems(numeroLimpio);
            }

            System.out.println("Encontrado: " + pedidoOpt.isPresent());

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();
                System.out.println("ID Pedido: " + pedido.getId());
                System.out.println("Número: " + pedido.getNumero());
                System.out.println("Número Pedido: " + pedido.getNumeroPedido());
                System.out.println("Estado: " + pedido.getEstado());

                if (canalPedido != null && !canalPedido.isEmpty() &&
                        pedido.getCanal() != null &&
                        !canalPedido.equals(pedido.getCanal())) {
                    response.put("success", false);
                    response.put("message", "El pedido " + numeroLimpio +
                            " no fue realizado a través de " +
                            (canalPedido.equals("WEB") ? "Página Web" : canalPedido));
                    response.put("pedido", null);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }

                Map<String, Object> pedidoData = new HashMap<>();
                pedidoData.put("id", pedido.getId());
                pedidoData.put("numeroPedido", pedido.getNumeroPedido());
                pedidoData.put("numero", pedido.getNumero());
                pedidoData.put("estado", pedido.getEstado());
                pedidoData.put("fecha", pedido.getFecha());
                pedidoData.put("fechaPedido", pedido.getFechaPedido());
                pedidoData.put("total", pedido.getTotal());
                pedidoData.put("subtotal", pedido.getSubtotal());
                pedidoData.put("costoEnvio", pedido.getCostoEnvio());
                pedidoData.put("descuento", pedido.getDescuento());
                pedidoData.put("metodoPago", pedido.getMetodoPago());
                pedidoData.put("tipoEntrega", pedido.getTipoEntrega());
                pedidoData.put("direccionEntrega", pedido.getDireccionEntrega());
                pedidoData.put("observaciones", pedido.getObservaciones());
                pedidoData.put("instrucciones", pedido.getInstrucciones());
                pedidoData.put("canal", pedido.getCanal());

                if (pedido.getUsuario() != null) {
                    Map<String, String> usuarioData = new HashMap<>();
                    usuarioData.put("nombres", pedido.getUsuario().getNombres());
                    usuarioData.put("apellidos", pedido.getUsuario().getApellidos());
                    usuarioData.put("telefono", pedido.getUsuario().getTelefono());
                    usuarioData.put("email", pedido.getUsuario().getUsername());
                    pedidoData.put("usuario", usuarioData);
                }

                if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
                    List<Map<String, Object>> itemsData = new ArrayList<>();
                    for (ItemPedido item : pedido.getItems()) {
                        Map<String, Object> itemData = new HashMap<>();
                        itemData.put("id", item.getId());
                        itemData.put("nombreProducto", item.getNombreProductoSeguro());
                        itemData.put("cantidad", item.getCantidad());
                        itemData.put("precio", item.getPrecio());
                        itemData.put("subtotal", item.getSubtotal());
                        itemsData.add(itemData);
                    }
                    pedidoData.put("items", itemsData);
                }

                response.put("success", true);
                response.put("message", "Pedido encontrado exitosamente");
                response.put("pedido", pedidoData);

                return ResponseEntity.ok(response);

            } else {
                response.put("success", false);
                response.put("message", "No se encontró ningún pedido con el número: " + numeroLimpio);
                response.put("pedido", null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            System.err.println("Error en buscarPedido: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error al buscar el pedido: " + e.getMessage());
            response.put("pedido", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtiene el estado de un pedido por número
     */
    @GetMapping("/estado")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> obtenerEstadoPedido(
            @RequestParam("numeroPedido") String numeroPedido) {

        Map<String, Object> response = new HashMap<>();

        try {
            String numeroLimpio = numeroPedido.trim().toUpperCase();
            Optional<Pedido> pedidoOpt = pedidoRepository.findByNumeroPedido(numeroLimpio);

            if (!pedidoOpt.isPresent()) {
                pedidoOpt = pedidoRepository.findByNumero(numeroLimpio);
            }

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();
                response.put("success", true);
                response.put("estado", pedido.getEstado());
                response.put("numeroPedido", pedido.getNumeroPedido());
                response.put("fecha", pedido.getFecha());
                response.put("total", pedido.getTotal());

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Pedido no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener estado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ✅ GENERA COMPROBANTE EN PDF CON iText
     */
    @GetMapping("/{pedidoId}/comprobante")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generarComprobantePdf(@PathVariable Long pedidoId) {
        try {
            Optional<Pedido> pedidoOpt = pedidoRepository.findByIdWithItems(pedidoId);

            if (pedidoOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Pedido pedido = pedidoOpt.get();

            // Crear PDF en memoria
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // ========== HEADER ==========
            // Título principal
            Paragraph titulo = new Paragraph("🍗 Luren Chicken")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.ORANGE);
            document.add(titulo);

            document.add(new Paragraph(" "));

            Paragraph subtitulo = new Paragraph("COMPROBANTE DE PAGO")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(subtitulo);

            document.add(new Paragraph(" "));

            // Número de pedido
            Paragraph numeroPedidoP = new Paragraph("N° " + pedido.getNumeroPedido())
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(numeroPedidoP);

            document.add(new Paragraph(" "));

            // ========== INFORMACIÓN DEL PEDIDO ==========
            // Estado con icono
            String estadoTexto = pedido.getEstado();
            String estadoEmoji = estadoTexto.equals("PAGADO") ? "✅ " :
                    estadoTexto.equals("ENTREGADO") ? "📦 " :
                            estadoTexto.equals("PENDIENTE") ? "⏳ " :
                                    estadoTexto.equals("RECHAZADO") ? "❌ " :
                                            estadoTexto.equals("LISTO") ? "📋 " :
                                                    estadoTexto.equals("EN_CAMINO") ? "🚚 " : "";

            document.add(new Paragraph("Estado: " + estadoEmoji + estadoTexto)
                    .setFontSize(12));

            // Fecha
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String fechaFormateada = pedido.getFechaPedido() != null ?
                    pedido.getFechaPedido().format(formatter) : "No especificada";
            document.add(new Paragraph("Fecha: " + fechaFormateada)
                    .setFontSize(12));

            // Total
            document.add(new Paragraph("Total: S/ " + String.format("%.2f", pedido.getTotal()))
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(ColorConstants.GREEN));

            // Método de pago
            document.add(new Paragraph("Método de Pago: " + (pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "No especificado"))
                    .setFontSize(12));

            // Tipo de entrega
            document.add(new Paragraph("Tipo de Entrega: " + (pedido.getTipoEntrega() != null ? pedido.getTipoEntrega() : "No especificado"))
                    .setFontSize(12));

            // Dirección
            if (pedido.getDireccionEntrega() != null && !pedido.getDireccionEntrega().isEmpty()) {
                document.add(new Paragraph("Dirección: " + pedido.getDireccionEntrega())
                        .setFontSize(12));
            }

            document.add(new Paragraph(" "));

            // ========== TABLA DE PRODUCTOS ==========
            document.add(new Paragraph("📋 Productos")
                    .setFontSize(14)
                    .setBold());

            // Crear tabla: 4 columnas
            Table table = new Table(UnitValue.createPercentArray(new float[]{50, 15, 17, 18}));
            table.setWidth(UnitValue.createPercentValue(100));

            // Encabezados
            Cell header1 = new Cell().add(new Paragraph("Producto").setBold());
            Cell header2 = new Cell().add(new Paragraph("Cant.").setBold()).setTextAlignment(TextAlignment.CENTER);
            Cell header3 = new Cell().add(new Paragraph("Precio").setBold()).setTextAlignment(TextAlignment.RIGHT);
            Cell header4 = new Cell().add(new Paragraph("Subtotal").setBold()).setTextAlignment(TextAlignment.RIGHT);

            header1.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            header2.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            header3.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            header4.setBackgroundColor(ColorConstants.LIGHT_GRAY);

            table.addCell(header1);
            table.addCell(header2);
            table.addCell(header3);
            table.addCell(header4);

            // Filas de productos
            if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
                for (ItemPedido item : pedido.getItems()) {
                    table.addCell(new Cell().add(new Paragraph(item.getNombreProductoSeguro())));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(item.getCantidad())))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph("S/ " + String.format("%.2f", item.getPrecio())))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph("S/ " + String.format("%.2f", item.getSubtotal())))
                            .setTextAlignment(TextAlignment.RIGHT));
                }
            }

            document.add(table);

            document.add(new Paragraph(" "));

            // ========== TOTAL ==========
            Paragraph total = new Paragraph("TOTAL: S/ " + String.format("%.2f", pedido.getTotal()))
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFontColor(ColorConstants.GREEN);
            document.add(total);

            document.add(new Paragraph(" "));

            // ========== CLIENTE ==========
            if (pedido.getUsuario() != null) {
                document.add(new Paragraph("👤 Cliente")
                        .setFontSize(14)
                        .setBold());

                document.add(new Paragraph("Nombre: " + pedido.getUsuario().getNombres() + " " + pedido.getUsuario().getApellidos())
                        .setFontSize(12));
                document.add(new Paragraph("Email: " + pedido.getUsuario().getUsername())
                        .setFontSize(12));
                if (pedido.getUsuario().getTelefono() != null && !pedido.getUsuario().getTelefono().isEmpty()) {
                    document.add(new Paragraph("Teléfono: " + pedido.getUsuario().getTelefono())
                            .setFontSize(12));
                }
            }

            document.add(new Paragraph(" "));

            // ========== FOOTER ==========
            // Línea separadora
            document.add(new Paragraph("_____________________________________________")
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(" "));

            document.add(new Paragraph("¡Gracias por tu compra en Luren Chicken!")
                    .setFontSize(12)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("¿Tienes dudas? Llámanos al 123-456-7890")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(" "));

            // Cerrar documento
            document.close();

            // Retornar PDF
            byte[] pdfBytes = baos.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "comprobante-" + pedido.getNumeroPedido() + ".pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}