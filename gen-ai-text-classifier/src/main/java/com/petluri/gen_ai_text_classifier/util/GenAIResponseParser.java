package com.petluri.gen_ai_text_classifier.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petluri.gen_ai_text_classifier.model.Classification;

import java.util.ArrayList;
import java.util.List;

public class GenAIResponseParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses a JSON string response into a List of Classifications.
     * Handles markdown-wrapped JSON responses.
     *
     * @param jsonResponse the JSON response string
     * @return List of Classification objects
     */
    public static List<Classification> parseJsonResponse(String jsonResponse) {
        List<Classification> classifications = new ArrayList<>();

        try {
            String cleanJson = jsonResponse.trim();
            // Remove markdown ```json wrapper if present
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.replace("```json", "").replace("```", "").trim();
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replace("```", "").trim();
            }

            classifications = objectMapper.readValue(cleanJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Classification.class));
        } catch (Exception e) {
            System.err.println("Error parsing JSON response: " + e.getMessage());
        }

        return classifications;
    }
}