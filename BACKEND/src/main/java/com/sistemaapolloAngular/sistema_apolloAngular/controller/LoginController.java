package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        // Manejar mensajes de error
        if (error != null) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
        }

        // Manejar mensaje de logout exitoso
        if (logout != null) {
            model.addAttribute("message", "Has cerrado sesión exitosamente");
        }

        return "login";
    }
}
