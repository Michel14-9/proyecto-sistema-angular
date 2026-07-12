package com.chatbot.chatbot_service.dto;

import java.util.List;
import java.util.Map;

public class GroqRequest {
    private String model;
    private List<Map<String, String>> messages;
    private double temperature;
    private int max_tokens;

    public GroqRequest() {}

    public GroqRequest(String model, List<Map<String, String>> messages, double temperature, int max_tokens) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
    }

    // Getters y Setters
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public List<Map<String, String>> getMessages() { return messages; }
    public void setMessages(List<Map<String, String>> messages) { this.messages = messages; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getMax_tokens() { return max_tokens; }
    public void setMax_tokens(int max_tokens) { this.max_tokens = max_tokens; }
}