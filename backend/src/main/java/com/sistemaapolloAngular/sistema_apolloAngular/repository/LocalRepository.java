package com.sistemaapolloAngular.sistema_apolloAngular.repository;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalRepository extends JpaRepository<Local, Long> {

    // Encontrar locales activos
    List<Local> findByActivoTrue();

    // Encontrar por nombre (búsqueda parcial)
    List<Local> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    // Encontrar por distrito
    List<Local> findByDistritoContainingIgnoreCaseAndActivoTrue(String distrito);

    // Encontrar locales con delivery
    List<Local> findByDeliveryTrueAndActivoTrue();

    // Encontrar locales con retiro en tienda
    List<Local> findByRetiroEnTiendaTrueAndActivoTrue();

    // Ordenar por orden visual
    List<Local> findByActivoTrueOrderByOrdenVisualAsc();

    // Verificar si existe un local con el mismo nombre
    boolean existsByNombreAndActivoTrue(String nombre);

    // Encontrar por teléfono
    Optional<Local> findByTelefono(String telefono);
}

