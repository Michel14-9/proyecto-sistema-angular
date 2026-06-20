package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Favorito;
import com.sistemaapolloAngular.sistema_apolloAngular.service.FavoritoService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.UsuarioService;
import com.sistemaapolloAngular.sistema_apolloAngular.service.ProductoFinalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favoritos")
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
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Usuario no autenticado"
                ));
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            boolean yaEsFavorito = favoritoService.existeFavorito(usuario.getId(), productoId);

            if (yaEsFavorito) {
                favoritoService.eliminarFavoritoPorUsuarioYProducto(usuario.getId(), productoId);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "agregado", false,
                        "message", "Producto eliminado de favoritos"
                ));
            } else {
                ProductoFinal producto = productoFinalService.obtenerPorId(productoId)
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                Favorito favorito = favoritoService.agregarFavorito(usuario, producto);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "agregado", true,
                        "message", "Producto agregado a favoritos",
                        "favoritoId", favorito.getId()
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error al actualizar favoritos: " + e.getMessage()
            ));
        }
    }


    @GetMapping("/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarFavoritos(Authentication authentication) {
        try {
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

            List<Map<String, Object>> favoritosFormateados = favoritos.stream()
                    .map(favorito -> {
                        Map<String, Object> favMap = new HashMap<>();
                        favMap.put("id", favorito.getProducto().getId());
                        favMap.put("nombre", favorito.getProducto().getNombre());
                        favMap.put("precio", favorito.getProducto().getPrecio());
                        favMap.put("imagen", favorito.getProducto().getImagenUrl());
                        return favMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "favoritos", favoritosFormateados
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "favoritos", Collections.emptyList(),
                    "message", e.getMessage()
            ));
        }
    }


    @GetMapping
    @ResponseBody
    public ResponseEntity<?> obtenerMisFavoritos(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Usuario no autenticado"
                ));
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<Favorito> favoritos = favoritoService.obtenerFavoritosPorUsuario(usuario.getId());

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
                        }
                        return favoritoMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", favoritosResponse,
                    "total", favoritosResponse.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error al obtener favoritos: " + e.getMessage()
            ));
        }
    }


    @GetMapping("/verificar/{productoId}")
    @ResponseBody
    public ResponseEntity<?> verificarFavorito(@PathVariable Long productoId,
                                               Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(Map.of(
                        "esFavorito", false,
                        "authenticated", false
                ));
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            boolean esFavorito = favoritoService.existeFavorito(usuario.getId(), productoId);

            return ResponseEntity.ok(Map.of(
                    "esFavorito", esFavorito,
                    "authenticated", true,
                    "productoId", productoId
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "esFavorito", false,
                    "error", e.getMessage()
            ));
        }
    }


    @PostMapping("/agregar")
    @ResponseBody
    public ResponseEntity<?> agregarFavorito(@RequestParam Long productoId,
                                             Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Usuario no autenticado"
                ));
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            ProductoFinal producto = productoFinalService.obtenerPorId(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            boolean yaExiste = favoritoService.existeFavorito(usuario.getId(), productoId);
            if (yaExiste) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "success", false,
                        "message", "El producto ya está en favoritos"
                ));
            }

            Favorito favorito = favoritoService.agregarFavorito(usuario, producto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Producto agregado a favoritos",
                    "favoritoId", favorito.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error al agregar favorito: " + e.getMessage()
            ));
        }
    }


    @DeleteMapping("/eliminar/{favoritoId}")
    @ResponseBody
    public ResponseEntity<?> eliminarFavorito(@PathVariable Long favoritoId,
                                              Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Usuario no autenticado"
                ));
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Favorito favorito = favoritoService.obtenerFavoritoPorId(favoritoId)
                    .orElseThrow(() -> new RuntimeException("Favorito no encontrado"));

            if (!favorito.getUsuario().getId().equals(usuario.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "message", "No tienes permisos para eliminar este favorito"
                ));
            }

            favoritoService.eliminarFavorito(favoritoId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Favorito eliminado correctamente",
                    "favoritoId", favoritoId
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error al eliminar favorito: " + e.getMessage()
            ));
        }
    }


    @DeleteMapping("/limpiar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> limpiarFavoritos(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Usuario no autenticado"
                ));
            }

            String correo = authentication.getName();
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            int favoritosEliminados = favoritoService.eliminarTodosLosFavoritos(usuario.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Todos los favoritos han sido eliminados",
                    "eliminados", favoritosEliminados
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error al limpiar favoritos: " + e.getMessage()
            ));
        }
    }


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
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "count", 0,
                    "error", e.getMessage()
            ));
        }
    }


    @GetMapping("/auth/check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkAuth(Authentication authentication) {
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();

        return ResponseEntity.ok(Map.of(
                "authenticated", isAuthenticated,
                "username", isAuthenticated ? authentication.getName() : null
        ));
    }
}