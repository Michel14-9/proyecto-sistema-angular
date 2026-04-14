package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.CarritoItem;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.service.CarritoService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.ProductoFinalService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/carrito")
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

    //  Mostrar carrito
    @GetMapping
    public String verCarrito(Authentication authentication, Model model) {
        // Obtener el correo/username del usuario autenticado
        String correo = authentication.getName();
        Usuario usuario = usuarioService.buscarPorCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Long usuarioId = usuario.getId();

        List<CarritoItem> carrito = carritoService.obtenerCarrito(usuarioId);
        double total = carrito.stream()
                .mapToDouble(item -> item.getPrecioUnitario() * item.getCantidad())
                .sum();

        model.addAttribute("carrito", carrito);
        model.addAttribute("total", total);
        return "carrito";
    }

    //  Agregar producto al carrito
    @PostMapping("/agregar")
    public String agregarAlCarrito(@RequestParam Long productoId,
                                   @RequestParam int cantidad,
                                   Authentication authentication) {
        String correo = authentication.getName();
        Usuario usuario = usuarioService.buscarPorCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        ProductoFinal producto = productoFinalService.obtenerPorId(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        carritoService.agregarProducto(usuario, producto, cantidad, producto.getPrecio());

        return "redirect:/carrito";
    }


    @PostMapping("/actualizar/{id}")
    public String actualizarCantidad(@PathVariable Long id,
                                     @RequestParam int cantidad,
                                     Authentication authentication) {
        String correo = authentication.getName();
        Usuario usuario = usuarioService.buscarPorCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        carritoService.actualizarCantidad(id, cantidad);
        return "redirect:/carrito";
    }

    //  Eliminar item
    @GetMapping("/eliminar/{id}")
    public String eliminarDelCarrito(@PathVariable Long id,
                                     Authentication authentication) {
        String correo = authentication.getName();
        Usuario usuario = usuarioService.buscarPorCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        carritoService.eliminarProducto(id);
        return "redirect:/carrito";
    }


    @PostMapping("/vaciar")
    public String vaciarCarrito(Authentication authentication) {
        String correo = authentication.getName();
        Usuario usuario = usuarioService.buscarPorCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        carritoService.vaciarCarrito(usuario.getId());
        return "redirect:/carrito";
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
            response.put("cantidadItems", carrito.size());

        } catch (Exception e) {
            response.put("success", false);
            response.put("total", 0.0);
            response.put("cantidadItems", 0);
            response.put("error", e.getMessage());
        }

        return response;
    }


    @PostMapping("/agregar-ajax")
    @ResponseBody
    public Map<String, Object> agregarAlCarritoAjax(@RequestParam Long productoId,
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

            // Obtener el carrito actualizado para calcular el nuevo total
            List<CarritoItem> carrito = carritoService.obtenerCarrito(usuario.getId());
            double total = carrito.stream()
                    .mapToDouble(item -> item.getPrecioUnitario() * item.getCantidad())
                    .sum();

            response.put("success", true);
            response.put("message", "Producto agregado al carrito");
            response.put("total", total);
            response.put("cantidadItems", carrito.size());
            response.put("productoNombre", producto.getNombre());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al agregar producto: " + e.getMessage());
        }

        return response;
    }
}

