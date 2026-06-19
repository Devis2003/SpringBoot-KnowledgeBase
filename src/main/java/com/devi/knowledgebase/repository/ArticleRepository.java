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

    @Query(
            value = """
                SELECT *
                FROM articles
                WHERE deleted_at IS NULL
                AND search_vector @@ plainto_tsquery('english', :keyword)
                ORDER BY ts_rank(search_vector, plainto_tsquery('english', :keyword)) DESC
                """,
            nativeQuery = true
    )
    List<Article> searchByKeyword(@Param("keyword") String keyword);

    @Query(
            value = """
            SELECT DISTINCT a.*
            FROM articles a
            JOIN article_tags at ON a.id = at.article_id
            JOIN tags t ON t.id = at.tag_id
            WHERE a.deleted_at IS NULL
            AND a.search_vector @@ plainto_tsquery('english', :keyword)
            AND LOWER(t.name) IN (:tags)
            ORDER BY ts_rank(a.search_vector, plainto_tsquery('english', :keyword)) DESC
            """,
            nativeQuery = true
    )
    List<Article> searchByKeywordAndTags(
            @Param("keyword") String keyword,
            @Param("tags") List<String> tags
    );

    @Query("SELECT a FROM Article a")
    List<Article> findAllIncludingDeleted();

}