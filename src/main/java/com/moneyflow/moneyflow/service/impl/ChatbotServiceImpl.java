package com.moneyflow.moneyflow.service.impl;

import com.moneyflow.moneyflow.service.ChatbotService;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.logging.Logger;

@Service
public class ChatbotServiceImpl implements ChatbotService {
    private static final Logger LOGGER = Logger.getLogger(ChatbotServiceImpl.class.getName());
    private final OkHttpClient client;
    @Value("${gemini.api.key}")
    private String apiKey;
    @Value("${gemini.api.url}")
    private String apiUrl;

    public ChatbotServiceImpl(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public String getChatbotResponse(String userMessage) throws IOException {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("User message cannot be empty");
        }

        RequestBody requestBody = buildRequestBody(userMessage);
        Request request = buildRequest(requestBody);

        return executeApiCall(request);
    }

    private RequestBody buildRequestBody(String userMessage) {
        MediaType mediaType = MediaType.parse("application/json");
        JSONObject jsonBody = new JSONObject()
                .put("contents", new JSONArray()
                        .put(new JSONObject()
                                .put("parts", new JSONArray()
                                        .put(new JSONObject().put("text", userMessage))
                                )
                        )
                )
                .put("generationConfig", new JSONObject()
                        .put("temperature", 0.7)
                        .put("maxOutputTokens", 2048)
                );
        return RequestBody.create(mediaType, jsonBody.toString());
    }

    private Request buildRequest(RequestBody requestBody) {
        return new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-goog-api-key", apiKey)
                .build();
    }

    private String executeApiCall(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                LOGGER.info("API response: " + responseBody);
                JSONObject jsonResponse = new JSONObject(responseBody);
                return jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
            } else {
                String errorMessage = response.body() != null ? response.body().string() : "No error content";
                LOGGER.severe("API call failed. Code: " + response.code() + ", Message: " + errorMessage);
                if (response.code() == 429) {
                    return "Error: Quota exceeded. Please check at https://aistudio.google.com.";
                } else if (response.code() == 401) {
                    return "Error: Invalid authentication. Please check API key at https://aistudio.google.com or use OAuth 2.0.";
                }
                return "Error calling chatbot API.";
            }
        } catch (IOException e) {
            LOGGER.severe("Connection error to API: " + e.getMessage());
            throw e;
        }
    }
}