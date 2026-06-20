package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.CarritoItem;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.service.PedidoService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.CarritoService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final CarritoService carritoService;
    private final UsuarioService usuarioService;
    private final ObjectMapper objectMapper;

    public PedidoController(PedidoService pedidoService,
                            CarritoService carritoService,
                            UsuarioService usuarioService) {
        this.pedidoService = pedidoService;
        this.carritoService = carritoService;
        this.usuarioService = usuarioService;
        this.objectMapper = new ObjectMapper();
    }


    @PostMapping("/crear")
    public ResponseEntity<Map<String, Object>> crearPedido(@RequestBody Map<String, Object> datos,
                                                           Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== CREAR PEDIDO API REST ===");

            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String correo = authentication.getName();
            System.out.println(" Usuario autenticado: " + correo);

            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<CarritoItem> carrito = carritoService.obtenerCarrito(usuario.getId());
            System.out.println("🛒 Items en carrito: " + carrito.size());

            if (carrito.isEmpty()) {
                response.put("success", false);
                response.put("message", "El carrito está vacío");
                return ResponseEntity.badRequest().body(response);
            }

            Pedido pedido = pedidoService.crearPedido(usuario, carrito, datos);
            System.out.println(" Pedido creado - ID: " + pedido.getId());

            carritoService.limpiarCarrito(usuario.getId());
            System.out.println(" Carrito limpiado");

            response.put("success", true);
            response.put("message", "Pedido creado exitosamente");
            response.put("pedidoId", pedido.getId());
            response.put("numeroPedido", pedido.getNumeroPedido());
            response.put("estado", pedido.getEstado());
            response.put("total", pedido.getTotal());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println(" Error al crear pedido: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Error al crear pedido: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/{pedidoId}")
    public ResponseEntity<?> obtenerPedido(@PathVariable Long pedidoId, Authentication authentication) {
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

            Optional<Pedido> pedidoOpt = pedidoService.obtenerPedidoPorId(pedidoId);

            if (pedidoOpt.isPresent() && pedidoOpt.get().getUsuario().getId().equals(usuario.getId())) {
                Pedido pedido = pedidoOpt.get();

                // Crear respuesta con datos del pedido
                Map<String, Object> pedidoData = new HashMap<>();
                pedidoData.put("id", pedido.getId());
                pedidoData.put("numeroPedido", pedido.getNumeroPedido());
                pedidoData.put("fechaPedido", pedido.getFechaPedido());
                pedidoData.put("total", pedido.getTotal());
                pedidoData.put("estado", pedido.getEstado());
                pedidoData.put("metodoPago", pedido.getMetodoPago());
                pedidoData.put("tipoEntrega", pedido.getTipoEntrega());
                pedidoData.put("direccionEntrega", pedido.getDireccionEntrega());
                pedidoData.put("subtotal", pedido.getSubtotal());
                pedidoData.put("costoEnvio", pedido.getCostoEnvio());
                pedidoData.put("descuento", pedido.getDescuento());
                pedidoData.put("observaciones", pedido.getObservaciones());

                // Items del pedido
                List<Map<String, Object>> itemsList = new ArrayList<>();
                if (pedido.getItems() != null) {
                    for (ItemPedido item : pedido.getItems()) {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("id", item.getId());
                        itemMap.put("cantidad", item.getCantidad());
                        itemMap.put("precio", item.getPrecio());
                        itemMap.put("subtotal", item.getSubtotal());
                        itemMap.put("nombreProducto", item.getNombreProducto());
                        itemsList.add(itemMap);
                    }
                }
                pedidoData.put("items", itemsList);

                // Información del usuario
                Map<String, Object> usuarioData = new HashMap<>();
                usuarioData.put("nombres", pedido.getUsuario().getNombres());
                usuarioData.put("apellidos", pedido.getUsuario().getApellidos());
                usuarioData.put("telefono", pedido.getUsuario().getTelefono());
                usuarioData.put("email", pedido.getUsuario().getUsername());
                pedidoData.put("usuario", usuarioData);

                return ResponseEntity.ok(pedidoData);
            } else {
                response.put("success", false);
                response.put("message", "Pedido no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al cargar el pedido: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/todos")
    public ResponseEntity<?> obtenerTodosLosPedidos() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Pedido> pedidos = pedidoService.obtenerTodosLosPedidos();

            if (pedidos.isEmpty()) {
                response.put("success", true);
                response.put("data", List.of());
                response.put("total", 0);
                return ResponseEntity.ok(response);
            }

            List<Map<String, Object>> pedidosData = new ArrayList<>();
            for (Pedido pedido : pedidos) {
                Map<String, Object> pedidoMap = new HashMap<>();
                pedidoMap.put("id", pedido.getId());
                pedidoMap.put("numeroPedido", pedido.getNumeroPedido());
                pedidoMap.put("fechaPedido", pedido.getFechaPedido());
                pedidoMap.put("total", pedido.getTotal());
                pedidoMap.put("estado", pedido.getEstado());
                pedidoMap.put("metodoPago", pedido.getMetodoPago());
                pedidoMap.put("tipoEntrega", pedido.getTipoEntrega());

                if (pedido.getUsuario() != null) {
                    Map<String, String> usuarioMap = new HashMap<>();
                    usuarioMap.put("nombres", pedido.getUsuario().getNombres());
                    usuarioMap.put("apellidos", pedido.getUsuario().getApellidos());
                    pedidoMap.put("usuario", usuarioMap);
                }

                pedidosData.add(pedidoMap);
            }

            response.put("success", true);
            response.put("data", pedidosData);
            response.put("total", pedidosData.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al cargar los pedidos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/mis-pedidos")
    public ResponseEntity<?> obtenerMisPedidos(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String correo = authentication.getName();
            Optional<Usuario> usuarioOpt = usuarioService.buscarPorCorreo(correo);

            if (usuarioOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Usuario usuario = usuarioOpt.get();
            List<Pedido> pedidos = pedidoService.obtenerPedidosPorUsuarioConItems(usuario.getId());

            List<Map<String, Object>> pedidosResponse = new ArrayList<>();

            for (Pedido pedido : pedidos) {
                Map<String, Object> pedidoMap = new HashMap<>();
                pedidoMap.put("id", pedido.getId());
                pedidoMap.put("numeroPedido", pedido.getNumeroPedido());
                pedidoMap.put("estado", pedido.getEstado());
                pedidoMap.put("fechaPedido", pedido.getFechaPedido());
                pedidoMap.put("total", pedido.getTotal());
                pedidoMap.put("metodoPago", pedido.getMetodoPago());
                pedidoMap.put("tipoEntrega", pedido.getTipoEntrega());
                pedidoMap.put("direccionEntrega", pedido.getDireccionEntrega());

                List<Map<String, Object>> itemsList = new ArrayList<>();
                if (pedido.getItems() != null) {
                    for (ItemPedido item : pedido.getItems()) {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("cantidad", item.getCantidad());
                        itemMap.put("precio", item.getPrecio());
                        itemMap.put("subtotal", item.getSubtotal());
                        itemMap.put("nombreProducto", item.getNombreProducto());
                        itemsList.add(itemMap);
                    }
                }
                pedidoMap.put("items", itemsList);

                pedidosResponse.add(pedidoMap);
            }

            response.put("success", true);
            response.put("data", pedidosResponse);
            response.put("total", pedidosResponse.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println(" ERROR en obtenerMisPedidos: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/buscar/{numeroPedido}")
    public ResponseEntity<?> buscarPedido(@PathVariable String numeroPedido) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Pedido> pedidoOpt = pedidoService.buscarPorNumeroPedido(numeroPedido);

            if (pedidoOpt.isPresent()) {
                Pedido pedido = pedidoOpt.get();

                Map<String, Object> pedidoData = new HashMap<>();
                pedidoData.put("id", pedido.getId());
                pedidoData.put("numeroPedido", pedido.getNumeroPedido());
                pedidoData.put("estado", pedido.getEstado());
                pedidoData.put("fechaPedido", pedido.getFechaPedido());
                pedidoData.put("total", pedido.getTotal());
                pedidoData.put("tipoEntrega", pedido.getTipoEntrega());
                pedidoData.put("direccionEntrega", pedido.getDireccionEntrega());

                if (pedido.getUsuario() != null) {
                    Map<String, String> usuarioMap = new HashMap<>();
                    usuarioMap.put("nombres", pedido.getUsuario().getNombres());
                    usuarioMap.put("apellidos", pedido.getUsuario().getApellidos());
                    pedidoData.put("usuario", usuarioMap);
                }

                List<Map<String, Object>> itemsList = new ArrayList<>();
                if (pedido.getItems() != null) {
                    for (ItemPedido item : pedido.getItems()) {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("nombreProducto", item.getNombreProducto());
                        itemMap.put("cantidad", item.getCantidad());
                        itemMap.put("precio", item.getPrecio());
                        itemMap.put("subtotal", item.getSubtotal());
                        itemsList.add(itemMap);
                    }
                }
                pedidoData.put("items", itemsList);

                response.put("success", true);
                response.put("data", pedidoData);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "No se encontró ningún pedido con el número: " + numeroPedido);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al buscar pedido: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}