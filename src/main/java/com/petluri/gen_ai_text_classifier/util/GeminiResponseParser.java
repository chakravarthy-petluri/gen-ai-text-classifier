package com.petluri.gen_ai_text_classifier.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petluri.gen_ai_text_classifier.model.Classification;

import java.util.ArrayList;
import java.util.List;

public class GeminiResponseParser {

    public List<Classification> parseGeminiResponse(String jsonResponse) {
        List<Classification> classifications = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode candidatesNode = rootNode.path("candidates");

            if (!candidatesNode.isArray() || candidatesNode.isEmpty()) {
                return classifications; // Return empty list if no candidates found
            }

            // Extract the text containing the JSON classification list
            String textResponse = candidatesNode.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // Remove the markdown ```json wrapper if present
            String cleanJson = textResponse.replace("```json", "").replace("```", "").trim();

            // Parse extracted JSON array into List<Classification>
            classifications = objectMapper.readValue(cleanJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Classification.class));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return classifications;
    }
}
