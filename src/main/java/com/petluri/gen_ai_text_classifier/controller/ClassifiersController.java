package com.petluri.gen_ai_text_classifier.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petluri.gen_ai_text_classifier.model.Classification;
import com.petluri.gen_ai_text_classifier.model.ClassificationRequest;
import com.petluri.gen_ai_text_classifier.service.ClassificationService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Qualifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;


@RestController
@RequestMapping("/api/classifiers")
@Tag(name = "Text Classifiers", description = "API for classifying text using GenAI models (Claude, Gemini, ChatGPT)")
public class ClassifiersController {

    private final ClassificationService classificationService;

    public ClassifiersController(@Qualifier("classificationServiceUsingGemini") ClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @PostMapping("/classify")
    @Operation(summary = "Classify text", description = "Classifies given text into specified attributes using the selected GenAI model")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully classified text",
            content = @Content(schema = @Schema(implementation = Classification.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or model"),
        @ApiResponse(responseCode = "500", description = "Internal server error or API call failure")
    })
    public List<Classification> classify(@RequestBody ClassificationRequest classificationRequest) {
        return classificationService.classify(classificationRequest);

    }
    
}
