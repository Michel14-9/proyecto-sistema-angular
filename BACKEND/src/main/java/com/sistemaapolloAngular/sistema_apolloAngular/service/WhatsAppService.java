package com.sistemaapolloAngular.service;

import com.sistemaapolloAngular.dto.WhatsAppRequestDTO;
import com.sistemaapolloAngular.model.Pedido;
import com.sistemaapolloAngular.model.WhatsAppMessage;
import com.sistemaapolloAngular.repository.PedidoRepository;
import com.sistemaapolloAngular.repository.WhatsAppMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppService {

    @Value("${whatsapp.api.url}")
    private String apiUrl;

    @Value("${whatsapp.phone.number.id}")
    private String phoneNumberId;

    @Value("${whatsapp.token}")
    private String token;

    private final WhatsAppMessageRepository whatsAppRepo;
    private final PedidoRepository pedidoRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public WhatsAppService(WhatsAppMessageRepository whatsAppRepo,
                           PedidoRepository pedidoRepository) {
        this.whatsAppRepo = whatsAppRepo;
        this.pedidoRepository = pedidoRepository;
    }

    // ─── Enviar mensaje de texto simple ───────────────────────────
    public WhatsAppMessage sendTextMessage(WhatsAppRequestDTO dto) {
        String url = apiUrl + "/" + phoneNumberId + "/messages";

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", dto.getPhoneNumber());
        body.put("type", "text");
        body.put("text", Map.of("body", dto.getMessage()));

        return ejecutarEnvio(url, body, dto, "text");
    }

    // ─── Notificación de pedido confirmado ────────────────────────
    public WhatsAppMessage notificarPedidoConfirmado(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        String telefono = pedido.getUsuario().getTelefono(); // ajusta según tu modelo
        String mensaje = String.format(
            "✅ *Pedido confirmado #%d*\n\n" +
            "Hola %s, tu pedido ha sido recibido.\n" +
            "Total: S/ %.2f\n" +
            "Estado: %s\n\n" +
            "Te avisaremos cuando esté en camino. 🚀",
            pedido.getId(),
            pedido.getUsuario().getNombre(),
            pedido.getTotal(),
            pedido.getEstado()
        );

        WhatsAppRequestDTO dto = new WhatsAppRequestDTO();
        dto.setPhoneNumber("51" + telefono); // código Perú
        dto.setMessage(mensaje);
        dto.setPedidoId(pedidoId);

        return sendTextMessage(dto);
    }

    // ─── Notificación de pedido en camino ─────────────────────────
    public WhatsAppMessage notificarPedidoEnCamino(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        String telefono = pedido.getUsuario().getTelefono();
        String mensaje = String.format(
            "🛵 *Tu pedido #%d está en camino*\n\n" +
            "Hola %s, tu delivery ya salió.\n" +
            "Tiempo estimado: 30-45 min.\n\n" +
            "¡Gracias por tu preferencia! 😊",
            pedido.getId(),
            pedido.getUsuario().getNombre()
        );

        WhatsAppRequestDTO dto = new WhatsAppRequestDTO();
        dto.setPhoneNumber("51" + telefono);
        dto.setMessage(mensaje);
        dto.setPedidoId(pedidoId);

        return sendTextMessage(dto);
    }

    // ─── Método interno para ejecutar el envío ────────────────────
    private WhatsAppMessage ejecutarEnvio(String url, Map<String, Object> body,
                                          WhatsAppRequestDTO dto, String tipo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        WhatsAppMessage registro = new WhatsAppMessage();
        registro.setPhoneNumber(dto.getPhoneNumber());
        registro.setMessageType(tipo);
        registro.setContent(dto.getMessage());

        if (dto.getPedidoId() != null) {
            pedidoRepository.findById(dto.getPedidoId())
                    .ifPresent(registro::setPedido);
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map messages = (Map) ((java.util.List) response.getBody()
                        .get("messages")).get(0);
                registro.setWaMessageId((String) messages.get("id"));
                registro.setStatus("SENT");
            }
        } catch (Exception e) {
            registro.setStatus("FAILED");
            registro.setContent("ERROR: " + e.getMessage());
        }

        return whatsAppRepo.save(registro);
    }
}