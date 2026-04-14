package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginasController {

    // Página principal o home
    @GetMapping("/")
    public String home() {
        return "index"; // templates/index.html
    }

    // Página de locales
    @GetMapping("/locales")
    public String locales() {
        return "locales"; // templates/locales.html
    }

    // Registro de usuario
    @GetMapping("/registrate")
    public String registrate() {
        return "registrate"; // templates/registrate.html
    }







    // Mis cuentas
    @GetMapping("/cuenta")
    public String cuenta() {
        return "mis-cuentas"; // templates/mis-cuentas.html
    }

    // Mis datos
    @GetMapping("/datos")
    public String datos() {
        return "mis-datos"; // templates/mis-datos.html
    }

    // Mis direcciones
    @GetMapping("/direcciones")
    public String direcciones() {
        return "mis-direcciones"; // templates/mis-direcciones.html
    }

    // Mis pedidos
    @GetMapping("/pedidos")
    public String pedidos() {
        return "mis-pedidos"; // templates/mis-pedidos.html
    }

    // Crear un pedido
    @GetMapping("/crear-pedido")
    public String crearPedido() {
        return "crear-pedido"; // templates/crear-pedido.html
    }



    // Confirmación de pedido
    @GetMapping("/confirmacion-pedido")
    public String confirmacionPedido() {
        return "confirmacion-pedido"; // templates/confirmacion-pedido.html
    }


}
