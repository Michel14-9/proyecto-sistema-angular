package com.sistemaapolloAngular.sistema_apolloAngular.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class MercadoPagoProxyService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mercadopago.api.url:http://mercadopago:5001}")
    private String mercadoPagoApiUrl;

    public Map<String, Object> crearPreferencia(Map<String, Object> requestData) {
        try {
            String url = mercadoPagoApiUrl + "/api/pago/crear-preferencia";
            System.out.println("📡 Llamando a microservicio en: " + url);
            System.out.println("📦 Datos enviados al microservicio: " + requestData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("Error al crear preferencia: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ Error llamando al microservicio: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al comunicarse con el microservicio de pagos", e);
        }
    }
}