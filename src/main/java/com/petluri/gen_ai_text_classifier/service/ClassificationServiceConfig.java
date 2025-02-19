package com.petluri.gen_ai_text_classifier.service;

import com.petluri.gen_ai_text_classifier.service.impl.ClassificationServiceUsingGemini;
import com.petluri.gen_ai_text_classifier.config.GenAIServicesConfig;
import com.petluri.gen_ai_text_classifier.service.impl.ClassificationServiceUsingChatGPT;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class ClassificationServiceConfig {

    private final GenAIServicesConfig genAIServicesConfig;

    public ClassificationServiceConfig(GenAIServicesConfig genAIServicesConfig) {
        this.genAIServicesConfig = genAIServicesConfig;
    }

    @Bean
    @Qualifier("classificationServiceUsingGemini")
    public ClassificationService classificationServiceUsingGemini() {
        return new ClassificationServiceUsingGemini(genAIServicesConfig);
    }

    @Bean
    @Qualifier("classificationServiceUsingChatGPT")
    public ClassificationService classificationServiceUsingChatGPT() {
        return new ClassificationServiceUsingChatGPT(genAIServicesConfig);
    }
}