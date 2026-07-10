package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.service.ProductoFinalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final ProductoFinalService productoFinalService;

    public MenuController(ProductoFinalService productoFinalService) {
        this.productoFinalService = productoFinalService;
    }


    @GetMapping("/todos")
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


    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<Map<String, Object>> obtenerProductosPorCategoria(@PathVariable String categoria) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProductoFinal> todos = productoFinalService.obtenerTodos();

            List<ProductoFinal> productos = todos.stream()
                    .filter(p -> p.getTipo() != null && p.getTipo().equalsIgnoreCase(categoria))
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("categoria", categoria);
            response.put("data", productos);
            response.put("total", productos.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener productos por categoría: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    // En MenuController.java - método obtenerCombos()

    @GetMapping("/combos")
    public ResponseEntity<Map<String, Object>> obtenerCombos() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProductoFinal> todosProductos = productoFinalService.obtenerTodos();

            // ✅ Aceptar "COMBO", "Combo", "combos", "combo"
            List<ProductoFinal> combos = todosProductos.stream()
                    .filter(p -> p.getTipo() != null)
                    .filter(p -> p.getTipo().matches("(?i)^combo(s?)$")) // Regex: combo o combos (insensible a mayúsculas)
                    .filter(p -> p.isActivo()) // Solo activos
                    .collect(Collectors.toList());

            // Si no hay combos, mostrar productos destacados
            if (combos.isEmpty()) {
                System.out.println("⚠️ No hay combos, mostrando productos destacados...");
                combos = todosProductos.stream()
                        .filter(p -> p.isActivo())
                        .limit(5)
                        .collect(Collectors.toList());
            }

            System.out.println("✅ Combos encontrados: " + combos.size());

            response.put("success", true);
            response.put("data", combos);
            response.put("total", combos.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener combos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/completo")
    public ResponseEntity<Map<String, Object>> obtenerMenuCompleto() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProductoFinal> todosProductos = productoFinalService.obtenerTodos();

            // Organizar por categorías
            Map<String, List<ProductoFinal>> menuPorCategoria = new LinkedHashMap<>();

            // Categorías predefinidas (en el orden deseado)
            String[] categorias = {"pollos", "parrillas", "chicharron", "broaster", "hamburguesas", "criollos", "combos"};

            for (String categoria : categorias) {
                List<ProductoFinal> productos = todosProductos.stream()
                        .filter(p -> p.getTipo() != null && p.getTipo().equalsIgnoreCase(categoria))
                        .collect(Collectors.toList());

                if (!productos.isEmpty()) {
                    menuPorCategoria.put(categoria, productos);
                }
            }

            // Si hay productos con categorías no listadas, agregarlas al final
            Set<String> categoriasEncontradas = todosProductos.stream()
                    .map(ProductoFinal::getTipo)
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            for (String categoria : categoriasEncontradas) {
                if (!menuPorCategoria.containsKey(categoria)) {
                    List<ProductoFinal> productos = todosProductos.stream()
                            .filter(p -> p.getTipo() != null && p.getTipo().equalsIgnoreCase(categoria))
                            .collect(Collectors.toList());
                    menuPorCategoria.put(categoria, productos);
                }
            }

            // Calcular total de productos
            int totalProductos = menuPorCategoria.values().stream()
                    .mapToInt(List::size)
                    .sum();

            response.put("success", true);
            response.put("menu", menuPorCategoria);
            response.put("totalProductos", totalProductos);
            response.put("categorias", menuPorCategoria.keySet());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener menú completo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/producto/{id}")
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


    @GetMapping("/categorias")
    public ResponseEntity<Map<String, Object>> obtenerCategorias() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProductoFinal> productos = productoFinalService.obtenerTodos();

            Set<String> categorias = productos.stream()
                    .map(ProductoFinal::getTipo)
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            response.put("success", true);
            response.put("categorias", categorias);
            response.put("total", categorias.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener categorías: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}