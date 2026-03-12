package com.petluri.gen_ai_text_classifier.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.petluri.gen_ai_text_classifier.util.GenAIResponseParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.petluri.gen_ai_text_classifier.config.GenAIServicesConfig;
import com.petluri.gen_ai_text_classifier.model.Classification;
import com.petluri.gen_ai_text_classifier.model.ClassificationRequest;
import com.petluri.gen_ai_text_classifier.service.ClassificationService;

public class ClassificationServiceUsingClaude implements ClassificationService {

    private final GenAIServicesConfig genAIServicesConfig;
    private final RestTemplate restTemplate;

    @Autowired
    private Environment environment;

    public ClassificationServiceUsingClaude(GenAIServicesConfig genAIServicesConfig) {
        this.genAIServicesConfig = genAIServicesConfig;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public List<Classification> classify(ClassificationRequest classificationRequest) {
        String response = getResponse(classificationRequest, getHeaders(classificationRequest),
                                     getRequestBody(getPrompt(classificationRequest), classificationRequest));

        System.out.println("Claude Response: " + response);

        return parseResponse(response, classificationRequest.getTextToClassifyList());
    }

    private List<Classification> parseResponse(String response, List<String> textToClassifyList) {
        return GenAIResponseParser.parseJsonResponse(response);
    }

    private String getPrompt(ClassificationRequest classificationRequest) {
        StringBuilder prompt = new StringBuilder(genAIServicesConfig.getBasePrompt() + classificationRequest.getAttributeList() + ".\n");
        for (int i = 0; i < classificationRequest.getTextToClassifyList().size(); i++) {
            prompt.append(i + 1).append(". ").append(classificationRequest.getTextToClassifyList().get(i)).append("\n");
        }
        prompt.append("\n\nIMPORTANT: Return ONLY a valid JSON array (no markdown, no extra text). Each object must have 'textToClassify' (the original text) and 'attribute' (one of the classification attributes) fields. Example: [{\"textToClassify\": \"text1\", \"attribute\": \"attr1\"}, {\"textToClassify\": \"text2\", \"attribute\": \"attr2\"}]");
        return prompt.toString();
    }

    private String getResponse(ClassificationRequest classificationRequest, HttpHeaders headers, JSONObject requestBody) {
        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
        String url = "https://api.anthropic.com/v1/messages";

        try {
            System.out.println("Claude API URL: " + url);
            System.out.println("Claude API Headers: " + headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject jsonResponse = new JSONObject(response.getBody());
                JSONArray content = jsonResponse.getJSONArray("content");
                String result = content.getJSONObject(0).getString("text");
                System.out.println("Claude API Response Text: " + result);
                return result;
            } else {
                System.err.println("Claude API Error: " + response.getStatusCode() + " - " + response.getBody());
                return "Error calling Claude API";
            }
        } catch (Exception e) {
            System.err.println("Claude API Exception: " + e.getMessage());
            e.printStackTrace();
            return "Error calling Claude API";
        }
    }

    private JSONObject getRequestBody(String prompt, ClassificationRequest classificationRequest) throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", classificationRequest.getGenAIModel());

        JSONArray messagesArray = new JSONArray();
        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", prompt);
        messagesArray.put(messageObject);

        requestBody.put("messages", messagesArray);
        requestBody.put("max_tokens", 1024);

        System.out.println("Claude request: " + requestBody.toString());

        return requestBody;
    }

    private HttpHeaders getHeaders(ClassificationRequest classificationRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", classificationRequest.getGenAIAPIKey());
        headers.set("anthropic-version", "2024-06-01");
        return headers;
    }
}