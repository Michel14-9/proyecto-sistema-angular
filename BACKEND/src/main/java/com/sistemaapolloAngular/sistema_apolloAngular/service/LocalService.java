package com.sistemaapolloAngular.sistema_apolloAngular.service;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Local;
import com.sistemaapolloAngular.sistema_apolloAngular.repository.LocalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalService {

    private final LocalRepository localRepository;

    public LocalService(LocalRepository localRepository) {
        this.localRepository = localRepository;
    }

    public List<Local> obtenerTodosLosLocales() {
        return localRepository.findAll();
    }

    public Local obtenerLocalPorId(Long id) {
        return localRepository.findById(id).orElse(null);
    }
}
