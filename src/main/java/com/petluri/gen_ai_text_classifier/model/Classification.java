package com.petluri.gen_ai_text_classifier.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Classification {
    private String textToClassify;
    private String attribute;
}