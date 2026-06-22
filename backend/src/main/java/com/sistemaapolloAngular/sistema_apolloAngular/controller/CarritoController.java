package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.dto.CarritoItemDTO;
import com.sistemaapolloAngular.sistema_apolloAngular.model.CarritoItem;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.service.CarritoService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.ProductoFinalService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // ✅ IMPORTANTE: Agregar esta importación

@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final CarritoService carritoService;
    private final ProductoFinalService productoFinalService;
    private final UsuarioService usuarioService;

    public CarritoController(CarritoService carritoService,
                             ProductoFinalService productoFinalService,
                             UsuarioService usuarioService) {
        this.carritoService = carritoService;
        this.productoFinalService = productoFinalService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @ResponseBody
    public Map<String, Object> obtenerCarrito(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<CarritoItem> carrito = carritoService.obtenerCarrito(usuario.getId());

            // ✅ CONVERTIR A DTO
            List<CarritoItemDTO> itemsDTO = carrito.stream()
                    .map(CarritoItemDTO::new)
                    .collect(Collectors.toList());

            // Calcular total
            double total = carrito.stream()
                    .mapToDouble(item -> item.getPrecioUnitario() * item.getCantidad())
                    .sum();

            // Calcular cantidad total de items
            int cantidadItems = carrito.stream()
                    .mapToInt(CarritoItem::getCantidad)
                    .sum();

            response.put("success", true);
            response.put("items", itemsDTO); // ✅ Enviar DTO, no la entidad
            response.put("total", total);
            response.put("cantidadItems", cantidadItems);
            response.put("totalItems", carrito.size());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener carrito: " + e.getMessage());
            response.put("items", List.of());
            response.put("total", 0.0);
            response.put("cantidadItems", 0);
        }

        return response;
    }

    @PostMapping("/agregar")
    @ResponseBody
    public Map<String, Object> agregarAlCarrito(@RequestParam Long productoId,
                                                @RequestParam int cantidad,
                                                Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            ProductoFinal producto = productoFinalService.obtenerPorId(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            carritoService.agregarProducto(usuario, producto, cantidad, producto.getPrecio());

            // Obtener carrito actualizado
            List<CarritoItem> carrito = carritoService.obtenerCarrito(usuario.getId());
            double total = carrito.stream()
                    .mapToDouble(item -> item.getPrecioUnitario() * item.getCantidad())
                    .sum();

            response.put("success", true);
            response.put("message", "Producto agregado al carrito");
            response.put("total", total);
            response.put("productoNombre", producto.getNombre());
            response.put("cantidadItems", carrito.stream().mapToInt(CarritoItem::getCantidad).sum());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al agregar producto: " + e.getMessage());
        }

        return response;
    }

    @PutMapping("/actualizar/{id}")
    @ResponseBody
    public Map<String, Object> actualizarCantidad(@PathVariable Long id,
                                                  @RequestParam int cantidad,
                                                  Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            carritoService.actualizarCantidad(id, cantidad);

            // Obtener carrito actualizado
            List<CarritoItem> carrito = carritoService.obtenerCarrito(usuario.getId());
            double total = carrito.stream()
                    .mapToDouble(item -> item.getPrecioUnitario() * item.getCantidad())
                    .sum();

            response.put("success", true);
            response.put("message", "Cantidad actualizada");
            response.put("total", total);
            response.put("cantidadItems", carrito.stream().mapToInt(CarritoItem::getCantidad).sum());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar cantidad: " + e.getMessage());
        }

        return response;
    }

    @DeleteMapping("/eliminar/{id}")
    @ResponseBody
    public Map<String, Object> eliminarDelCarrito(@PathVariable Long id,
                                                  Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            carritoService.eliminarProducto(id);

            // Obtener carrito actualizado
            List<CarritoItem> carrito = carritoService.obtenerCarrito(usuario.getId());
            double total = carrito.stream()
                    .mapToDouble(item -> item.getPrecioUnitario() * item.getCantidad())
                    .sum();

            response.put("success", true);
            response.put("message", "Producto eliminado del carrito");
            response.put("total", total);
            response.put("cantidadItems", carrito.stream().mapToInt(CarritoItem::getCantidad).sum());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar producto: " + e.getMessage());
        }

        return response;
    }

    @DeleteMapping("/vaciar")
    @ResponseBody
    public Map<String, Object> vaciarCarrito(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            carritoService.vaciarCarrito(usuario.getId());

            response.put("success", true);
            response.put("message", "Carrito vaciado exitosamente");
            response.put("total", 0.0);
            response.put("cantidadItems", 0);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al vaciar carrito: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/total")
    @ResponseBody
    public Map<String, Object> obtenerTotalCarrito(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<CarritoItem> carrito = carritoService.obtenerCarrito(usuario.getId());
            double total = carrito.stream()
                    .mapToDouble(item -> item.getPrecioUnitario() * item.getCantidad())
                    .sum();

            response.put("success", true);
            response.put("total", total);
            response.put("cantidadItems", carrito.stream().mapToInt(CarritoItem::getCantidad).sum());

        } catch (Exception e) {
            response.put("success", false);
            response.put("total", 0.0);
            response.put("cantidadItems", 0);
            response.put("error", e.getMessage());
        }

        return response;
    }
}