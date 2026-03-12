package com.petluri.gen_ai_text_classifier.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class GenAIServicesConfig {

    @Value("${chatgpt.url}")
    private String chatGPTUrl;

    @Value("${gemini.url:https://generativelanguage.googleapis.com/v1beta/models/}")
    private String geminiUrl;

    @Value("${claude.url:https://api.anthropic.com/v1/models/}")
    private String claudeUrl;

    @Value("${base.prompt:Please classify the following texts into these attributes: }")
    private String basePrompt;
}
