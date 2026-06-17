package com.devi.knowledgebase.controller;

import com.devi.knowledgebase.dto.article.ArticleRequest;
import com.devi.knowledgebase.dto.article.ArticleResponse;
import com.devi.knowledgebase.entity.User;
import com.devi.knowledgebase.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse createArticle(
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return articleService.createArticle(request, currentUser);
    }

    @GetMapping
    public List<ArticleResponse> getAllArticles() {
        return articleService.getAllArticles();
    }

    @GetMapping("/{id}")
    public ArticleResponse getArticleById(@PathVariable Long id) {
        return articleService.getArticleById(id);
    }

    @PutMapping("/{id}")
    public ArticleResponse updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return articleService.updateArticle(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        articleService.deleteArticle(id, currentUser);
    }
}