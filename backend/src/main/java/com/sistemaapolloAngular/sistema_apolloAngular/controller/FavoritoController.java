package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Favorito;
import com.sistemaapolloAngular.sistema_apolloAngular.service.FavoritoService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.UsuarioService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.ProductoFinalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/favoritos")
public class FavoritoController {

    private final FavoritoService favoritoService;
    private final UsuarioService usuarioService;
    private final ProductoFinalService productoFinalService;

    public FavoritoController(FavoritoService favoritoService,
                              UsuarioService usuarioService,
                              ProductoFinalService productoFinalService) {
        this.favoritoService = favoritoService;
        this.usuarioService = usuarioService;
        this.productoFinalService = productoFinalService;
    }


    @PostMapping("/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleFavorito(@RequestParam Long productoId,
                                                              Authentication authentication) {
        try {
            System.out.println("=== TOGGLE FAVORITO - productoId: " + productoId + " ===");

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Usuario no autenticado"
                ));
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Verificar si ya es favorito
            boolean yaEsFavorito = favoritoService.existeFavorito(usuario.getId(), productoId);

            if (yaEsFavorito) {
                // Eliminar de favoritos
                favoritoService.eliminarFavoritoPorUsuarioYProducto(usuario.getId(), productoId);
                System.out.println("Favorito eliminado - Usuario: " + correo + ", Producto: " + productoId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "agregado", false,
                        "message", "Producto eliminado de favoritos"
                ));
            } else {
                // Agregar a favoritos
                ProductoFinal producto = productoFinalService.obtenerPorId(productoId)
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                Favorito favorito = favoritoService.agregarFavorito(usuario, producto);
                System.out.println(" Favorito agregado - Usuario: " + correo + ", Producto: " + producto.getNombre());

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "agregado", true,
                        "message", "Producto agregado a favoritos",
                        "favoritoId", favorito.getId()
                ));
            }

        } catch (Exception e) {
            System.err.println("💥 ERROR en toggleFavorito: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error al actualizar favoritos: " + e.getMessage()
            ));
        }
    }


    @GetMapping("/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarFavoritos(Authentication authentication) {
        try {
            System.out.println("=== LISTANDO FAVORITOS ===");

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "favoritos", Collections.emptyList()
                ));
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<Favorito> favoritos = favoritoService.obtenerFavoritosPorUsuario(usuario.getId());

            // Formatear respuesta para el JavaScript
            List<Map<String, Object>> favoritosFormateados = favoritos.stream()
                    .map(favorito -> {
                        Map<String, Object> favMap = new HashMap<>();
                        favMap.put("id", favorito.getProducto().getId());
                        favMap.put("nombre", favorito.getProducto().getNombre());
                        return favMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "favoritos", favoritosFormateados
            ));

        } catch (Exception e) {
            System.err.println("💥 ERROR en listarFavoritos: " + e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "favoritos", Collections.emptyList(),
                    "message", e.getMessage()
            ));
        }
    }

    // ENDPOINT PARA CONTAR FAVORITOS
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> contarFavoritos(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "count", 0
                ));
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            int cantidad = favoritoService.contarFavoritosPorUsuario(usuario.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", cantidad
            ));

        } catch (Exception e) {
            System.err.println(" ERROR en contarFavoritos: " + e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "count", 0
            ));
        }
    }




    @GetMapping("/api/auth/check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkAuth(Authentication authentication) {
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();

        return ResponseEntity.ok(Map.of(
                "authenticated", isAuthenticated,
                "username", isAuthenticated ? authentication.getName() : null
        ));
    }



    @GetMapping
    public String mostrarFavoritos(Authentication authentication, Model model) {
        try {
            System.out.println("=== CARGANDO PÁGINA DE FAVORITOS ===");

            if (authentication == null || !authentication.isAuthenticated()) {
                return "mis-favoritos";
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            model.addAttribute("usuario", usuario);
            System.out.println(" Página de favoritos cargada para: " + correo);

            return "mis-favoritos";

        } catch (Exception e) {
            System.err.println(" Error cargando página de favoritos: " + e.getMessage());
            model.addAttribute("error", "Error al cargar los favoritos");
            return "mis-favoritos";
        }
    }

    @GetMapping("/api/favoritos")
    @ResponseBody
    public ResponseEntity<?> obtenerMisFavoritos(Authentication authentication) {
        try {
            System.out.println("=== SOLICITANDO FAVORITOS DEL USUARIO ===");

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<Favorito> favoritos = favoritoService.obtenerFavoritosPorUsuario(usuario.getId());
            System.out.println(" Favoritos encontrados: " + favoritos.size());

            List<Map<String, Object>> favoritosResponse = favoritos.stream()
                    .map(favorito -> {
                        Map<String, Object> favoritoMap = new HashMap<>();
                        favoritoMap.put("id", favorito.getId());
                        favoritoMap.put("fechaAgregado", favorito.getFechaAgregado());
                        favoritoMap.put("esActivo", favorito.isActivo());

                        if (favorito.getProducto() != null) {
                            ProductoFinal producto = favorito.getProducto();
                            Map<String, Object> productoMap = new HashMap<>();
                            productoMap.put("id", producto.getId());
                            productoMap.put("nombre", producto.getNombre());
                            productoMap.put("precio", producto.getPrecio());
                            productoMap.put("descripcion", producto.getDescripcion());
                            productoMap.put("imagen", producto.getImagenUrl());
                            productoMap.put("disponible", true);
                            productoMap.put("categoria", producto.getTipo());
                            productoMap.put("tiempoPreparacion", 25);

                            favoritoMap.put("producto", productoMap);
                        } else {
                            Map<String, Object> productoMap = new HashMap<>();
                            productoMap.put("id", 0);
                            productoMap.put("nombre", "Producto no disponible");
                            productoMap.put("precio", 0.0);
                            productoMap.put("descripcion", "Este producto ya no está disponible");
                            productoMap.put("imagen", "/archivos/placeholder.jpg");
                            productoMap.put("disponible", false);
                            productoMap.put("categoria", "No disponible");
                            productoMap.put("tiempoPreparacion", 0);

                            favoritoMap.put("producto", productoMap);
                        }

                        return favoritoMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(favoritosResponse);

        } catch (Exception e) {
            System.err.println(" ERROR en obtenerMisFavoritos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error interno del servidor");
        }
    }

    @PostMapping("/api/agregar")
    @ResponseBody
    public ResponseEntity<?> agregarFavorito(@RequestParam Long productoId,
                                             Authentication authentication) {
        try {
            System.out.println("=== AGREGANDO A FAVORITOS ===");

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            ProductoFinal producto = productoFinalService.obtenerPorId(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + productoId));

            boolean yaExiste = favoritoService.existeFavorito(usuario.getId(), productoId);
            if (yaExiste) {
                return ResponseEntity.status(409).body("El producto ya está en favoritos");
            }

            Favorito favorito = favoritoService.agregarFavorito(usuario, producto);
            System.out.println(" Producto agregado a favoritos: " + producto.getNombre());

            return ResponseEntity.ok().body(Map.of(
                    "mensaje", "Producto agregado a favoritos",
                    "favoritoId", favorito.getId()
            ));

        } catch (Exception e) {
            System.err.println(" ERROR agregando favorito: " + e.getMessage());
            return ResponseEntity.status(500).body("Error al agregar a favoritos: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/eliminar/{favoritoId}")
    @ResponseBody
    public ResponseEntity<?> eliminarFavorito(@PathVariable Long favoritoId,
                                              Authentication authentication) {
        try {
            System.out.println("=== ELIMINANDO DE FAVORITOS ===");

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Favorito favorito = favoritoService.obtenerFavoritoPorId(favoritoId)
                    .orElseThrow(() -> new RuntimeException("Favorito no encontrado"));

            if (!favorito.getUsuario().getId().equals(usuario.getId())) {
                return ResponseEntity.status(403).body("No tienes permisos para eliminar este favorito");
            }

            favoritoService.eliminarFavorito(favoritoId);
            System.out.println(" Favorito eliminado: " + favoritoId);

            return ResponseEntity.ok().body("Favorito eliminado correctamente");

        } catch (Exception e) {
            System.err.println(" ERROR eliminando favorito: " + e.getMessage());
            return ResponseEntity.status(500).body("Error al eliminar de favoritos");
        }
    }

    @DeleteMapping("/api/limpiar")
    @ResponseBody
    public ResponseEntity<?> limpiarFavoritos(Authentication authentication) {
        try {
            System.out.println("=== LIMPIANDO TODOS LOS FAVORITOS ===");

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            int favoritosEliminados = favoritoService.eliminarTodosLosFavoritos(usuario.getId());
            System.out.println(" Favoritos eliminados: " + favoritosEliminados);

            return ResponseEntity.ok().body(Map.of(
                    "mensaje", "Todos los favoritos han sido eliminados",
                    "eliminados", favoritosEliminados
            ));

        } catch (Exception e) {
            System.err.println(" ERROR limpiando favoritos: " + e.getMessage());
            return ResponseEntity.status(500).body("Error al limpiar favoritos");
        }
    }

    @GetMapping("/api/verificar/{productoId}")
    @ResponseBody
    public ResponseEntity<?> verificarFavorito(@PathVariable Long productoId,
                                               Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(false);
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            boolean esFavorito = favoritoService.existeFavorito(usuario.getId(), productoId);
            return ResponseEntity.ok(esFavorito);

        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }
    // En FavoritoController.java
    @PostMapping("/limpiar-todos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> limpiarTodosFavoritos(Authentication authentication) {
        try {
            System.out.println("=== LIMPIANDO TODOS LOS FAVORITOS ===");

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Usuario no autenticado"
                ));
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            int favoritosEliminados = favoritoService.eliminarTodosLosFavoritos(usuario.getId());
            System.out.println(" Favoritos eliminados: " + favoritosEliminados);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Todos los favoritos han sido eliminados",
                    "eliminados", favoritosEliminados
            ));

        } catch (Exception e) {
            System.err.println(" ERROR limpiando favoritos: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error al limpiar favoritos"
            ));
        }
    }


}
