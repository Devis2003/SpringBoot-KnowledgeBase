package com.devi.knowledgebase.dto.event;

public record ArticleEvent(
        String eventType,
        Long articleId,
        String title
) {
}
