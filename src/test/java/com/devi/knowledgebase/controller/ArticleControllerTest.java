package com.devi.knowledgebase.controller;

import com.devi.knowledgebase.entity.Article;
import com.devi.knowledgebase.entity.User;
import com.devi.knowledgebase.repository.ArticleRepository;
import com.devi.knowledgebase.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("article-user@example.com")
                .passwordHash("hashed-password")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        otherUser = userRepository.save(User.builder()
                .email("other-user@example.com")
                .passwordHash("hashed-password")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void createArticleWithoutAuthenticationShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Spring Boot",
                                  "content": "Spring Boot article content"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createArticleWithAuthenticationShouldReturnCreated() throws Exception {
        mockMvc.perform(post("/articles")
                        .with(authentication(auth(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Spring Boot",
                                  "content": "Spring Boot article content"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Spring Boot"))
                .andExpect(jsonPath("$.content").value("Spring Boot article content"))
                .andExpect(jsonPath("$.authorId").value(user.getId()));
    }

    @Test
    void getAllArticlesShouldReturnOnlyActiveArticles() throws Exception {
        articleRepository.save(Article.builder()
                .title("Active Article")
                .content("Active content")
                .author(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        articleRepository.save(Article.builder()
                .title("Deleted Article")
                .content("Deleted content")
                .author(user)
                .deletedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Active Article"));
    }

    @Test
    void getArticleByIdShouldReturnArticle() throws Exception {
        Article article = articleRepository.save(Article.builder()
                .title("Article One")
                .content("Article content")
                .author(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/articles/{id}", article.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Article One"))
                .andExpect(jsonPath("$.content").value("Article content"));
    }

    @Test
    void getArticleByInvalidIdShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/articles/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Article not found"));
    }

    @Test
    void updateOwnArticleShouldReturnUpdatedArticle() throws Exception {
        Article article = articleRepository.save(Article.builder()
                .title("Old Title")
                .content("Old content")
                .author(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(put("/articles/{id}", article.getId())
                        .with(authentication(auth(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title",
                                  "content": "Updated content"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated content"));
    }

    @Test
    void updateOtherUsersArticleShouldReturnForbidden() throws Exception {
        Article article = articleRepository.save(Article.builder()
                .title("Other User Article")
                .content("Other user content")
                .author(otherUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(put("/articles/{id}", article.getId())
                        .with(authentication(auth(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hack Title",
                                  "content": "Hack content"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().string("You are not allowed to access this article"));
    }

    @Test
    void deleteOwnArticleShouldSoftDeleteArticle() throws Exception {
        Article article = articleRepository.save(Article.builder()
                .title("Delete Me")
                .content("Delete content")
                .author(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(delete("/articles/{id}", article.getId())
                        .with(authentication(auth(user))))
                .andExpect(status().isNoContent());

        Article deletedArticle = articleRepository.findById(article.getId()).orElseThrow();

        assert deletedArticle.getDeletedAt() != null;
    }

    @Test
    void deleteOtherUsersArticleShouldReturnForbidden() throws Exception {
        Article article = articleRepository.save(Article.builder()
                .title("Other User Article")
                .content("Other user content")
                .author(otherUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(delete("/articles/{id}", article.getId())
                        .with(authentication(auth(user))))
                .andExpect(status().isForbidden())
                .andExpect(content().string("You are not allowed to access this article"));
    }

    private UsernamePasswordAuthenticationToken auth(User user) {
        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of()
        );
    }
}