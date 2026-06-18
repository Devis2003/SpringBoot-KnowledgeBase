package com.devi.knowledgebase.dto.article;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record ArticleRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        Set<String> tags
) {
}
