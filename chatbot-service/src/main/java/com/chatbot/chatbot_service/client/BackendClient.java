package com.chatbot.chatbot_service.client;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


@Component
public class BackendClient {


    private final WebClient webClient;


    public BackendClient(
            WebClient.Builder builder,
            @Value("${backend.url}") String url
    ){

        this.webClient = builder
                .baseUrl(url)
                .build();

    }


    public String obtenerPedidos(){

        return webClient.get()
                .uri("/api/pedidos")
                .retrieve()
                .bodyToMono(String.class)
                .block();

    }


}