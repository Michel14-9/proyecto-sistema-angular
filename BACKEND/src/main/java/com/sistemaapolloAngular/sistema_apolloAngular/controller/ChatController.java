package com.sistemaapolloAngular.sistema_apolloAngular.controller;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin("*")
public class ChatController {

    private final String API_KEY =
            "TU_API_KEY_GROQ";

    @PostMapping
    public Map<String, String> chat(
            @RequestBody Map<String, String> body
    ) {

        try {

            String message =
                    body.get("message");

            URL url = new URL(
              "https://api.groq.com/openai/v1/chat/completions"
            );

            HttpURLConnection conn =
                    (HttpURLConnection)
                            url.openConnection();

            conn.setRequestMethod("POST");

            conn.setRequestProperty(
                    "Authorization",
                    "Bearer " + API_KEY
            );

            conn.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );

            conn.setDoOutput(true);

            JSONObject request =
                    new JSONObject();

            request.put(
                    "model",
                    "llama-3.3-70b-versatile"
            );

            JSONArray messages =
                    new JSONArray();

            JSONObject user =
                    new JSONObject();

            user.put(
                    "role",
                    "user"
            );

            user.put(
                    "content",
                    message
            );

            messages.put(user);

            request.put(
                    "messages",
                    messages
            );

            OutputStream os =
                    conn.getOutputStream();

            os.write(
                    request.toString().getBytes()
            );

            os.flush();
            os.close();

            BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream()
                            )
                    );

            StringBuilder response =
                    new StringBuilder();

            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            br.close();

            JSONObject json =
                    new JSONObject(
                            response.toString()
                    );

            String reply =
                    json
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            return Map.of(
                    "reply",
                    reply
            );

        } catch (Exception e) {

            e.printStackTrace();

            return Map.of(
                    "reply",
                    "Error al conectar con Groq"
            );
        }
    }
}