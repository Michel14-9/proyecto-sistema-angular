package mercadopago.example.mercado.pago.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.resources.payment.Payment;
import mercadopago.example.mercado.pago.service.MercadoPagoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pago")
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public MercadoPagoController(MercadoPagoService mercadoPagoService) {
        this.mercadoPagoService = mercadoPagoService;
    }

    @PostMapping("/crear-preferencia")
    public ResponseEntity<?> crearPreferencia(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("📝 Creando preferencia - Inicio");

            Long pedidoId = ((Number) request.get("pedidoId")).longValue();
            Map<String, Object> pedidoData = (Map<String, Object>) request.get("pedido");

            Map<String, Object> preference = mercadoPagoService.crearPreferencia(pedidoData, pedidoId);

            Map<String, Object> response = new HashMap<>();
            response.put("preferenceId", preference.get("id"));
            response.put("publicKey", mercadoPagoService.getPublicKey());

            String initPoint = (String) preference.get("sandbox_init_point");
            if (initPoint == null || initPoint.isEmpty()) {
                initPoint = (String) preference.get("init_point");
            }
            response.put("initPoint", initPoint);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error creando preferencia: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear la preferencia: " + e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("📥 Webhook recibido: " + payload);

            String paymentId = null;
            String type = (String) payload.get("type");
            String topic = (String) payload.get("topic");

            // ✅ Caso 1: Formato con type=payment y data.id
            if (type != null && "payment".equals(type)) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                if (data != null) {
                    paymentId = (String) data.get("id");
                }
            }

            // ✅ Caso 2: Formato con topic=payment y resource
            if (paymentId == null && "payment".equals(topic)) {
                String resource = (String) payload.get("resource");
                if (resource != null) {
                    paymentId = resource.replaceAll("[^0-9]", "");
                    System.out.println("🔍 PaymentId extraído de resource: " + paymentId);
                }
            }

            // ✅ Caso 3: topic=merchant_order (formato viejo, con resource=URL)
            if (paymentId == null && "merchant_order".equals(topic)) {
                String resource = (String) payload.get("resource");
                if (resource != null) {
                    String merchantOrderId = resource.replaceAll("[^0-9]", "");
                    paymentId = extraerPaymentIdDeMerchantOrder(merchantOrderId);
                }
            }

            // ✅ Caso 3b: NUEVO - type=topic_merchant_order_wh (id directo en el payload)
            if (paymentId == null && type != null && type.contains("merchant_order")) {
                Object idObj = payload.get("id");
                if (idObj != null) {
                    String merchantOrderId = String.valueOf(idObj).replaceAll("[^0-9]", "");
                    System.out.println("🔍 Merchant Order ID (formato nuevo): " + merchantOrderId);
                    paymentId = extraerPaymentIdDeMerchantOrder(merchantOrderId);
                }
            }

            // ✅ Caso 4: Si tiene id directamente (fallback final, SOLO si es tipo payment o no tiene type reconocido)
            if (paymentId == null && payload.containsKey("id")
                    && (type == null || "payment".equals(type))) {
                paymentId = String.valueOf(payload.get("id"));
            }

            // ⚠️ Solo procesar si es un paymentId válido
            if (paymentId != null) {
                try {
                    Payment payment = mercadoPagoService.obtenerPago(paymentId);
                    System.out.println("💰 Pago obtenido: " + payment.getId() + " - Status: " + payment.getStatus());

                    String externalReference = payment.getExternalReference();
                    if (externalReference != null) {
                        mercadoPagoService.notificarBackendPrincipal(payment, externalReference);
                    } else {
                        System.out.println("⚠️ Pago sin externalReference: " + paymentId);
                    }

                } catch (Exception e) {
                    System.err.println("❌ Error procesando pago: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("ℹ️ Webhook ignorado (no se pudo extraer paymentId): " + payload);
            }

            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (Exception e) {
            System.err.println("❌ Error en webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 🔧 Método auxiliar extraído para reutilizar en Caso 3 y Caso 3b
    private String extraerPaymentIdDeMerchantOrder(String merchantOrderId) {
        try {
            String url = "https://api.mercadopago.com/merchant_orders/" + merchantOrderId;
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    String.class
            );

            Map<String, Object> merchantOrder = objectMapper.readValue(response.getBody(), Map.class);
            Object paymentsObj = merchantOrder.get("payments");

            if (paymentsObj instanceof java.util.List) {
                java.util.List<Map<String, Object>> payments = (java.util.List<Map<String, Object>>) paymentsObj;
                if (!payments.isEmpty()) {
                    String pid = String.valueOf(payments.get(0).get("id"));
                    System.out.println("🔍 PaymentId extraído de merchant_order: " + pid);
                    return pid;
                } else {
                    System.out.println("ℹ️ Merchant_order sin pagos asociados aún (id=" + merchantOrderId + ")");
                }
            } else if (paymentsObj instanceof Map) {
                Map<String, Object> paymentMap = (Map<String, Object>) paymentsObj;
                String pid = String.valueOf(paymentMap.get("id"));
                System.out.println("🔍 PaymentId extraído de merchant_order: " + pid);
                return pid;
            }
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo merchant_order: " + e.getMessage());
        }
        return null;
    }

    @GetMapping("/public-key")
    public ResponseEntity<?> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", mercadoPagoService.getPublicKey()));
    }
}