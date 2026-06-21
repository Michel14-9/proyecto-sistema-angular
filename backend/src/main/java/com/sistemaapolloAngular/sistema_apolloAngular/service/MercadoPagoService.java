package com.sistemaapolloAngular.sistema_apolloAngular.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.sistemaapolloAngular.sistema_apolloAngular.model.Pedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ItemPedido;
import com.sistemaapolloAngular.sistema_apolloAngular.model.ProductoFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    // ✅ CAMBIAR: Usar la URL de ngrok (o la variable backend.url)
    @Value("${backend.url:http://localhost:8080}")
    private String backendUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentClient paymentClient = new PaymentClient();

    @Transactional
    public Map<String, Object> crearPreferencia(Pedido pedido) {
        try {
            System.out.println("📝 Creando preferencia para pedido: " + pedido.getId());

            List<Map<String, Object>> items = new ArrayList<>();

            for (ItemPedido item : pedido.getItems()) {
                ProductoFinal producto = item.getProductoFinal();
                String titulo = producto != null ? producto.getNombre() : item.getNombreProducto();
                Double precio = item.getPrecio() != null ? item.getPrecio() : 0.0;
                Integer cantidad = item.getCantidad() != null ? item.getCantidad() : 1;

                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", producto != null ? producto.getId().toString() : "prod_" + System.currentTimeMillis());
                itemMap.put("title", titulo);
                itemMap.put("description", "Producto de Luren Chicken");
                itemMap.put("quantity", cantidad);
                itemMap.put("currency_id", "PEN");
                itemMap.put("unit_price", precio);
                items.add(itemMap);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("items", items);
            body.put("external_reference", pedido.getId().toString());

            // ✅ CORREGIDO: Usar backendUrl (que ahora es la URL de ngrok)
            String webhookUrl = backendUrl + "/api/pago/webhook";
            body.put("notification_url", webhookUrl);
            System.out.println("📡 Webhook URL: " + webhookUrl);

            Map<String, String> backUrls = new HashMap<>();
            backUrls.put("success", frontendUrl + "/pago-exitoso");
            backUrls.put("failure", frontendUrl + "/pago-fallido");
            backUrls.put("pending", frontendUrl + "/pago-pendiente");
            body.put("back_urls", backUrls);

            String jsonBody = objectMapper.writeValueAsString(body);
            System.out.println("📤 JSON enviado a MercadoPago: " + jsonBody);

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

            System.out.println("📥 Respuesta de MercadoPago: " + response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                System.out.println("✅ Preferencia creada: " + responseMap.get("id"));

                responseMap.put("init_point", responseMap.get("init_point"));
                responseMap.put("sandbox_init_point", responseMap.get("sandbox_init_point"));

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
            System.out.println("🔍 Buscando pago con ID: " + paymentId);

            MercadoPagoConfig.setAccessToken(accessToken);

            Long id = Long.parseLong(paymentId);
            return paymentClient.get(id);
        } catch (NumberFormatException e) {
            System.err.println("❌ Error al convertir paymentId: " + paymentId);
            throw new RuntimeException("ID de pago inválido: " + paymentId, e);
        } catch (Exception e) {
            System.err.println("❌ Error al obtener pago: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al obtener pago: " + e.getMessage(), e);
        }
    }

    public String getPublicKey() {
        return publicKey;
    }
}