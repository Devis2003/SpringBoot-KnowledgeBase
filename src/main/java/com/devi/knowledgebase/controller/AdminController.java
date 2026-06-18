package com.devi.knowledgebase.controller;

import com.devi.knowledgebase.dto.article.ArticleResponse;
import com.devi.knowledgebase.service.ArticleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ArticleService articleService;

    public AdminController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/articles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ArticleResponse> getAllArticlesIncludingDeleted() {
        return articleService.getAllArticlesForAdmin();
    }
}
