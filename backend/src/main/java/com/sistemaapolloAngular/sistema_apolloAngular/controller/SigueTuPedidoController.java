package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Map<String, Object>> buscarPedido(
            @RequestParam("numeroPedido") String numeroPedido,
            @RequestParam(value = "canalPedido", required = false, defaultValue = "WEB") String canalPedido) {

        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== DEBUG SIGUE TU PEDIDO API ===");
            System.out.println("Buscando: " + numeroPedido);
            System.out.println("Canal: " + canalPedido);

            // Limpiar y formatear el número de pedido
            String numeroLimpio = numeroPedido.trim().toUpperCase();

            // Buscar por número de pedido
            Optional<Pedido> pedidoOpt = pedidoRepository.findByNumeroPedido(numeroLimpio);

            // Si no se encuentra, buscar por numero
            if (!pedidoOpt.isPresent()) {
                pedidoOpt = pedidoRepository.findByNumero(numeroLimpio);
            }

            System.out.println("Encontrado: " + pedidoOpt.isPresent());

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();
                System.out.println("ID Pedido: " + pedido.getId());
                System.out.println("Número: " + pedido.getNumero());
                System.out.println("Número Pedido: " + pedido.getNumeroPedido());
                System.out.println("Estado: " + pedido.getEstado());

                // Verificar que el canal coincida (si se especificó)
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

                // Construir respuesta con datos del pedido
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

                // Información del cliente
                if (pedido.getUsuario() != null) {
                    Map<String, String> usuarioData = new HashMap<>();
                    usuarioData.put("nombres", pedido.getUsuario().getNombres());
                    usuarioData.put("apellidos", pedido.getUsuario().getApellidos());
                    usuarioData.put("telefono", pedido.getUsuario().getTelefono());
                    usuarioData.put("email", pedido.getUsuario().getUsername());
                    pedidoData.put("usuario", usuarioData);
                }

                // Items del pedido
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
                response.put("message", "No se encontró ningún pedido con el número: " + numeroLimpio +
                        ". Verifica que el número sea correcto.");
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
     * Busca un pedido por número (sin canal)
     */
    @GetMapping("/buscar-simple")
    public ResponseEntity<Map<String, Object>> buscarPedidoSimple(
            @RequestParam("numeroPedido") String numeroPedido) {

        return buscarPedido(numeroPedido, "WEB");
    }

    /**
     * Obtiene el estado de un pedido por número
     */
    @GetMapping("/estado")
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
}