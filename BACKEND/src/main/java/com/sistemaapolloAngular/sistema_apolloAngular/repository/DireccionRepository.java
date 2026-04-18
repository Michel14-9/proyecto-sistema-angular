package com.sistemaapolloAngular.sistema_apolloAngular.repository;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Direccion;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    List<Direccion> findByUsuario(Usuario usuario);

    List<Direccion> findByUsuarioAndPredeterminadaTrue(Usuario usuario);

    List<Direccion> findByUsuarioAndFacturacionTrue(Usuario usuario);
}
