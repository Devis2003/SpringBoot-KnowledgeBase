package com.devi.knowledgebase.controller;

import com.devi.knowledgebase.entity.Article;
import com.devi.knowledgebase.entity.User;
import com.devi.knowledgebase.repository.ArticleRepository;
import com.devi.knowledgebase.repository.TagRepository;
import com.devi.knowledgebase.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private User otherUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        tagRepository.deleteAll();
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

        adminUser = userRepository.save(User.builder()
                .email("admin-user@example.com")
                .passwordHash("hashed-password")
                .role("ADMIN")
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
                                  "content": "Spring Boot article content",
                                  "tags": ["spring", "java"]
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
                                  "content": "Spring Boot article content",
                                  "tags": ["spring", "java"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Spring Boot"))
                .andExpect(jsonPath("$.content").value("Spring Boot article content"))
                .andExpect(jsonPath("$.authorId").value(user.getId()))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("spring", "java")));
    }

    @Test
    void getAllArticlesShouldReturnOnlyActiveArticlesWithPagination() throws Exception {
        articleRepository.save(Article.builder()
                .title("Active Article")
                .content("Active content")
                .author(user)
                .build());

        articleRepository.save(Article.builder()
                .title("Deleted Article")
                .content("Deleted content")
                .author(user)
                .deletedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/articles")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Active Article"));
    }

    @Test
    void searchArticlesShouldReturnMatchingArticles() throws Exception {
        articleRepository.save(Article.builder()
                .title("Spring Boot Search")
                .content("Search content")
                .author(user)
                .build());

        articleRepository.save(Article.builder()
                .title("Redis Cache")
                .content("Cache content")
                .author(user)
                .build());

        mockMvc.perform(get("/articles/search")
                        .param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Spring Boot Search"));
    }

    @Test
    void getArticleByIdShouldReturnArticle() throws Exception {
        Article article = articleRepository.save(Article.builder()
                .title("Article One")
                .content("Article content")
                .author(user)
                .build());

        mockMvc.perform(get("/articles/{id}", article.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Article One"))
                .andExpect(jsonPath("$.content").value("Article content"));
    }

    @Test
    void getArticleByInvalidIdShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/articles/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOwnArticleShouldReturnUpdatedArticleWithTags() throws Exception {
        Article article = articleRepository.save(Article.builder()
                .title("Old Title")
                .content("Old content")
                .author(user)
                .build());

        mockMvc.perform(put("/articles/{id}", article.getId())
                        .with(authentication(auth(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title",
                                  "content": "Updated content",
                                  "tags": ["backend", "security"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated content"))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("backend", "security")));
    }

    @Test
    void updateOtherUsersArticleShouldReturnForbidden() throws Exception {
        Article article = articleRepository.save(Article.builder()
                .title("Other User Article")
                .content("Other user content")
                .author(otherUser)
                .build());

        mockMvc.perform(put("/articles/{id}", article.getId())
                        .with(authentication(auth(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hack Title",
                                  "content": "Hack content",
                                  "tags": ["hack"]
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOwnArticleShouldSoftDeleteArticle() throws Exception {
        Article article = articleRepository.save(Article.builder()
                .title("Delete Me")
                .content("Delete content")
                .author(user)
                .build());

        mockMvc.perform(delete("/articles/{id}", article.getId())
                        .with(authentication(auth(user))))
                .andExpect(status().isNoContent());

        Article deletedArticle = articleRepository.findById(article.getId()).orElseThrow();

        assertNotNull(deletedArticle.getDeletedAt());
    }

    @Test
    void deleteOtherUsersArticleShouldReturnForbidden() throws Exception {
        Article article = articleRepository.save(Article.builder()
                .title("Other User Article")
                .content("Other user content")
                .author(otherUser)
                .build());

        mockMvc.perform(delete("/articles/{id}", article.getId())
                        .with(authentication(auth(user))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldSeeActiveAndDeletedArticles() throws Exception {
        articleRepository.save(Article.builder()
                .title("Active Article")
                .content("Active content")
                .author(user)
                .build());

        articleRepository.save(Article.builder()
                .title("Deleted Article")
                .content("Deleted content")
                .author(user)
                .deletedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/admin/articles")
                        .with(authentication(auth(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void normalUserShouldNotAccessAdminArticles() throws Exception {
        mockMvc.perform(get("/admin/articles")
                        .with(authentication(auth(user))))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken auth(User user) {
        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}