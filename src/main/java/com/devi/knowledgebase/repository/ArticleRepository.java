package com.devi.knowledgebase.repository;

import com.devi.knowledgebase.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findByDeletedAtIsNull();

    Optional<Article> findByIdAndDeletedAtIsNull(Long id);
}
