package com.sistemaapolloAngular.sistema_apolloAngular.repository;

import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoFinalRepository extends JpaRepository<ProductoFinal, Long> {
}
