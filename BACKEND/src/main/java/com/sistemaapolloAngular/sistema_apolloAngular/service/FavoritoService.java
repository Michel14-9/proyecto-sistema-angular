package com.sistemaapolloAngular.sistema_apolloAngular.service;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Favorito;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.FavoritoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FavoritoService {

    @Autowired
    private FavoritoRepository favoritoRepository;

    // Obtener todos los favoritos activos de un usuario
    public List<Favorito> obtenerFavoritosPorUsuario(Long usuarioId) {
        System.out.println(" Buscando favoritos para usuario ID: " + usuarioId);
        List<Favorito> favoritos = favoritoRepository.findByUsuarioIdAndActivoTrue(usuarioId);
        System.out.println(" Favoritos encontrados: " + favoritos.size());
        return favoritos;
    }

    // Agregar producto a favoritos
    @Transactional
    public Favorito agregarFavorito(Usuario usuario, ProductoFinal producto) {
        System.out.println("Agregando favorito - Usuario: " + usuario.getUsername() + ", Producto: " + producto.getNombre());

        // Verificar si ya existe
        boolean yaExiste = favoritoRepository.existsByUsuarioIdAndProductoIdAndActivoTrue(usuario.getId(), producto.getId());
        if (yaExiste) {
            System.out.println(" El producto ya está en favoritos");
            // Reactivar si estaba desactivado
            Optional<Favorito> favoritoExistente = favoritoRepository.findByUsuarioIdAndProductoId(usuario.getId(), producto.getId());
            if (favoritoExistente.isPresent()) {
                Favorito favorito = favoritoExistente.get();
                favorito.setActivo(true);
                favorito.setFechaAgregado(LocalDateTime.now());
                return favoritoRepository.save(favorito);
            }
        }

        // Crear nuevo favorito
        Favorito favorito = new Favorito();
        favorito.setUsuario(usuario);
        favorito.setProducto(producto);
        favorito.setFechaAgregado(LocalDateTime.now());
        favorito.setActivo(true);

        Favorito saved = favoritoRepository.save(favorito);
        System.out.println(" Favorito agregado - ID: " + saved.getId());
        return saved;
    }

    // Eliminar favorito (soft delete)
    @Transactional
    public void eliminarFavorito(Long favoritoId) {
        System.out.println(" Eliminando favorito ID: " + favoritoId);
        Optional<Favorito> favoritoOpt = favoritoRepository.findById(favoritoId);

        if (favoritoOpt.isPresent()) {
            Favorito favorito = favoritoOpt.get();
            favorito.setActivo(false);
            favoritoRepository.save(favorito);
            System.out.println(" Favorito desactivado: " + favoritoId);
        } else {
            System.out.println(" Favorito no encontrado: " + favoritoId);
            throw new RuntimeException("Favorito no encontrado");
        }
    }


    @Transactional
    public boolean eliminarFavoritoPorUsuarioYProducto(Long usuarioId, Long productoId) {
        System.out.println(" Eliminando favorito - Usuario: " + usuarioId + ", Producto: " + productoId);
        try {
            Optional<Favorito> favorito = favoritoRepository.findByUsuarioIdAndProductoId(usuarioId, productoId);
            if (favorito.isPresent()) {
                Favorito fav = favorito.get();
                fav.setActivo(false);
                favoritoRepository.save(fav);
                System.out.println(" Favorito eliminado por usuario y producto");
                return true;
            }
            System.out.println(" Favorito no encontrado para eliminar");
            return false;
        } catch (Exception e) {
            System.err.println(" Error eliminando favorito: " + e.getMessage());
            return false;
        }
    }

    // Eliminar todos los favoritos de un usuario
    @Transactional
    public int eliminarTodosLosFavoritos(Long usuarioId) {
        System.out.println(" Eliminando todos los favoritos del usuario ID: " + usuarioId);
        int eliminados = favoritoRepository.desactivarByUsuarioId(usuarioId);
        System.out.println(" Favoritos eliminados: " + eliminados);
        return eliminados;
    }

    // Verificar si un producto está en favoritos
    public boolean existeFavorito(Long usuarioId, Long productoId) {
        return favoritoRepository.existsByUsuarioIdAndProductoIdAndActivoTrue(usuarioId, productoId);
    }

    // Obtener favorito por ID
    public Optional<Favorito> obtenerFavoritoPorId(Long favoritoId) {
        return favoritoRepository.findById(favoritoId);
    }

    // Obtener favorito por usuario y producto
    public Optional<Favorito> obtenerFavoritoPorUsuarioYProducto(Long usuarioId, Long productoId) {
        return favoritoRepository.findByUsuarioIdAndProductoId(usuarioId, productoId);
    }

    // Contar favoritos de un usuario
    public int contarFavoritosPorUsuario(Long usuarioId) {
        return favoritoRepository.countByUsuarioIdAndActivoTrue(usuarioId);
    }
}
