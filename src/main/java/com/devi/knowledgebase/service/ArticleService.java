package com.devi.knowledgebase.service;

import com.devi.knowledgebase.dto.article.ArticleRequest;
import com.devi.knowledgebase.dto.article.ArticleResponse;
import com.devi.knowledgebase.entity.Article;
import com.devi.knowledgebase.entity.User;
import com.devi.knowledgebase.repository.ArticleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import com.devi.knowledgebase.exception.ArticleNotFoundException;
import com.devi.knowledgebase.exception.UnauthorizedArticleAccessException;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    private ArticleResponse toResponse(Article article) {
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getAuthor().getId(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    public ArticleResponse createArticle(ArticleRequest request, User author) {

        Article article = Article.builder()
                .title(request.title())
                .content(request.content())
                .author(author)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Article savedArticle = articleRepository.save(article);

        return toResponse(savedArticle);
    }

    public List<ArticleResponse> getAllArticles() {
        return articleRepository.findByDeletedAtIsNull()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ArticleResponse getArticleById(Long id) {

        Article article = articleRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article not found"));
        return toResponse(article);
    }

    public ArticleResponse updateArticle(Long id, ArticleRequest request, User currentUser) {

        Article article = articleRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article not found"));

        if (!article.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedArticleAccessException("You are not allowed to access this article");
        }

        article.setTitle(request.title());
        article.setContent(request.content());
        article.setUpdatedAt(LocalDateTime.now());

        Article updatedArticle = articleRepository.save(article);

        return toResponse(updatedArticle);
    }

    public void deleteArticle(Long id, User currentUser) {

        Article article = articleRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article not found"));

        if (!article.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedArticleAccessException("You are not allowed to access this article");
        }

        article.setDeletedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());

        articleRepository.save(article);
    }
}
