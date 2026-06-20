package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.CarritoItem;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Direccion;
import com.sistemaapolloAngular.sistema_apolloAngular.service.CarritoService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.UsuarioService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.DireccionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/pago")
public class PagoController {

    private final CarritoService carritoService;
    private final UsuarioService usuarioService;
    private final DireccionService direccionService;

    public PagoController(CarritoService carritoService,
                          UsuarioService usuarioService,
                          DireccionService direccionService) {
        this.carritoService = carritoService;
        this.usuarioService = usuarioService;
        this.direccionService = direccionService;
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

            // Obtener carrito del usuario
            List<CarritoItem> carrito = carritoService.obtenerCarrito(usuarioId);
            if (carrito == null) {
                carrito = new ArrayList<>();
            }

            // Obtener direcciones del usuario
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

            // Calcular totales
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

            // Datos del usuario
            Map<String, Object> usuarioData = new HashMap<>();
            usuarioData.put("id", usuario.getId());
            usuarioData.put("nombres", usuario.getNombres());
            usuarioData.put("apellidos", usuario.getApellidos());
            usuarioData.put("email", usuario.getUsername());
            usuarioData.put("telefono", usuario.getTelefono());

            // Datos del carrito - CORREGIDO
            List<Map<String, Object>> carritoData = new ArrayList<>();
            for (CarritoItem item : carrito) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("id", item.getId());

                // Obtener nombre del producto desde el objeto Producto
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

            // Datos de direcciones
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

            // Dirección predeterminada
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