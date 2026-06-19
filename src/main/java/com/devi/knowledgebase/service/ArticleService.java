package com.devi.knowledgebase.service;

import com.devi.knowledgebase.dto.article.ArticleRequest;
import com.devi.knowledgebase.dto.article.ArticleResponse;
import com.devi.knowledgebase.entity.Article;
import com.devi.knowledgebase.entity.Tag;
import com.devi.knowledgebase.entity.User;
import com.devi.knowledgebase.exception.ArticleNotFoundException;
import com.devi.knowledgebase.exception.UnauthorizedArticleAccessException;
import com.devi.knowledgebase.repository.ArticleRepository;
import com.devi.knowledgebase.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;

    public ArticleService(ArticleRepository articleRepository, TagRepository tagRepository) {
        this.articleRepository = articleRepository;
        this.tagRepository = tagRepository;
    }

    private ArticleResponse toResponse(Article article) {
        Set<String> tagNames = article.getTags()
                .stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getAuthor().getId(),
                article.getCreatedAt(),
                article.getUpdatedAt(),
                tagNames
        );
    }

    private Set<Tag> resolveTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Set.of();
        }

        return tagNames.stream()
                .map(String::trim)
                .filter(tagName -> !tagName.isBlank())
                .map(String::toLowerCase)
                .map(tagName -> tagRepository.findByNameIgnoreCase(tagName)
                        .orElseGet(() -> tagRepository.save(
                                Tag.builder()
                                        .name(tagName)
                                        .build()
                        )))
                .collect(Collectors.toSet());
    }

    @CacheEvict(value = "articles", allEntries = true)
    public ArticleResponse createArticle(ArticleRequest request, User author) {

        Article article = Article.builder()
                .title(request.title())
                .content(request.content())
                .author(author)
                .tags(resolveTags(request.tags()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Article savedArticle = articleRepository.save(article);

        return toResponse(savedArticle);
    }

    @Cacheable(
            value = "articles",
            key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction"
    )
    public List<ArticleResponse> getAllArticles(int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return articleRepository.findByDeletedAtIsNull(pageable)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ArticleResponse> searchArticles(String keyword) {
        return articleRepository.searchByKeyword(keyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "article", key = "#id")
    public ArticleResponse getArticleById(Long id) {

        Article article = articleRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article not found"));

        return toResponse(article);
    }

    @Caching(evict = {
            @CacheEvict(value = "article", key = "#id"),
            @CacheEvict(value = "articles", allEntries = true)
    })
    public ArticleResponse updateArticle(Long id, ArticleRequest request, User currentUser) {

        Article article = articleRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article not found"));

        if (!article.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedArticleAccessException("You are not allowed to access this article");
        }

        article.setTitle(request.title());
        article.setContent(request.content());
        article.setTags(resolveTags(request.tags()));
        article.setUpdatedAt(LocalDateTime.now());

        Article updatedArticle = articleRepository.save(article);

        return toResponse(updatedArticle);
    }

    @Caching(evict = {
            @CacheEvict(value = "article", key = "#id"),
            @CacheEvict(value = "articles", allEntries = true)
    })
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

    public List<ArticleResponse> getAllArticlesForAdmin() {
        return articleRepository.findAllIncludingDeleted()
                .stream()
                .map(this::toResponse)
                .toList();
    }
}