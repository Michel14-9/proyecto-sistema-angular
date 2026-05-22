package com.sistemaapolloAngular.dto;

import lombok.Data;

@Data
public class WhatsAppRequestDTO {
    private String phoneNumber;  // formato: 51987654321
    private String message;
    private Long pedidoId;       // opcional, para notif. de pedido
}