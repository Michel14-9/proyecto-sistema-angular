package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.model.Local;
import com.sistemaapolloAngular.sistema_apolloAngular.service.LocalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class LocalController {

    private final LocalService localService;

    public LocalController(LocalService localService) {
        this.localService = localService;
    }

    // Muestra la página principal de "nuestros locales" y lista todos
    @GetMapping("/nuestros-locales")
    public String mostrarLocales(Model model) {
        List<Local> locales = localService.obtenerTodosLosLocales();
        model.addAttribute("locales", locales);
        return "locales"; // Thymeleaf: nuestros-locales.html
    }

    // Muestra detalle de un local específico
    @GetMapping("/nuestros-locales/{id}")
    public String verLocal(@PathVariable Long id, Model model) {
        Local local = localService.obtenerLocalPorId(id);
        model.addAttribute("local", local);
        return "detalle-local"; // Thymeleaf: detalle-local.html
    }
}
