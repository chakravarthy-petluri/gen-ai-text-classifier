package com.petluri.gen_ai_text_classifier.service;

import java.util.List;

import com.petluri.gen_ai_text_classifier.model.Classification;
import com.petluri.gen_ai_text_classifier.model.ClassificationRequest;

public interface ClassificationService {
    
    public List<Classification> classify(ClassificationRequest classificationRequest);
}
