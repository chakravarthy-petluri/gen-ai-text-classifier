package com.petluri.gen_ai_text_classifier.util;

import com.petluri.gen_ai_text_classifier.service.ClassificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

@Component
public class GenAITypeSelector {

    private final Map<String, ClassificationService> services = new HashMap<>();

    public GenAITypeSelector(@Qualifier("classificationServiceUsingGemini") ClassificationService geminiService,
                         @Qualifier("classificationServiceUsingChatGPT") ClassificationService chatGPTService) {
        services.put("gemini", geminiService);
        services.put("chatgpt", chatGPTService);
        // Add more models here as needed:
        // services.put("llama", llamaService); // Example
        // services.put("claude", claudeService); // Example
    }

    public ClassificationService getService(String typeName) {
        ClassificationService service = services.get(typeName.toLowerCase()); // Case-insensitive lookup
        if (service == null) {
          throw new IllegalArgumentException("No service found for GenAI type: " + typeName);
        }
        return service;
    }
}