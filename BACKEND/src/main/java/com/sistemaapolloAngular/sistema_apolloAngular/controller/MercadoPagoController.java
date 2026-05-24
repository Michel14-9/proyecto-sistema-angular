package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.dto.PagoRequest;
import com.sistemaapolloAngular.sistema_apolloAngular.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class MercadoPagoController {

    private final PagoService pagoService;

    @PostMapping("/crear")
    public String crearPago(@RequestBody PagoRequest request) throws Exception {

        return pagoService.crearPago(request);
    }
    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirNotificacion(@RequestBody String notification) {
        System.out.println("Notificación MP: " + notification);
        // Aquí actualizas el estado del pedido en tu base de datos
        return ResponseEntity.ok().build();
    }
}