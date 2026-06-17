package com.devi.knowledgebase.dto.article;

import jakarta.validation.constraints.NotBlank;

public record ArticleRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Content is required")
        String content
) {
}
