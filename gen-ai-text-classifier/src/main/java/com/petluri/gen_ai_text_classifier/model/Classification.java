package com.petluri.gen_ai_text_classifier.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@RequiredArgsConstructor
@Schema(description = "Classification result for a piece of text")
public class Classification {
    @Schema(description = "The text that was classified", example = "This is a positive review")
    private String textToClassify;

    @Schema(description = "The attribute assigned to the text", example = "positive")
    private String attribute;
}