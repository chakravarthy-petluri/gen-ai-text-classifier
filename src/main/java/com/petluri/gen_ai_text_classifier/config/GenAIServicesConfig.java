package com.petluri.gen_ai_text_classifier.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class GenAIServicesConfig {

    @Value("${chatgpt.url}")
    private String chatGPTUrl;
}
