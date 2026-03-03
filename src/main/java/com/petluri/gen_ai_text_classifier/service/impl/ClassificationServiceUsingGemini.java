package com.petluri.gen_ai_text_classifier.service.impl;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.petluri.gen_ai_text_classifier.config.GenAIServicesConfig;
import com.petluri.gen_ai_text_classifier.model.Classification;
import com.petluri.gen_ai_text_classifier.model.ClassificationRequest;
import com.petluri.gen_ai_text_classifier.service.ClassificationService;
import com.petluri.gen_ai_text_classifier.util.GenAIResponseParser;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClassificationServiceUsingGemini implements ClassificationService {

    private final GenAIServicesConfig genAIServicesConfig;
    private final RestTemplate restTemplate;

    @Autowired
    private Environment environment;

    public ClassificationServiceUsingGemini(GenAIServicesConfig genAIServicesConfig) {
        this.genAIServicesConfig = genAIServicesConfig;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public List<Classification> classify(ClassificationRequest classificationRequest) {
        String response = getResponse(classificationRequest, 
        getHeaders(classificationRequest), 
        getRequestBody(getPrompt(classificationRequest), classificationRequest));

        System.out.println("Gemini Response: " + response); // More specific log message

        return parseResponse(response);
    }

    private List<Classification> parseResponse(String response) {
        try {
            // Extract text from Gemini's nested response structure: candidates[0].content.parts[0].text
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode candidatesNode = rootNode.path("candidates");

            if (!candidatesNode.isArray() || candidatesNode.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            String textResponse = candidatesNode.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // Use common parser to parse the JSON response
            return GenAIResponseParser.parseJsonResponse(textResponse);

        } catch (Exception e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage() + "\nResponse: " + response);
            return java.util.Collections.emptyList();
        }
    }

    private String getPrompt(ClassificationRequest classificationRequest) {
        StringBuilder prompt = new StringBuilder(genAIServicesConfig.getBasePrompt() + classificationRequest.getAttributeList() + ".\n");
        for (int i = 0; i < classificationRequest.getTextToClassifyList().size(); i++) {
            prompt.append(i + 1).append(". ").append(classificationRequest.getTextToClassifyList().get(i)).append("\n");
        }
        prompt.append("\n\nIMPORTANT: Return ONLY a valid JSON array (no markdown, no extra text). Each object must have 'textToClassify' (the original text) and 'attribute' (one of the classification attributes) fields. Example: [{\"textToClassify\": \"This is great\", \"attribute\": \"positive\"}, {\"textToClassify\": \"I love it\", \"attribute\": \"positive\"}]");
        return prompt.toString();
    }


    private String getResponse(ClassificationRequest classificationRequest, HttpHeaders headers, JSONObject requestBody) {
        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
        String URL = genAIServicesConfig.getGeminiUrl() + classificationRequest.getGenAIModel() + ":generateContent?key=" + classificationRequest.getGenAIAPIKey();
        ResponseEntity<String> response = restTemplate.exchange(URL, HttpMethod.POST, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody(); // Return the whole response body (Gemini's structure is different)
        } else {
            System.err.println("Gemini API Error: " + response.getStatusCode() + " - " + response.getBody());
            return "Error calling Gemini API"; // Or throw an exception
        }
    }


    private JSONObject getRequestBody(String prompt, ClassificationRequest classificationRequest) throws JSONException {

        JSONObject requestBody = new JSONObject();

        JSONArray partsArray = new JSONArray();
        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);
        partsArray.put(textPart);

        JSONArray contentsArray = new JSONArray();
        JSONObject contentObject = new JSONObject();
        contentObject.put("parts", partsArray);
        contentsArray.put(contentObject);

        requestBody.put("contents", contentsArray); // Correct structure

        System.out.println("Gemini request: " + requestBody.toString()); // Log the prompt being sent

        return requestBody;
    }

    private HttpHeaders getHeaders(ClassificationRequest classificationRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}