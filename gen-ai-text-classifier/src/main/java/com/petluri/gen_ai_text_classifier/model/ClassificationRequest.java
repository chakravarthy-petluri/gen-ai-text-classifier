package com.petluri.gen_ai_text_classifier.model;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@RequiredArgsConstructor
@Schema(description = "Request to classify text using a GenAI model")
public class ClassificationRequest {

    @Schema(description = "Type of GenAI service", example = "gemini", allowableValues = {"gemini", "claude", "chatgpt"})
    private String genAIType;

    @Schema(description = "Specific model to use", example = "gemini-2.5-flash-lite")
    private String genAIModel;

    @Schema(description = "API key for the GenAI service", example = "your-google-gemini-api-key-here")
    private String genAIAPIKey;

    @Schema(description = "List of texts to classify", example = "[\"This is great\", \"I love it\"]")
    private List<String> textToClassifyList;

    @Schema(description = "List of attributes to classify into", example = "[\"positive\", \"negative\", \"neutral\"]")
    private List<String> attributeList;
}
