package com.petluri.gen_ai_text_classifier.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petluri.gen_ai_text_classifier.model.ClassificationRequest;
import com.petluri.gen_ai_text_classifier.util.GenAITypeSelector;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/classifiers")
public class ClassifiersController {

    private final GenAITypeSelector genAITypeSelector;

    public ClassifiersController(GenAITypeSelector genAITypeSelector) {
        this.genAITypeSelector = genAITypeSelector;
    }

    @PostMapping("/classify")
    public ResponseEntity<?> classify(@RequestBody ClassificationRequest classificationRequest) {

        try {
            return new ResponseEntity<>(
                genAITypeSelector
                .getService(classificationRequest.getGenAIType())
                .classify(classificationRequest), 
                HttpStatus.OK
                );
        } catch (IllegalArgumentException e) { // Catch invalid model names
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            System.err.println("Error during classification: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
}
