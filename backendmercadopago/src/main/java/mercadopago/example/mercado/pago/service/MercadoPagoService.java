package mercadopago.example.mercado.pago.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class MercadoPagoService {

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @Value("${mercadopago.public.key}")
    private String publicKey;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    // URL pública (ngrok) de ESTE microservicio -> se usa SOLO para el notification_url que le damos a MercadoPago
    @Value("${backend.url:http://localhost:8080}")
    private String backendUrl;

    // URL del backend principal (sistema-apolloAngular) -> se usa para notificarle el resultado del pago
    @Value("${sistema.backend.url:http://localhost:8080}")
    private String sistemaBackendUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentClient paymentClient = new PaymentClient();

    public Map<String, Object> crearPreferencia(Map<String, Object> pedidoData, Long pedidoId) {
        try {
            System.out.println("📝 Creando preferencia para pedido: " + pedidoId);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) pedidoData.get("items");

            List<Map<String, Object>> items = new ArrayList<>();
            for (Map<String, Object> item : itemsData) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.get("id").toString());
                itemMap.put("title", item.get("nombre"));
                itemMap.put("description", "Producto de Luren Chicken");
                itemMap.put("quantity", item.get("cantidad"));
                itemMap.put("currency_id", "PEN");
                itemMap.put("unit_price", item.get("precio"));
                items.add(itemMap);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("items", items);
            body.put("external_reference", pedidoId.toString());

            // ✅ Este SÍ debe apuntar al microservicio público (ngrok), porque quien llama es MercadoPago desde internet
            String webhookUrl = backendUrl + "/api/pago/webhook";
            body.put("notification_url", webhookUrl);

            Map<String, String> backUrls = new HashMap<>();
            backUrls.put("success", frontendUrl + "/pago-exitoso");
            backUrls.put("failure", frontendUrl + "/pago-fallido");
            backUrls.put("pending", frontendUrl + "/pago-pendiente");
            body.put("back_urls", backUrls);

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            String url = "https://api.mercadopago.com/checkout/preferences";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                return responseMap;
            } else {
                throw new RuntimeException("Error al crear preferencia: " + response.getBody());
            }

        } catch (Exception e) {
            System.err.println("❌ Error al crear preferencia: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al crear preferencia: " + e.getMessage(), e);
        }
    }

    public Payment obtenerPago(String paymentId) {
        try {
            String cleanPaymentId = paymentId.replaceAll("[^0-9]", "");
            MercadoPagoConfig.setAccessToken(accessToken);
            Long id = Long.parseLong(cleanPaymentId);
            return paymentClient.get(id);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener pago: " + e.getMessage(), e);
        }
    }

    public void notificarBackendPrincipal(Payment payment, String externalReference) {
        try {
            String url = sistemaBackendUrl + "/api/pago/confirmar";

            Map<String, Object> body = new HashMap<>();
            // ✅ CORREGIDO: convertir explícitamente Long -> String para que el backend principal
            // pueda castearlo con (String) sin lanzar ClassCastException
            body.put("paymentId", payment.getId() != null ? String.valueOf(payment.getId()) : null);
            body.put("status", payment.getStatus());
            body.put("externalReference", externalReference);
            // ✅ CORREGIDO: convertir explícitamente BigDecimal -> Double
            body.put("amount", payment.getTransactionAmount() != null
                    ? payment.getTransactionAmount().doubleValue() : null);
            body.put("paymentMethodId", payment.getPaymentMethodId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("✅ Notificado al backend principal: " + response.getStatusCode());

        } catch (Exception e) {
            System.err.println("❌ Error notificando al backend: " + e.getMessage());
        }
    }

    public String getPublicKey() {
        return publicKey;
    }
}