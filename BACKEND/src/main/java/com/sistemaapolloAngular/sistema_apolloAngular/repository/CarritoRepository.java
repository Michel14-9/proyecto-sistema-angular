package com.sistemaapolloAngular.sistema_apolloAngular.repository;

import com.sistemaapolloAngular.sistema_apolloAngular.model.CarritoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarritoRepository extends JpaRepository<CarritoItem, Long> {
    List<CarritoItem> findByUsuarioId(Long usuarioId);
    void deleteByUsuarioId(Long usuarioId);


}
