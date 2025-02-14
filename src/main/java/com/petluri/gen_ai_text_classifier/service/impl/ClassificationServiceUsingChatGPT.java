package com.petluri.gen_ai_text_classifier.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.petluri.gen_ai_text_classifier.config.GenAIServicesConfig;
import com.petluri.gen_ai_text_classifier.model.Classification;
import com.petluri.gen_ai_text_classifier.model.ClassificationRequest;
import com.petluri.gen_ai_text_classifier.service.ClassificationService;

@Service
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

        List<Classification> classifications = Collections.emptyList();

        String[] responses = response.split("\n");
        for (int i = 0; i < responses.length; i++) {
            Classification classification = new Classification();
            classification.setTextToClassify(textToClassifyList.get(i));
            classification.setAttribute(responses[i]);
            classifications.add(classification);
        }
        return classifications;
    }
        
    private String getPrompt(ClassificationRequest classificationRequest) {
        StringBuilder prompt = new StringBuilder("Classify the following comments based on the attributes: " + classificationRequest.getAttributeList() + ".\n");
        for (int i = 0; i < classificationRequest.getTextToClassifyList().size(); i++) {
            prompt.append(i + 1).append(". ").append(classificationRequest.getTextToClassifyList().get(i)).append("\n");
        }
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