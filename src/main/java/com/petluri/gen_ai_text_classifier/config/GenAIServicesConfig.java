package com.petluri.gen_ai_text_classifier.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class GenAIServicesConfig {

    @Value("${base.prompt}")
    private String basePrompt;

    @Value("${chatgpt.url}")
    private String chatGPTUrl;

    @Value("${gemini.url}")
    private String geminiUrl;
}
