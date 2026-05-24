package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import com.sistemaapolloAngular.sistema_apolloAngular.dto.PagoRequest;
import com.sistemaapolloAngular.sistema_apolloAngular.service.PagoService;
import lombok.RequiredArgsConstructor;
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
}