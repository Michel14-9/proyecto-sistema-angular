package com.sistemaapolloAngular.sistema_apolloAngular.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class MercadoPagoProxyService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String MP_SERVICE_URL = "http://localhost:8081/api/pago";

    public Map<String, Object> crearPreferencia(Map<String, Object> requestData) {
        try {
            String url = MP_SERVICE_URL + "/crear-preferencia";

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
            throw new RuntimeException("Error al comunicarse con el microservicio de pagos", e);
        }
    }
}