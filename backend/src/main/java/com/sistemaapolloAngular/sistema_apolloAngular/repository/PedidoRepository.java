package com.sistemaapolloAngular.sistema_apolloAngular.repository;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByCanalAndNumero(String canal, String numero);

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.items WHERE p.id = :id")
    Optional<Pedido> findByIdWithItems(@Param("id") Long id);


    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.items i LEFT JOIN FETCH i.producto ORDER BY p.fecha DESC")
    List<Pedido> findAllWithItemsAndProducts();

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.items ORDER BY p.fecha DESC")
    List<Pedido> findAllWithItems();

    @Query("SELECT p FROM Pedido p WHERE p.usuario.id = :usuarioId ORDER BY p.fecha DESC")
    List<Pedido> findByUsuarioIdOrderByFechaDesc(@Param("usuarioId") Long usuarioId);

    @Query("SELECT p FROM Pedido p WHERE p.numero = :numero")
    Optional<Pedido> findByNumero(@Param("numero") String numero);


    @Query("SELECT p FROM Pedido p WHERE p.estado = :estado ORDER BY p.fecha DESC")
    List<Pedido> findByEstadoOrderByFechaDesc(@Param("estado") String estado);

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.items WHERE p.estado = :estado ORDER BY p.fecha ASC")
    List<Pedido> findByEstadoWithItems(@Param("estado") String estado);

    @Query("SELECT p FROM Pedido p WHERE p.estado = :estado ORDER BY p.fecha ASC")
    List<Pedido> findByEstadoOrderByFechaAsc(@Param("estado") String estado);

    List<Pedido> findAllByOrderByFechaDesc();

    Optional<Pedido> findByNumeroPedido(String numeroPedido);
}
