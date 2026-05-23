package com.sistemaapolloAngular.repository;

import com.sistemaapolloAngular.model.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WhatsAppMessageRepository 
        extends JpaRepository<WhatsAppMessage, Long> {
    List<WhatsAppMessage> findByPhoneNumber(String phoneNumber);
    List<WhatsAppMessage> findByPedidoId(Long pedidoId);
}