package com.chatbot.chatbot_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@Service
public class GroqService {

    private final ObjectMapper objectMapper;

    // ✅ LEER API KEY DESDE .env
    @Value("${GROQ_API_KEY}")
    private String apiKey;

    // MODELOS ACTUALES DE GROQ (2024-2025)
    private static final String[] MODELS = {
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "llama-guard-3-8b"
    };

    public GroqService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        System.out.println("🔑 GroqService inicializado");
    }

    // ✅ Método para obtener WebClient con la API Key
    private WebClient getWebClient() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("❌ GROQ_API_KEY no está configurada en el archivo .env");
        }
        System.out.println("🔑 API Key cargada correctamente");
        return WebClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String sendMessage(String message) {
        try {
            // ✅ Obtener WebClient con la API Key
            WebClient client = getWebClient();

            if (apiKey == null || apiKey.isEmpty()) {
                return "🍗 Error: API Key de Groq no configurada. Revisa tu archivo .env";
            }

            for (String model : MODELS) {
                try {
                    System.out.println("🔄 Probando con modelo: " + model);
                    String result = sendMessageWithModel(message, model, client);
                    if (result != null && !result.startsWith("Error") && !result.startsWith("No se pudo")) {
                        System.out.println("✅ Éxito con modelo: " + model);
                        return result;
                    }
                } catch (Exception e) {
                    System.out.println("❌ Modelo " + model + " falló: " + e.getMessage());
                }
            }
            return "🍗 Lo siento, no pude procesar tu mensaje. Por favor, intenta de nuevo.";

        } catch (Exception e) {
            System.err.println("❌ Error en sendMessage: " + e.getMessage());
            e.printStackTrace();
            return "🍗 Error al procesar tu mensaje. Por favor, intenta de nuevo.";
        }
    }

    private String sendMessageWithModel(String message, String model, WebClient client) {
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);

            List<Map<String, String>> messages = new ArrayList<>();

            // 📌 MENSAJE DEL SISTEMA - FUERZA EL ESPAÑOL
            Map<String, String> systemMessage = new LinkedHashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content",
                    "Eres el asistente virtual de 'Chicken Luren', un restaurante peruano especializado en pollo a la brasa. " +
                            "REGLAS OBLIGATORIAS:\n" +
                            "1. RESPONDE SIEMPRE EN ESPAÑOL. NUNCA en inglés.\n" +
                            "2. Si el usuario escribe en inglés, responde en español.\n" +
                            "3. Tienes un tono amigable, cálido y profesional.\n" +
                            "4. El restaurante está en La Mar 1141, Ica.\n" +
                            "5. El horario es de 10:00 AM a 10:00 PM.\n" +
                            "6. Ofrecemos pollo a la brasa, papas fritas, ensaladas, gaseosas y combos familiares.\n" +
                            "7. SIEMPRE RESPONDE EN ESPAÑOL. Esta es la regla más importante."
            );
            messages.add(systemMessage);

            // Mensaje del usuario
            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 1024);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            System.out.println("📤 Enviando mensaje a Groq: " + message);

            Map<String, Object> response = client.post()
                    .uri("/chat/completions")
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
                    String content = (String) messageObj.get("content");

                    // 📌 Si la respuesta contiene inglés, forzar español
                    if (content != null && (content.toLowerCase().contains("hello") ||
                            content.toLowerCase().contains("lovely") ||
                            content.toLowerCase().contains("how can i") ||
                            content.toLowerCase().contains("nice to meet"))) {
                        return "🍗 ¡Hola! Disculpa, hubo un problema con mi respuesta. ¿En qué puedo ayudarte con nuestro delicioso pollo a la brasa?";
                    }

                    return content;
                }
            }
            return "No se pudo obtener respuesta";

        } catch (WebClientResponseException e) {
            System.err.println("❌ Error HTTP: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}