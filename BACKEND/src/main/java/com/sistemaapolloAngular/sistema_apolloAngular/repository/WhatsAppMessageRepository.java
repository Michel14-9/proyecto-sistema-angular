package com.sistemaapolloAngular.sistema_apolloAngular.repository;

import com.sistemaapolloAngular.sistema_apolloAngular.model.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WhatsAppMessageRepository 
        extends JpaRepository<WhatsAppMessage, Long> {
    List<WhatsAppMessage> findByPhoneNumber(String phoneNumber);
    List<WhatsAppMessage> findByPedidoId(Long pedidoId);
}