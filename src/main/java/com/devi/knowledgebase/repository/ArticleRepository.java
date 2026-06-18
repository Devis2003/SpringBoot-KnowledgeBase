package com.devi.knowledgebase.repository;

import com.devi.knowledgebase.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Page<Article> findByDeletedAtIsNull(Pageable pageable);

    Optional<Article> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        SELECT a FROM Article a
        WHERE a.deletedAt IS NULL
        AND (
            LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """)
    List<Article> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT a FROM Article a")
    List<Article> findAllIncludingDeleted();

}
