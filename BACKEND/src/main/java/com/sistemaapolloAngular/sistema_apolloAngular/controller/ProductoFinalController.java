package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.service.ProductoFinalService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/productos")
public class ProductoFinalController {

    private final ProductoFinalService productoFinalService;

    public ProductoFinalController(ProductoFinalService productoFinalService) {
        this.productoFinalService = productoFinalService;
    }

    //  Mostrar productos (vista admin)
    @GetMapping
    public String listarProductos(Model model) {
        List<ProductoFinal> productos = productoFinalService.obtenerTodos();
        model.addAttribute("productos", productos);
        return "admin/productos"; // tu vista de productos admin
    }

    //  Mostrar productos en menú público
    @GetMapping("/menu")
    public String mostrarMenu(Model model) {
        List<ProductoFinal> productos = productoFinalService.obtenerTodos();
        model.addAttribute("productos", productos);
        return "menu"; // vista para los clientes
    }

    //  Obtener producto
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerProducto(@PathVariable Long id) {
        Optional<ProductoFinal> producto = productoFinalService.obtenerPorId(id);
        return producto.isPresent() ? ResponseEntity.ok(producto.get())
                : ResponseEntity.notFound().build();
    }


    @PostMapping("/guardar")
    public String guardarProducto(ProductoFinal producto, RedirectAttributes redirect) {
        productoFinalService.guardar(producto);
        redirect.addFlashAttribute("success", " Producto guardado correctamente");
        return "redirect:/admin/productos";
    }


    @PostMapping("/guardar-json")
    @ResponseBody
    public ResponseEntity<?> guardarProductoJson(@RequestBody ProductoFinal producto) {
        ProductoFinal nuevo = productoFinalService.guardar(producto);
        return ResponseEntity.ok(nuevo);
    }


    @PostMapping("/actualizar")
    public String actualizarProducto(ProductoFinal producto, RedirectAttributes redirect) {
        try {
            productoFinalService.actualizar(producto);
            redirect.addFlashAttribute("success", " Producto actualizado correctamente");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", " Error al actualizar: " + e.getMessage());
        }
        return "redirect:/admin/productos";
    }


    @PutMapping("/actualizar-json")
    @ResponseBody
    public ResponseEntity<?> actualizarProductoJson(@RequestBody ProductoFinal producto) {
        try {
            ProductoFinal actualizado = productoFinalService.actualizar(producto);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }


    @PostMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable Long id, RedirectAttributes redirect) {
        productoFinalService.eliminar(id);
        redirect.addFlashAttribute("success", " Producto eliminado correctamente");
        return "redirect:/admin/productos";
    }


    @DeleteMapping("/eliminar-json/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarProductoJson(@PathVariable Long id) {
        productoFinalService.eliminar(id);
        return ResponseEntity.ok(" Producto eliminado correctamente");
    }
}


