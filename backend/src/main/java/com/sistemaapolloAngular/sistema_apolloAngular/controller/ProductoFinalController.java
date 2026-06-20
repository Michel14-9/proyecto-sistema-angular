package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.service.ProductoFinalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/productos")
public class ProductoFinalController {

    private final ProductoFinalService productoFinalService;

    public ProductoFinalController(ProductoFinalService productoFinalService) {
        this.productoFinalService = productoFinalService;
    }


    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerTodosLosProductos() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProductoFinal> productos = productoFinalService.obtenerTodos();

            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener productos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerProductoPorId(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<ProductoFinal> productoOpt = productoFinalService.obtenerPorId(id);

            if (productoOpt.isPresent()) {
                response.put("success", true);
                response.put("data", productoOpt.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Producto no encontrado con ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener producto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping
    public ResponseEntity<Map<String, Object>> guardarProducto(@RequestBody ProductoFinal producto) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validaciones básicas
            if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El nombre del producto es obligatorio");
                return ResponseEntity.badRequest().body(response);
            }

            if (producto.getPrecio() == null || producto.getPrecio() <= 0) {
                response.put("success", false);
                response.put("message", "El precio debe ser mayor a 0");
                return ResponseEntity.badRequest().body(response);
            }

            if (producto.getTipo() == null || producto.getTipo().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El tipo/categoría es obligatorio");
                return ResponseEntity.badRequest().body(response);
            }

            ProductoFinal nuevoProducto = productoFinalService.guardar(producto);

            response.put("success", true);
            response.put("message", "Producto guardado exitosamente");
            response.put("data", nuevoProducto);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al guardar producto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizarProducto(@PathVariable Long id,
                                                                  @RequestBody ProductoFinal producto) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validar que el producto existe
            Optional<ProductoFinal> productoExistente = productoFinalService.obtenerPorId(id);
            if (productoExistente.isEmpty()) {
                response.put("success", false);
                response.put("message", "Producto no encontrado con ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Validaciones
            if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El nombre del producto es obligatorio");
                return ResponseEntity.badRequest().body(response);
            }

            if (producto.getPrecio() == null || producto.getPrecio() <= 0) {
                response.put("success", false);
                response.put("message", "El precio debe ser mayor a 0");
                return ResponseEntity.badRequest().body(response);
            }

            // Establecer el ID para actualizar
            producto.setId(id);
            ProductoFinal productoActualizado = productoFinalService.actualizar(producto);

            response.put("success", true);
            response.put("message", "Producto actualizado exitosamente");
            response.put("data", productoActualizado);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar producto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminarProducto(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<ProductoFinal> productoExistente = productoFinalService.obtenerPorId(id);
            if (productoExistente.isEmpty()) {
                response.put("success", false);
                response.put("message", "Producto no encontrado con ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            productoFinalService.eliminar(id);

            response.put("success", true);
            response.put("message", "Producto eliminado correctamente");
            response.put("id", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar producto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/categoria/{tipo}")
    public ResponseEntity<Map<String, Object>> obtenerProductosPorCategoria(@PathVariable String tipo) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProductoFinal> productos = productoFinalService.obtenerTodos();

            List<ProductoFinal> productosFiltrados = productos.stream()
                    .filter(p -> p.getTipo() != null && p.getTipo().equalsIgnoreCase(tipo))
                    .toList();

            response.put("success", true);
            response.put("categoria", tipo);
            response.put("data", productosFiltrados);
            response.put("total", productosFiltrados.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener productos por categoría: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/destacados")
    public ResponseEntity<Map<String, Object>> obtenerProductosDestacados() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProductoFinal> productos = productoFinalService.obtenerTodos();

            // Obtener últimos 5 productos (o los primeros 5)
            List<ProductoFinal> destacados = productos.stream()
                    .limit(5)
                    .toList();

            response.put("success", true);
            response.put("data", destacados);
            response.put("total", destacados.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener productos destacados: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> contarProductos() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProductoFinal> productos = productoFinalService.obtenerTodos();

            response.put("success", true);
            response.put("count", productos.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al contar productos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/buscar")
    public ResponseEntity<Map<String, Object>> buscarProductos(@RequestParam String q) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProductoFinal> productos = productoFinalService.obtenerTodos();

            List<ProductoFinal> resultados = productos.stream()
                    .filter(p -> p.getNombre() != null &&
                            p.getNombre().toLowerCase().contains(q.toLowerCase()))
                    .toList();

            response.put("success", true);
            response.put("query", q);
            response.put("data", resultados);
            response.put("total", resultados.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al buscar productos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}