package tracking.apollo.backendtracking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tracking.apollo.backendtracking.model.TrackingDelivery;

@Repository
public interface TrackingDeliveryRepository extends JpaRepository<TrackingDelivery, Long> {

    Optional<TrackingDelivery> findTopByNumeroPedidoOrderByFechaCreacionDesc(String numeroPedido);

    Optional<TrackingDelivery> findByPedidoId(Long pedidoId);

    Optional<TrackingDelivery> findByNumeroPedidoAndEstadoIn(String numeroPedido, List<String> estados);

    List<TrackingDelivery> findByEstado(String estado);
}