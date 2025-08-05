package com.moneyflow.moneyflow.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.logging.Logger;

@Service
public class ChatbotService {
    private final OkHttpClient client = new OkHttpClient();
    private static final Logger LOGGER = Logger.getLogger(ChatbotService.class.getName());
    private static final String API_KEY = "AIzaSyApLvkL1k0iDyViHLFyKl7mHKH-4XWZVbE"; // Thay bằng API key từ Google AI Studio
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"; // Thay bằng endpoint từ curl

    public String getChatbotResponse(String userMessage) throws IOException {
        MediaType mediaType = MediaType.parse("application/json");
        JSONObject requestBody = new JSONObject()
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

        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(mediaType, requestBody.toString()))
                .addHeader("Content-Type", "application/json")
                .addHeader("X-goog-api-key", API_KEY) // Sử dụng header từ curl
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                LOGGER.info("Phản hồi từ Gemini API: " + responseBody);
                JSONObject jsonResponse = new JSONObject(responseBody);
                return jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
            } else {
                String errorMessage = response.body() != null ? response.body().string() : "Không có nội dung lỗi";
                LOGGER.severe("Gọi API Gemini thất bại. Mã lỗi: " + response.code() + ", Thông điệp: " + errorMessage);
                if (response.code() == 429) {
                    return "Lỗi: Bạn đã vượt quá quota. Vui lòng kiểm tra tại https://aistudio.google.com.";
                } else if (response.code() == 401) {
                    return "Lỗi: Xác thực không hợp lệ. Vui lòng kiểm tra API key tại https://aistudio.google.com hoặc dùng OAuth 2.0.";
                }
                return "Lỗi khi gọi API chatbot.";
            }
        } catch (IOException e) {
            LOGGER.severe("Lỗi kết nối đến Gemini API: " + e.getMessage());
            return "Lỗi khi gọi API chatbot.";
        }
    }
}