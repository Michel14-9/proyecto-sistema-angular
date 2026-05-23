package com.sistemaapolloAngular.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_messages")
@Data
public class WhatsAppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phoneNumber;     // destinatario
    private String messageType;     // text, template, order_confirm
    private String content;
    private String status;          // SENT, FAILED, DELIVERED
    private String waMessageId;     // ID que devuelve Meta

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;          // relación con tu Pedido existente
}