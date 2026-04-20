package com.sistemaapolloAngular.sistema_apolloAngular.service;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.CarritoItem;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.PedidoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }


    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorUsuarioConItems(Long usuarioId) {
        try {
            System.out.println(" Buscando pedidos para usuario ID: " + usuarioId);


            String jpql = "SELECT DISTINCT p FROM Pedido p " +
                    "LEFT JOIN FETCH p.items i " +
                    "LEFT JOIN FETCH i.producto pf " +
                    "WHERE p.usuario.id = :usuarioId " +
                    "ORDER BY p.fecha DESC"; //

            System.out.println(" Ejecutando JPQL: " + jpql);

            List<Pedido> pedidos = entityManager.createQuery(jpql, Pedido.class)
                    .setParameter("usuarioId", usuarioId)
                    .getResultList();

            System.out.println("Pedidos cargados con items: " + pedidos.size());

            // Debug detallado
            for (Pedido pedido : pedidos) {
                System.out.println(" Pedido ID: " + pedido.getId() +
                        ", Estado: " + pedido.getEstado() +
                        ", Fecha: " + pedido.getFecha() +
                        ", Items: " + pedido.getItems().size());

                for (ItemPedido item : pedido.getItems()) {
                    String nombreProducto = item.getProductoFinal() != null ?
                            item.getProductoFinal().getNombre() : item.getNombreProducto();
                    System.out.println("   🛒 Item: " + nombreProducto + " x" + item.getCantidad());
                }
            }

            return pedidos;

        } catch (Exception e) {
            System.err.println(" ERROR en obtenerPedidosPorUsuarioConItems: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al cargar pedidos: " + e.getMessage(), e);
        }
    }


    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorUsuarioEager(Long usuarioId) {
        try {
            List<Pedido> pedidos = pedidoRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);

            // Forzar la inicialización de las relaciones lazy
            for (Pedido pedido : pedidos) {
                // Inicializar items
                pedido.getItems().size(); // Esto fuerza la carga

                // Inicializar producto en cada item
                for (ItemPedido item : pedido.getItems()) {
                    if (item.getProductoFinal() != null) {
                        item.getProductoFinal().getNombre(); // Fuerza la carga
                    }
                }
            }

            System.out.println(" Pedidos inicializados: " + pedidos.size());
            return pedidos;

        } catch (Exception e) {
            System.err.println(" Error en obtenerPedidosPorUsuarioEager: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Optional<Pedido> buscarPedido(String canal, String numero) {
        return pedidoRepository.findByCanalAndNumero(canal, numero);
    }

    public List<Pedido> obtenerTodosLosPedidos() {
        return pedidoRepository.findAllWithItemsAndProducts();
    }

    // CREAR NUEVO PEDIDO
    @Transactional
    public Pedido crearPedido(Usuario usuario, List<CarritoItem> carrito, Map<String, Object> datosPedido) {
        try {
            // Crear nuevo pedido
            Pedido pedido = new Pedido();
            pedido.setUsuario(usuario);
            pedido.setEstado("PENDIENTE");
            pedido.setMetodoPago((String) datosPedido.get("metodoPago"));
            pedido.setTipoEntrega((String) datosPedido.get("tipoEntrega"));
            pedido.setDireccionEntrega((String) datosPedido.get("direccion"));
            pedido.setInstrucciones((String) datosPedido.get("instrucciones"));
            pedido.setObservaciones((String) datosPedido.get("observaciones"));

            for (CarritoItem carritoItem : carrito) {
                ItemPedido itemPedido = new ItemPedido();
                itemPedido.setNombreProducto(carritoItem.getProducto().getNombre());
                itemPedido.setCantidad(carritoItem.getCantidad());
                itemPedido.setPrecio(carritoItem.getPrecioUnitario());
                itemPedido.setSubtotal(carritoItem.getPrecioUnitario() * carritoItem.getCantidad());

                itemPedido.setProductoFinal(carritoItem.getProducto()); // Guarda la relación real

                pedido.agregarItem(itemPedido);
            }

            pedido.calcularTotales();

            Pedido pedidoGuardado = pedidoRepository.save(pedido);

            String numeroGenerado = "LR" + String.format("%06d", pedidoGuardado.getId());
            pedidoGuardado.setNumero(numeroGenerado);
            pedidoGuardado.setNumeroPedido(numeroGenerado);
            pedidoGuardado.setCanal("WEB");

            return pedidoRepository.save(pedidoGuardado);

        } catch (Exception e) {
            throw new RuntimeException("Error al crear el pedido: " + e.getMessage(), e);
        }
    }

    public Optional<Pedido> obtenerPedidoPorId(Long pedidoId) {
        return pedidoRepository.findById(pedidoId);
    }

    // OBTENER PEDIDOS POR USUARIO (versión original)
    public List<Pedido> obtenerPedidosPorUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);
    }

    @Transactional
    public Pedido actualizarEstadoPedido(Long pedidoId, String nuevoEstado) {
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
        if (pedidoOpt.isPresent()) {
            Pedido pedido = pedidoOpt.get();
            pedido.setEstado(nuevoEstado);
            return pedidoRepository.save(pedido);
        } else {
            throw new RuntimeException("Pedido no encontrado");
        }
    }

    public Optional<Pedido> buscarPorNumeroPedido(String numeroPedido) {
        return pedidoRepository.findByNumeroPedido(numeroPedido);
    }
}
