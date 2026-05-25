package com.sistemaapolloAngular.sistema_apolloAngular.service;

import com.sistemaapolloAngular.sistema_apolloAngular.model.CarritoItem;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.CarritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;

    public CarritoService(CarritoRepository carritoRepository) {
        this.carritoRepository = carritoRepository;
    }

    // Agregar producto
    @Transactional
    public void agregarProducto(Usuario usuario, ProductoFinal producto, int cantidad, double precioUnitario) {
        CarritoItem item = new CarritoItem();
        item.setUsuario(usuario);
        item.setProducto(producto);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(precioUnitario);
        carritoRepository.save(item);
    }

    // Obtener carrito por usuario
    public List<CarritoItem> obtenerCarrito(Long usuarioId) {
        return carritoRepository.findByUsuarioId(usuarioId);
    }

    // Actualizar cantidad de un producto en el carrito
    @Transactional
    public void actualizarCantidad(Long itemId, int nuevaCantidad) {
        Optional<CarritoItem> itemOpt = carritoRepository.findById(itemId);
        if (itemOpt.isPresent()) {
            CarritoItem item = itemOpt.get();

            // Validar cantidad
            if (nuevaCantidad < 1) {
                throw new RuntimeException("La cantidad debe ser al menos 1");
            }

            if (nuevaCantidad > 50) {
                throw new RuntimeException("La cantidad máxima es 50");
            }

            item.setCantidad(nuevaCantidad);
            carritoRepository.save(item);
        } else {
            throw new RuntimeException("Item del carrito no encontrado");
        }
    }

    // Eliminar un producto del carrito
    @Transactional
    public void eliminarProducto(Long itemId) {
        carritoRepository.deleteById(itemId);
    }

    // Vaciar carrito (alias de limpiarCarrito)
    @Transactional
    public void vaciarCarrito(Long usuarioId) {
        carritoRepository.deleteByUsuarioId(usuarioId);
    }


    @Transactional
    public void limpiarCarrito(Long usuarioId) {
        carritoRepository.deleteByUsuarioId(usuarioId);
    }


    public boolean estaVacio(Long usuarioId) {
        return carritoRepository.findByUsuarioId(usuarioId).isEmpty();
    }


    public int obtenerTotalItems(Long usuarioId) {
        List<CarritoItem> carrito = carritoRepository.findByUsuarioId(usuarioId);
        return carrito.stream().mapToInt(CarritoItem::getCantidad).sum();
    }
}
