package com.sistemaapolloAngular.sistema_apolloAngular.service;

import com.sistemaapolloAngular.sistema_apolloAngular.dto.ChatbotResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class ChatbotService {

    private final RestTemplate restTemplate;
    private final String chatbotUrl;

    public ChatbotService(RestTemplate restTemplate,
                          @Value("${chatbot.url:http://localhost:8083}") String chatbotUrl,
                          @Value("${chatbot.endpoint:/api/chatbot/message}") String chatbotEndpoint) {
        this.restTemplate = restTemplate;
        this.chatbotUrl = chatbotUrl + chatbotEndpoint;

        System.out.println("🔗 Chatbot URL: " + this.chatbotUrl);
    }

    public String sendMessage(String message) {
        try {
            // Validar que el mensaje no esté vacío
            if (message == null || message.trim().isEmpty()) {
                return "Por favor, escribe un mensaje válido.";
            }

            System.out.println("📤 Enviando mensaje al chatbot: " + message);

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Crear el body
            Map<String, String> request = Map.of("message", message);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            // Hacer la petición
            ChatbotResponse response = restTemplate.postForObject(
                    chatbotUrl,
                    entity,
                    ChatbotResponse.class
            );

            System.out.println("📥 Respuesta recibida del chatbot");

            if (response != null && response.getResponse() != null) {
                return response.getResponse();
            }

            return "Lo siento, no pude obtener una respuesta en este momento.";

        } catch (RestClientException e) {
            System.err.println("❌ Error de conexión con el chatbot: " + e.getMessage());

            // Mensaje más amigable según el tipo de error
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Connection refused")) {
                return "El servicio del chatbot no está disponible. Verifica que esté ejecutándose en el puerto 8083.";
            } else if (errorMessage.contains("404")) {
                return "El endpoint del chatbot no fue encontrado. Verifica la URL.";
            } else if (errorMessage.contains("500")) {
                return "El chatbot tuvo un error interno. Por favor, intenta más tarde.";
            }

            return "Error al conectar con el chatbot. Por favor, intenta de nuevo más tarde.";

        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            return "Error inesperado. Por favor, intenta de nuevo más tarde.";
        }
    }
}