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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final MeterRegistry meterRegistry;

    public ArticleService(
            ArticleRepository articleRepository,
            TagRepository tagRepository,
            MeterRegistry meterRegistry
    ) {
        this.articleRepository = articleRepository;
        this.tagRepository = tagRepository;
        this.meterRegistry = meterRegistry;
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
                article.getViewCount(),
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

    @Caching(evict = {
            @CacheEvict(value = "articles", allEntries = true),
            @CacheEvict(value = "articleSearch", allEntries = true)
    })
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

    @Cacheable(
            value = "articleSearch",
            key = "#keyword + '-' + (#tags == null ? '' : #tags.toString())"
    )
    public List<ArticleResponse> searchArticles(String keyword, List<String> tags) {
        return Timer.builder("article.search")
                .description("Time taken to search articles")
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(() -> {
                    List<Article> articles;

                    if (tags == null || tags.isEmpty()) {
                        articles = articleRepository.searchByKeyword(keyword);
                    } else {
                        List<String> normalizedTags = tags.stream()
                                .map(String::trim)
                                .map(String::toLowerCase)
                                .filter(tag -> !tag.isBlank())
                                .toList();

                        articles = articleRepository.searchByKeywordAndTags(keyword, normalizedTags);
                    }

                    return articles.stream()
                            .map(this::toResponse)
                            .toList();
                });
    }

    @Caching(evict = {
            @CacheEvict(value = "article", key = "#id"),
            @CacheEvict(value = "articles", allEntries = true)
    })
    public ArticleResponse getArticleById(Long id) {

        Article article = articleRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article not found"));

        article.setViewCount(article.getViewCount() + 1);
        Article updatedArticle = articleRepository.save(article);

        return toResponse(updatedArticle);
    }

    @Caching(evict = {
            @CacheEvict(value = "article", key = "#id"),
            @CacheEvict(value = "articles", allEntries = true),
            @CacheEvict(value = "articleSearch", allEntries = true)
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
            @CacheEvict(value = "articles", allEntries = true),
            @CacheEvict(value = "articleSearch", allEntries = true)
    })
    public void deleteArticle(Long id, User currentUser) {
        Article article = articleRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article not found"));

        boolean isOwner = article.getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedArticleAccessException(
                    "You are not allowed to delete this article"
            );
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