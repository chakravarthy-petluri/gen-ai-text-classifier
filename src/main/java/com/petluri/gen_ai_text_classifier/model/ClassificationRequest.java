package com.petluri.gen_ai_text_classifier.model;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ClassificationRequest {

    private String genAIType;
    private String genAIModel;
    private String genAIAPIKey;
    private List<String> textToClassifyList;
    private List<String> attributeList;
}
