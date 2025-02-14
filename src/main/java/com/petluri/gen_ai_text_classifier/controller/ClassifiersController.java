package com.petluri.gen_ai_text_classifier.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petluri.gen_ai_text_classifier.model.Classification;
import com.petluri.gen_ai_text_classifier.model.ClassificationRequest;
import com.petluri.gen_ai_text_classifier.service.ClassificationService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/classifiers")
public class ClassifiersController {

    private final ClassificationService classificationService;

    public ClassifiersController(ClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @PostMapping("/classify")
    public List<Classification> classify(@RequestBody ClassificationRequest classificationRequest) {
        return classificationService.classify(classificationRequest);

    }
    
}
