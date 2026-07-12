package com.chatbot.chatbot_service.controller;


import com.chatbot.chatbot_service.dto.ChatRequest;
import com.chatbot.chatbot_service.dto.ChatResponse;
import com.chatbot.chatbot_service.service.GroqService;

import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {


    private final GroqService groqService;



    public ChatbotController(GroqService groqService){

        this.groqService = groqService;

    }



    @PostMapping("/message")
    public ChatResponse sendMessage(
            @RequestBody ChatRequest request
    ){


        String respuesta = groqService.sendMessage(
                request.getMessage()
        );


        return new ChatResponse(respuesta);

    }


}