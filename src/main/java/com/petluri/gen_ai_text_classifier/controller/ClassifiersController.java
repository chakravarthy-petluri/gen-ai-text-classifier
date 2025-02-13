package com.petluri.gen_ai_text_classifier.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petluri.gen_ai_text_classifier.model.Classification;
import com.petluri.gen_ai_text_classifier.model.ClassificationRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/classifiers")
public class ClassifiersController {

    @PostMapping("/classify")
    public List<Classification> classify(@RequestBody ClassificationRequest classificationRequest) {
        //TODO: process POST request
        
        Classification classification = new Classification();
        classification.setTextToClassify("textToClassify");
        classification.setAttribute("attribute");

        return List.of(classification);
    }
    
}
