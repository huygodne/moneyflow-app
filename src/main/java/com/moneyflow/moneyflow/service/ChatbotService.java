package com.moneyflow.moneyflow.service;

import java.io.IOException;

public interface ChatbotService {
    String getChatbotResponse(String userMessage) throws IOException;
}