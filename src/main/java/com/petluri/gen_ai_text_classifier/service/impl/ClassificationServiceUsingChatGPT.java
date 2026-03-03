package com.petluri.gen_ai_text_classifier.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.petluri.gen_ai_text_classifier.config.GenAIServicesConfig;
import com.petluri.gen_ai_text_classifier.model.Classification;
import com.petluri.gen_ai_text_classifier.model.ClassificationRequest;
import com.petluri.gen_ai_text_classifier.service.ClassificationService;
import com.petluri.gen_ai_text_classifier.util.GenAIResponseParser;

public class ClassificationServiceUsingChatGPT implements ClassificationService {

    private final GenAIServicesConfig genAIServicesConfig;
    private final RestTemplate restTemplate;

    public ClassificationServiceUsingChatGPT(GenAIServicesConfig genAIServicesConfig) {
        this.genAIServicesConfig = genAIServicesConfig;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public List<Classification> classify(ClassificationRequest classificationRequest) {

        String response = getResponse(getHeaders(classificationRequest), getRequestBody(getPrompt(classificationRequest), classificationRequest));

        System.out.println("Response: " + response);
        
        return parseResponse(response, classificationRequest.getTextToClassifyList());
    }

    private List<Classification> parseResponse(String response, List<String> textToClassifyList) {
        return GenAIResponseParser.parseJsonResponse(response);
    }
        
    private String getPrompt(ClassificationRequest classificationRequest) {
        StringBuilder prompt = new StringBuilder("Classify the following comments based on the attributes: " + classificationRequest.getAttributeList() + ".\n");
        for (int i = 0; i < classificationRequest.getTextToClassifyList().size(); i++) {
            prompt.append(i + 1).append(". ").append(classificationRequest.getTextToClassifyList().get(i)).append("\n");
        }
        prompt.append("\n\nIMPORTANT: Return ONLY a valid JSON array (no markdown, no extra text). Each object must have 'textToClassify' (the original text) and 'attribute' (one of the classification attributes) fields. Example: [{\"textToClassify\": \"text1\", \"attribute\": \"attr1\"}, {\"textToClassify\": \"text2\", \"attribute\": \"attr2\"}]");
        return prompt.toString();
    }

    private String getResponse(HttpHeaders headers, JSONObject requestBody) {
        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
        ResponseEntity<String> response = restTemplate.exchange(genAIServicesConfig.getChatGPTUrl(), HttpMethod.POST, request, String.class);

        JSONObject jsonResponse = new JSONObject(response.getBody());
        return jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }

    private JSONObject getRequestBody(String prompt, ClassificationRequest classificationRequest) throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", classificationRequest.getGenAIModel());
        System.out.println("Prompt: " + prompt);
        requestBody.put("messages", Collections.singletonList(
            new JSONObject().put("role", "user").put("content", prompt)
        ));
        requestBody.put("temperature", 0.7);
        return requestBody;
    }

    private HttpHeaders getHeaders(ClassificationRequest classificationRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(classificationRequest.getGenAIAPIKey());
        return headers;
    }
    
}

//# chatgpt.api.key=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
//# chatgpt.api.key=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
