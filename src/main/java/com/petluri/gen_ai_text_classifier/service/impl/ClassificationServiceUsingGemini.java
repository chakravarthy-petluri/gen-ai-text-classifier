package com.petluri.gen_ai_text_classifier.service.impl;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.petluri.gen_ai_text_classifier.config.GenAIServicesConfig;
import com.petluri.gen_ai_text_classifier.model.Classification;
import com.petluri.gen_ai_text_classifier.model.ClassificationRequest;
import com.petluri.gen_ai_text_classifier.service.ClassificationService;
import com.petluri.gen_ai_text_classifier.util.GeminiResponseParser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Qualifier("classificationServiceUsingChatGPTConfig")
public class ClassificationServiceUsingGemini implements ClassificationService {

    private final GenAIServicesConfig genAIServicesConfig;
    private final RestTemplate restTemplate;
    private final GeminiResponseParser geminiResponseParser;

    public ClassificationServiceUsingGemini(GenAIServicesConfig genAIServicesConfig) {
        this.genAIServicesConfig = genAIServicesConfig;
        this.restTemplate = new RestTemplate();
        this.geminiResponseParser = new GeminiResponseParser();
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

        List<Classification> geminiResponse = null; 
        try {
             geminiResponse = geminiResponseParser.parseGeminiResponse(response);

        } catch (JSONException e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage() + "\nResponse: " + response);
        }
        return geminiResponse;
    }

    private String getPrompt(ClassificationRequest classificationRequest) {
        // ... (same as before)
        StringBuilder prompt = new StringBuilder(genAIServicesConfig.getBasePrompt() + classificationRequest.getAttributeList() + ".\n");
        for (int i = 0; i < classificationRequest.getTextToClassifyList().size(); i++) {
            prompt.append(i + 1).append(". ").append(classificationRequest.getTextToClassifyList().get(i)).append("\n");
        }
        return prompt.toString();
    }


    private String getResponse(ClassificationRequest classificationRequest, HttpHeaders headers, JSONObject requestBody) {
        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
        String URL = genAIServicesConfig.getGeminiUrl()+classificationRequest.getGenAIModel()+":generateContent?key="+classificationRequest.getGenAIAPIKey(); // Use Gemini URL
        ResponseEntity<String> response = restTemplate.exchange(URL, HttpMethod.POST, request, String.class); // Use Gemini URL

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