package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PostLoginController {

    @GetMapping("/postLogin")
    public String postLogin(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Obtiene el rol del usuario
        String rol = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        // TODOS LOS CASOS AQUÍ
        switch (rol) {
            case "ROLE_ADMIN":
                return "redirect:/admin-menu";
            case "ROLE_CAJERO":
                return "redirect:/cajero";
            case "ROLE_COCINERO":
                return "redirect:/cocinero";
            case "ROLE_DELIVERY":
                return "redirect:/delivery";
            case "ROLE_CLIENTE":
                return "redirect:/menu";
            default:
                return "redirect:/";
        }
    }
}
