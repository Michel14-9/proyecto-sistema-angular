package com.sistemaapolloAngular.controller;

import com.sistemaapolloAngular.dto.WhatsAppRequestDTO;
import com.sistemaapolloAngular.model.WhatsAppMessage;
import com.sistemaapolloAngular.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @Value("${whatsapp.verify.token}")
    private String verifyToken;

    public WhatsAppController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    // ─── Enviar mensaje manual (solo ADMIN) ───────────────────────
    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsAppMessage> sendMessage(
            @RequestBody WhatsAppRequestDTO dto) {
        return ResponseEntity.ok(whatsAppService.sendTextMessage(dto));
    }

    // ─── Notificar pedido confirmado ──────────────────────────────
    @PostMapping("/pedido/{id}/confirmado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsAppMessage> pedidoConfirmado(
            @PathVariable Long id) {
        return ResponseEntity.ok(whatsAppService.notificarPedidoConfirmado(id));
    }

    // ─── Notificar pedido en camino ───────────────────────────────
    @PostMapping("/pedido/{id}/en-camino")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsAppMessage> pedidoEnCamino(
            @PathVariable Long id) {
        return ResponseEntity.ok(whatsAppService.notificarPedidoEnCamino(id));
    }

    // ─── Webhook de Meta (verificación) ──────────────────────────
    @GetMapping("/webhook")
    public ResponseEntity<String> verificarWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Forbidden");
    }

    // ─── Webhook de Meta (recibir mensajes entrantes) ─────────────
    @PostMapping("/webhook")
    public ResponseEntity<String> recibirWebhook(@RequestBody Map<String, Object> payload) {
        // Aquí puedes procesar mensajes entrantes de clientes
        System.out.println("Webhook recibido: " + payload);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}