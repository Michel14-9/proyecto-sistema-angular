package com.chatbot.chatbot_service.dto;


public class ChatResponse {


    private String response;


    public ChatResponse(String response){

        this.response = response;

    }


    public String getResponse(){

        return response;

    }


    public void setResponse(String response){

        this.response = response;

    }


}