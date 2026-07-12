package com.sistemaapolloAngular.sistema_apolloAngular.repository;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    // ✅ CORREGIDO: Agregar JOIN FETCH para cargar los productos
    @Query("SELECT f FROM Favorito f JOIN FETCH f.producto WHERE f.usuario.id = :usuarioId AND f.activo = true ORDER BY f.fechaAgregado DESC")
    List<Favorito> findByUsuarioIdAndActivoTrue(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(f) > 0 FROM Favorito f WHERE f.usuario.id = :usuarioId AND f.producto.id = :productoId AND f.activo = true")
    boolean existsByUsuarioIdAndProductoIdAndActivoTrue(@Param("usuarioId") Long usuarioId, @Param("productoId") Long productoId);

    @Query("SELECT f FROM Favorito f WHERE f.usuario.id = :usuarioId AND f.producto.id = :productoId AND f.activo = true")
    Optional<Favorito> findByUsuarioIdAndProductoId(@Param("usuarioId") Long usuarioId, @Param("productoId") Long productoId);

    @Modifying
    @Query("UPDATE Favorito f SET f.activo = false WHERE f.usuario.id = :usuarioId AND f.activo = true")
    int desactivarByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("DELETE FROM Favorito f WHERE f.usuario.id = :usuarioId")
    int deleteByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(f) FROM Favorito f WHERE f.usuario.id = :usuarioId AND f.activo = true")
    int countByUsuarioIdAndActivoTrue(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // ✅ NUEVOS MÉTODOS PARA EL ADMIN CONTROLLER
    // ============================================================

    /**
     * Obtener todos los favoritos con usuario y producto (para Python)
     */
    @Query("SELECT f FROM Favorito f JOIN FETCH f.usuario u JOIN FETCH f.producto p WHERE f.activo = true ORDER BY f.fechaAgregado DESC")
    List<Favorito> findAllWithUsuarioAndProducto();

    /**
     * Obtener favoritos por ID de usuario con producto
     */
    @Query("SELECT f FROM Favorito f JOIN FETCH f.producto WHERE f.usuario.id = :usuarioId AND f.activo = true")
    List<Favorito> findFavoritosByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Contar favoritos por producto
     */
    @Query("SELECT COUNT(f) FROM Favorito f WHERE f.producto.id = :productoId AND f.activo = true")
    long countByProductoId(@Param("productoId") Long productoId);
}