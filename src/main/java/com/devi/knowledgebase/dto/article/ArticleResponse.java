package com.devi.knowledgebase.dto.article;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

public record ArticleResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Set<String> tags
) implements Serializable {
}
