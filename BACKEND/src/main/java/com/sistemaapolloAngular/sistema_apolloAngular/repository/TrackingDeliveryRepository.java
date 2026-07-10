package com.sistemaapolloAngular.sistema_apolloAngular.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistemaapolloAngular.sistema_apolloAngular.model.TrackingDelivery;

@Repository
public interface TrackingDeliveryRepository extends JpaRepository<TrackingDelivery, Long> {

    Optional<TrackingDelivery> findByNumeroPedido(String numeroPedido);

    Optional<TrackingDelivery> findByPedidoId(Long pedidoId);

    List<TrackingDelivery> findByEstado(String estado);

    // Busca el tracking activo de un pedido (READY o ACTIVE)
    Optional<TrackingDelivery> findByNumeroPedidoAndEstadoIn(String numeroPedido, List<String> estados);
}
