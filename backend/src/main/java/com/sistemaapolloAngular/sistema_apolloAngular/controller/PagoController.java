package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.mercadopago.resources.payment.Payment;
import com.sistemaapolloAngular.sistema_apolloAngular.model.CarritoItem;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Direccion;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Pago;
import com.sistemaapolloAngular.sistema_apolloAngular.service.CarritoService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.UsuarioService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.DireccionService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.MercadoPagoService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.PedidoService;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PagoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/pago")
public class PagoController {

    private final CarritoService carritoService;
    private final UsuarioService usuarioService;
    private final DireccionService direccionService;
    private final MercadoPagoService mercadoPagoService;
    private final PedidoService pedidoService;
    private final PagoRepository pagoRepository;

    public PagoController(CarritoService carritoService,
                          UsuarioService usuarioService,
                          DireccionService direccionService,
                          MercadoPagoService mercadoPagoService,
                          PedidoService pedidoService,
                          PagoRepository pagoRepository) {
        this.carritoService = carritoService;
        this.usuarioService = usuarioService;
        this.direccionService = direccionService;
        this.mercadoPagoService = mercadoPagoService;
        this.pedidoService = pedidoService;
        this.pagoRepository = pagoRepository;
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

    @PostMapping("/crear-preferencia")
    @Transactional
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

            Optional<Pedido> pedidoOpt = pedidoService.obtenerPedidoConItems(pedidoId);

            if (pedidoOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Pedido no encontrado"));
            }

            Pedido pedido = pedidoOpt.get();

            Map<String, Object> preference = mercadoPagoService.crearPreferencia(pedido);

            String preferenceId = (String) preference.get("id");
            System.out.println("✅ Preferencia creada: " + preferenceId);

            pedido.setPreferenceId(preferenceId);
            pedidoService.actualizarPedido(pedido);

            Map<String, Object> response = new HashMap<>();
            response.put("preferenceId", preferenceId);
            response.put("publicKey", mercadoPagoService.getPublicKey());

            String initPoint = (String) preference.get("sandbox_init_point");
            if (initPoint == null || initPoint.isEmpty()) {
                initPoint = (String) preference.get("init_point");
            }
            response.put("initPoint", initPoint);

            System.out.println("🔗 InitPoint: " + initPoint);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error creando preferencia: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear la preferencia: " + e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("📥 Webhook recibido: " + payload);

            String type = (String) payload.get("type");
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            String paymentId = data != null ? (String) data.get("id") : null;

            System.out.println("🔍 Type: " + type + ", PaymentId: " + paymentId);

            // ✅ También manejar cuando el webhook viene como merchant_order
            if (paymentId == null && payload.containsKey("resource")) {
                String resource = (String) payload.get("resource");
                if (resource != null && resource.contains("/payments/")) {
                    paymentId = resource.substring(resource.lastIndexOf("/") + 1);
                    System.out.println("🔍 PaymentId extraído de resource: " + paymentId);
                }
            }

            if (paymentId != null) {
                try {
                    Payment payment = mercadoPagoService.obtenerPago(paymentId);
                    System.out.println("💰 Pago obtenido: " + payment.getId() + " - Status: " + payment.getStatus());

                    String externalReference = payment.getExternalReference();
                    if (externalReference == null) {
                        System.err.println("❌ External reference no encontrada");
                        return ResponseEntity.ok(Map.of("status", "ignored"));
                    }

                    Long pedidoId = Long.parseLong(externalReference);
                    Optional<Pedido> pedidoOpt = pedidoService.obtenerPedidoPorId(pedidoId);

                    if (pedidoOpt.isEmpty()) {
                        System.err.println("❌ Pedido no encontrado: " + pedidoId);
                        return ResponseEntity.ok(Map.of("status", "pedido_no_encontrado"));
                    }

                    Pedido pedido = pedidoOpt.get();

                    // ✅ Guardar paymentId en el pedido SIEMPRE
                    pedido.setPaymentId(payment.getId().toString());
                    System.out.println("💳 PaymentId guardado: " + payment.getId());

                    // ✅ Actualizar estado según el pago
                    if ("approved".equals(payment.getStatus())) {
                        pedido.setEstado("PAGADO");
                        System.out.println("✅ Pedido " + pedidoId + " pagado exitosamente");

                        // Crear registro de pago
                        Pago pago = new Pago();
                        pago.setPedido(pedido);
                        pago.setPaymentId(payment.getId().toString());
                        pago.setStatus(payment.getStatus());
                        pago.setMetodoPago(payment.getPaymentMethodId());
                        pago.setMonto(payment.getTransactionAmount().doubleValue());
                        pago.setFechaPago(java.time.LocalDateTime.now());
                        pago.setResponseJson(payment.toString());
                        pagoRepository.save(pago);

                    } else if ("rejected".equals(payment.getStatus())) {
                        pedido.setEstado("RECHAZADO");
                        System.out.println("❌ Pedido " + pedidoId + " rechazado");
                    } else {
                        pedido.setEstado("PENDIENTE");
                        System.out.println("⏳ Pedido " + pedidoId + " pendiente");
                    }

                    pedidoService.actualizarPedido(pedido);
                    System.out.println("✅ Webhook procesado correctamente para pedido: " + pedidoId);

                } catch (Exception e) {
                    System.err.println("❌ Error procesando pago: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("ℹ️ Webhook ignorado (no es payment o no tiene paymentId)");
            }

            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (Exception e) {
            System.err.println("❌ Error en webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ NUEVO: Verificar pago manualmente consultando a MercadoPago
    @GetMapping("/verificar-pedido/{pedidoId}")
    public ResponseEntity<?> verificarPagoPedido(@PathVariable Long pedidoId, Authentication authentication) {
        try {
            System.out.println("🔍 Verificando pago para pedido: " + pedidoId);

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Optional<Pedido> pedidoOpt = pedidoService.obtenerPedidoConItems(pedidoId);

            if (pedidoOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Pedido no encontrado"));
            }

            Pedido pedido = pedidoOpt.get();

            // Verificar que el usuario sea el dueño
            if (!pedido.getUsuario().getId().equals(usuario.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No autorizado"));
            }

            // Preparar respuesta base
            Map<String, Object> response = new HashMap<>();
            response.put("pedidoId", pedido.getId());
            response.put("estado", pedido.getEstado());
            response.put("paymentId", pedido.getPaymentId());
            response.put("fecha", pedido.getFechaPedido());

            // Si tiene paymentId, verificar con MercadoPago
            if (pedido.getPaymentId() != null && !pedido.getPaymentId().isEmpty()) {
                try {
                    Payment payment = mercadoPagoService.obtenerPago(pedido.getPaymentId());

                    response.put("paymentStatus", payment.getStatus());
                    response.put("paymentStatusDetail", payment.getStatusDetail());
                    response.put("transactionAmount", payment.getTransactionAmount());
                    response.put("paymentMethodId", payment.getPaymentMethodId());

                    // ✅ Si el pago está aprobado pero el pedido no, actualizarlo
                    if ("approved".equals(payment.getStatus()) && !"PAGADO".equals(pedido.getEstado())) {
                        pedido.setEstado("PAGADO");
                        pedidoService.actualizarPedido(pedido);
                        response.put("estado", "PAGADO");
                        System.out.println("✅ Pedido " + pedidoId + " actualizado a PAGADO manualmente");
                    }

                    return ResponseEntity.ok(response);

                } catch (Exception e) {
                    System.err.println("❌ Error obteniendo pago de MercadoPago: " + e.getMessage());
                    response.put("message", "No se pudo verificar el pago en MercadoPago");
                    return ResponseEntity.ok(response);
                }
            } else {
                response.put("message", "El pago aún no ha sido procesado");
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            System.err.println("❌ Error verificando pago: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al verificar el pago: " + e.getMessage()));
        }
    }

    @GetMapping("/public-key")
    public ResponseEntity<?> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", mercadoPagoService.getPublicKey()));
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