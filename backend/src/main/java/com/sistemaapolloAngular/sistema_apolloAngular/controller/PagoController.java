package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.CarritoItem;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Direccion;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Pago;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PagoRepository;
import com.sistemaapolloAngular.sistema_apolloAngular.service.CarritoService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.UsuarioService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.DireccionService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.PedidoService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.MercadoPagoProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/pago")
public class PagoController {

    private final CarritoService carritoService;
    private final UsuarioService usuarioService;
    private final DireccionService direccionService;
    private final PedidoService pedidoService;
    private final MercadoPagoProxyService mercadoPagoProxyService;

    @Autowired
    private PagoRepository pagoRepository;

    public PagoController(CarritoService carritoService,
                          UsuarioService usuarioService,
                          DireccionService direccionService,
                          PedidoService pedidoService,
                          MercadoPagoProxyService mercadoPagoProxyService) {
        this.carritoService = carritoService;
        this.usuarioService = usuarioService;
        this.direccionService = direccionService;
        this.pedidoService = pedidoService;
        this.mercadoPagoProxyService = mercadoPagoProxyService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerDatosPago(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Long usuarioId = usuario.getId();

            List<CarritoItem> carrito = carritoService.obtenerCarrito(usuarioId);
            if (carrito == null) {
                carrito = new ArrayList<>();
            }

            List<Direccion> direcciones = new ArrayList<>();
            Direccion direccionPredeterminada = null;

            try {
                direcciones = direccionService.obtenerDireccionesUsuario(correo);
                Optional<Direccion> predeterminadaOpt = direccionService.obtenerDireccionPredeterminada(correo);
                if (predeterminadaOpt.isPresent()) {
                    direccionPredeterminada = predeterminadaOpt.get();
                }
            } catch (Exception e) {
                System.err.println(" Error al cargar direcciones: " + e.getMessage());
            }

            double subtotal = 0.0;
            double costoEnvio = 0.0;
            double descuento = 0.0;
            double total = 0.0;

            if (!carrito.isEmpty()) {
                subtotal = carrito.stream()
                        .mapToDouble(item -> item.getPrecioUnitario() * item.getCantidad())
                        .sum();

                costoEnvio = subtotal >= 50 ? 0 : 5;
                descuento = subtotal >= 80 ? 10 : 0;
                total = subtotal + costoEnvio - descuento;
            }

            Map<String, Object> usuarioData = new HashMap<>();
            usuarioData.put("id", usuario.getId());
            usuarioData.put("nombres", usuario.getNombres());
            usuarioData.put("apellidos", usuario.getApellidos());
            usuarioData.put("email", usuario.getUsername());
            usuarioData.put("telefono", usuario.getTelefono());

            List<Map<String, Object>> carritoData = new ArrayList<>();
            for (CarritoItem item : carrito) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("id", item.getId());

                String nombreProducto = "Producto";
                if (item.getProducto() != null) {
                    nombreProducto = item.getProducto().getNombre();
                }

                itemData.put("nombre", nombreProducto);
                itemData.put("cantidad", item.getCantidad());
                itemData.put("precioUnitario", item.getPrecioUnitario());
                itemData.put("subtotal", item.getPrecioUnitario() * item.getCantidad());

                if (item.getProducto() != null) {
                    itemData.put("productoId", item.getProducto().getId());
                    itemData.put("imagen", item.getProducto().getImagenUrl());
                }

                carritoData.add(itemData);
            }

            List<Map<String, Object>> direccionesData = new ArrayList<>();
            for (Direccion direccion : direcciones) {
                Map<String, Object> dirData = new HashMap<>();
                dirData.put("id", direccion.getId());
                dirData.put("nombre", direccion.getNombre());
                dirData.put("direccion", direccion.getDireccion());
                dirData.put("referencia", direccion.getReferencia());
                dirData.put("ciudad", direccion.getCiudad());
                dirData.put("telefono", direccion.getTelefono());
                dirData.put("predeterminada", direccion.isPredeterminada());
                dirData.put("facturacion", direccion.isFacturacion());
                direccionesData.add(dirData);
            }

            Map<String, Object> direccionPredData = null;
            if (direccionPredeterminada != null) {
                direccionPredData = new HashMap<>();
                direccionPredData.put("id", direccionPredeterminada.getId());
                direccionPredData.put("nombre", direccionPredeterminada.getNombre());
                direccionPredData.put("direccion", direccionPredeterminada.getDireccion());
                direccionPredData.put("referencia", direccionPredeterminada.getReferencia());
                direccionPredData.put("ciudad", direccionPredeterminada.getCiudad());
                direccionPredData.put("telefono", direccionPredeterminada.getTelefono());
                direccionPredData.put("predeterminada", direccionPredeterminada.isPredeterminada());
            }

            response.put("success", true);
            response.put("usuario", usuarioData);
            response.put("carrito", carritoData);
            response.put("direcciones", direccionesData);
            response.put("direccionPredeterminada", direccionPredData);
            response.put("resumen", Map.of(
                    "subtotal", subtotal,
                    "costoEnvio", costoEnvio,
                    "descuento", descuento,
                    "total", total,
                    "cantidadItems", carrito.size()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println(" ERROR en PagoController: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Error al obtener datos de pago: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ✅ Crear preferencia llamando al MICROSERVICIO
     */
    @PostMapping("/crear-preferencia")
    public ResponseEntity<?> crearPreferencia(@RequestBody Map<String, Long> request, Authentication auth) {
        try {
            System.out.println("📝 Creando preferencia - Inicio");

            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            Long pedidoId = request.get("pedidoId");
            System.out.println("📝 pedidoId: " + pedidoId);

            if (pedidoId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "pedidoId es requerido"));
            }

            // Obtener el pedido con sus items
            var pedidoOpt = pedidoService.obtenerPedidoConItems(pedidoId);
            if (pedidoOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Pedido no encontrado"));
            }

            var pedido = pedidoOpt.get();

            // Preparar los datos para el microservicio
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("pedidoId", pedidoId);

            Map<String, Object> pedidoData = new HashMap<>();
            List<Map<String, Object>> itemsData = new ArrayList<>();

            for (var item : pedido.getItems()) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("id", item.getId());
                itemData.put("nombre", item.getNombreProducto());
                itemData.put("cantidad", item.getCantidad());
                itemData.put("precio", item.getPrecio());
                itemsData.add(itemData);
            }

            pedidoData.put("items", itemsData);
            requestData.put("pedido", pedidoData);

            // 🔥 Llamar al microservicio
            Map<String, Object> response = mercadoPagoProxyService.crearPreferencia(requestData);

            // Guardar preferenceId en el pedido
            String preferenceId = (String) response.get("preferenceId");
            if (preferenceId != null) {
                pedido.setPreferenceId(preferenceId);
                pedidoService.actualizarPedido(pedido);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error creando preferencia: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear la preferencia: " + e.getMessage()));
        }
    }

    /**
     * ✅ Webhook que recibe del microservicio
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("📥 Webhook recibido del microservicio: " + payload);

            String paymentId = (String) payload.get("paymentId");
            String status = (String) payload.get("status");
            String externalReference = (String) payload.get("externalReference");

            if (paymentId == null || externalReference == null) {
                System.err.println("❌ Datos incompletos en webhook");
                return ResponseEntity.badRequest().body(Map.of("error", "Datos incompletos"));
            }

            Long pedidoId = Long.parseLong(externalReference);
            var pedidoOpt = pedidoService.obtenerPedidoPorId(pedidoId);

            if (pedidoOpt.isEmpty()) {
                System.err.println("❌ Pedido no encontrado: " + pedidoId);
                return ResponseEntity.ok(Map.of("status", "pedido_no_encontrado"));
            }

            var pedido = pedidoOpt.get();
            pedido.setPaymentId(paymentId);

            if ("approved".equals(status)) {
                pedido.setEstado("PAGADO");
                System.out.println("✅ Pedido " + pedidoId + " pagado exitosamente");
            } else if ("rejected".equals(status)) {
                pedido.setEstado("RECHAZADO");
                System.out.println("❌ Pedido " + pedidoId + " rechazado");
            } else {
                pedido.setEstado("PENDIENTE");
                System.out.println("⏳ Pedido " + pedidoId + " pendiente");
            }

            pedidoService.actualizarPedido(pedido);
            System.out.println("✅ Webhook procesado correctamente para pedido: " + pedidoId);

            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (Exception e) {
            System.err.println("❌ Error en webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ Endpoint para confirmar pago desde el microservicio (IDEMPOTENTE)
     */
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmarPago(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("📥 Confirmación de pago desde microservicio: " + payload);

            String paymentId = payload.get("paymentId") != null
                    ? String.valueOf(payload.get("paymentId")) : null;
            String status = (String) payload.get("status");
            String externalReference = payload.get("externalReference") != null
                    ? String.valueOf(payload.get("externalReference")) : null;

            if (externalReference == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "externalReference es requerido"));
            }

            Long pedidoId = Long.parseLong(externalReference);
            Optional<Pedido> pedidoOpt = pedidoService.obtenerPedidoPorId(pedidoId);

            if (pedidoOpt.isEmpty()) {
                System.err.println("❌ Pedido no encontrado: " + pedidoId);
                return ResponseEntity.ok(Map.of("status", "pedido_no_encontrado"));
            }

            Pedido pedido = pedidoOpt.get();

            // ✅ Atajo rápido: si ya está PAGADO, no hay nada más que hacer
            if ("PAGADO".equals(pedido.getEstado())) {
                System.out.println("ℹ️ Pedido " + pedidoId + " ya estaba PAGADO, ignorando confirmación duplicada");
                return ResponseEntity.ok(Map.of("status", "ya_procesado"));
            }

            pedido.setPaymentId(paymentId);

            if ("approved".equals(status)) {
                pedido.setEstado("PAGADO");
                System.out.println("✅ Pedido " + pedidoId + " pagado exitosamente");

                // ✅ Intentar guardar el pago; si otro hilo concurrente ya lo insertó,
                // la BD rechaza con violación de restricción única -> lo tratamos como éxito
                try {
                    boolean pagoYaExiste = pagoRepository.existsByPedidoId(pedidoId);
                    if (!pagoYaExiste) {
                        Pago pago = new Pago();
                        pago.setPedido(pedido);
                        pago.setPaymentId(paymentId);
                        pago.setStatus(status);
                        pago.setMetodoPago((String) payload.get("paymentMethodId"));

                        Object amountObj = payload.get("amount");
                        Double amount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : null;
                        pago.setMonto(amount);

                        pago.setFechaPago(LocalDateTime.now());
                        pagoRepository.save(pago);
                    } else {
                        System.out.println("ℹ️ Ya existe un registro de pago para el pedido " + pedidoId + ", se omite duplicado");
                    }
                } catch (org.springframework.dao.DataIntegrityViolationException dive) {
                    // ✅ CLAVE: otra petición concurrente ganó la carrera e insertó primero.
                    // El pago YA está guardado en la BD, así que no es un error real.
                    System.out.println("ℹ️ Pago para pedido " + pedidoId + " ya fue insertado por una petición concurrente (condición de carrera). Continuando normalmente.");
                }

            } else if ("rejected".equals(status)) {
                pedido.setEstado("RECHAZADO");
                System.out.println("❌ Pedido " + pedidoId + " rechazado");
            } else {
                pedido.setEstado("PENDIENTE");
                System.out.println("⏳ Pedido " + pedidoId + " pendiente");
            }

            pedidoService.actualizarPedido(pedido);
            System.out.println("✅ Confirmación procesada correctamente para pedido: " + pedidoId);

            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (Exception e) {
            System.err.println("❌ Error confirmando pago: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/calcular")
    public ResponseEntity<Map<String, Object>> calcularTotales(@RequestBody(required = false) Map<String, Object> datos) {
        Map<String, Object> response = new HashMap<>();

        try {
            double subtotal = 0.0;

            if (datos != null && datos.containsKey("items")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) datos.get("items");
                for (Map<String, Object> item : items) {
                    double precio = ((Number) item.getOrDefault("precio", 0)).doubleValue();
                    int cantidad = ((Number) item.getOrDefault("cantidad", 0)).intValue();
                    subtotal += precio * cantidad;
                }
            }

            double costoEnvio = subtotal >= 50 ? 0 : 5;
            double descuento = subtotal >= 80 ? 10 : 0;
            double total = subtotal + costoEnvio - descuento;

            response.put("success", true);
            response.put("subtotal", subtotal);
            response.put("costoEnvio", costoEnvio);
            response.put("descuento", descuento);
            response.put("total", total);
            response.put("envioGratis", subtotal >= 50);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al calcular totales: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/verificar")
    public ResponseEntity<Map<String, Object>> verificarCarrito(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<CarritoItem> carrito = carritoService.obtenerCarrito(usuario.getId());
            if (carrito == null) {
                carrito = new ArrayList<>();
            }

            response.put("success", true);
            response.put("tieneProductos", !carrito.isEmpty());
            response.put("cantidadItems", carrito.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al verificar carrito: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/direcciones")
    public ResponseEntity<Map<String, Object>> obtenerDireccionesPago(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String correo = authentication.getName();

            List<Direccion> direcciones = direccionService.obtenerDireccionesUsuario(correo);
            Optional<Direccion> predeterminadaOpt = direccionService.obtenerDireccionPredeterminada(correo);

            List<Map<String, Object>> direccionesData = new ArrayList<>();
            for (Direccion direccion : direcciones) {
                Map<String, Object> dirData = new HashMap<>();
                dirData.put("id", direccion.getId());
                dirData.put("nombre", direccion.getNombre());
                dirData.put("direccion", direccion.getDireccion());
                dirData.put("referencia", direccion.getReferencia());
                dirData.put("ciudad", direccion.getCiudad());
                dirData.put("telefono", direccion.getTelefono());
                dirData.put("predeterminada", direccion.isPredeterminada());
                dirData.put("facturacion", direccion.isFacturacion());
                direccionesData.add(dirData);
            }

            response.put("success", true);
            response.put("direcciones", direccionesData);
            response.put("direccionPredeterminada", predeterminadaOpt.map(d -> {
                Map<String, Object> dirData = new HashMap<>();
                dirData.put("id", d.getId());
                dirData.put("nombre", d.getNombre());
                dirData.put("direccion", d.getDireccion());
                dirData.put("referencia", d.getReferencia());
                dirData.put("ciudad", d.getCiudad());
                dirData.put("telefono", d.getTelefono());
                return dirData;
            }).orElse(null));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener direcciones: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}