package com.sistemaapolloAngular.sistema_apolloAngular.service;

import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.ProductoFinalRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoFinalService {

    private final ProductoFinalRepository productoFinalRepository;

    public ProductoFinalService(ProductoFinalRepository productoFinalRepository) {
        this.productoFinalRepository = productoFinalRepository;
    }


    public List<ProductoFinal> obtenerTodos() {
        return productoFinalRepository.findAll();
    }


    public Optional<ProductoFinal> obtenerPorId(Long id) {
        return productoFinalRepository.findById(id);
    }


    public ProductoFinal guardar(ProductoFinal producto) {
        return productoFinalRepository.save(producto);
    }


    public void eliminar(Long id) {
        productoFinalRepository.deleteById(id);
    }
    // Actualizar producto existente
    public ProductoFinal actualizar(ProductoFinal producto) {
        if (!productoFinalRepository.existsById(producto.getId())) {
            throw new RuntimeException("El producto con ID " + producto.getId() + " no existe");
        }
        return productoFinalRepository.save(producto);
    }

}
