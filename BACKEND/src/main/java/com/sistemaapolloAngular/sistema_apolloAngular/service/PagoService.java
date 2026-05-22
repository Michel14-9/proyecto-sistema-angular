package com.sistemaapolloAngular.sistema_apolloAngular.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.preference.Preference;
import com.sistemaapolloAngular.sistema_apolloAngular.dto.PagoRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PagoService {

    @Value("${mercadopago.access.token}")
    private String accessToken;

    public String crearPago(PagoRequest request) throws Exception {

        MercadoPagoConfig.setAccessToken(accessToken);

        try {
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(request.getTitulo())
                    .quantity(request.getCantidad())
                    .unitPrice(BigDecimal.valueOf(request.getPrecio()))
                    .currencyId("PEN")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(PreferenceBackUrlsRequest.builder()
                            .success("http://localhost:8080/pago/exitoso")
                            .failure("http://localhost:8080/pago/fallido")
                            .pending("http://localhost:8080/pago/pendiente")
                            .build())

                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            return preference.getInitPoint();

        } catch (MPApiException e) {
            System.err.println("=== ERROR MERCADO PAGO ===");
            System.err.println("HTTP Status: " + e.getStatusCode());
            System.err.println("Respuesta: " + e.getApiResponse().getContent());
            System.err.println("=========================");
            throw e;
        }
    }
}