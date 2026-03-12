package com.petluri.gen_ai_text_classifier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class GenAiTextClassifierApplication {

	public static void main(String[] args) {
		SpringApplication.run(GenAiTextClassifierApplication.class, args);
		log.info("GenAiTextClassifierApplication started");
		log.info("Docs available at: http://localhost:8080/swagger-ui/index.html");
	}

}
