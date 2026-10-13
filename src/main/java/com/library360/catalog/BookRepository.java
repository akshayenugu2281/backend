package com.library360.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query(value = """
        SELECT b.* FROM books b
        WHERE b.deleted = false
          AND (:q IS NULL OR
               LOWER(COALESCE(b.title, '')) LIKE CONCAT('%', LOWER(:q), '%') OR
               LOWER(COALESCE(b.subtitle, '')) LIKE CONCAT('%', LOWER(:q), '%') OR
               LOWER(COALESCE(b.description, '')) LIKE CONCAT('%', LOWER(:q), '%') OR
               COALESCE(b.isbn13, '') LIKE CONCAT('%', :q, '%') OR
               EXISTS (
                   SELECT 1 FROM book_authors ba
                   JOIN authors a ON a.id = ba.author_id
                   WHERE ba.book_id = b.id
                     AND LOWER(a.full_name) LIKE CONCAT('%', LOWER(:q), '%')
               ))
          AND (:categoryId IS NULL OR b.category_id = :categoryId)
          AND (:language IS NULL OR b.language = :language)
        """,
        countQuery = """
        SELECT COUNT(*) FROM books b
        WHERE b.deleted = false
          AND (:q IS NULL OR
               LOWER(COALESCE(b.title, '')) LIKE CONCAT('%', LOWER(:q), '%') OR
               LOWER(COALESCE(b.subtitle, '')) LIKE CONCAT('%', LOWER(:q), '%') OR
               LOWER(COALESCE(b.description, '')) LIKE CONCAT('%', LOWER(:q), '%') OR
               COALESCE(b.isbn13, '') LIKE CONCAT('%', :q, '%') OR
               EXISTS (
                   SELECT 1 FROM book_authors ba
                   JOIN authors a ON a.id = ba.author_id
                   WHERE ba.book_id = b.id
                     AND LOWER(a.full_name) LIKE CONCAT('%', LOWER(:q), '%')
               ))
          AND (:categoryId IS NULL OR b.category_id = :categoryId)
          AND (:language IS NULL OR b.language = :language)
        """,
        nativeQuery = true)
    Page<Book> search(@Param("q") String q,
                      @Param("categoryId") Long categoryId,
                      @Param("language") String language,
                      Pageable pageable);

    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.authors LEFT JOIN FETCH b.category LEFT JOIN FETCH b.publisher WHERE b.id = :id AND b.deleted = false")
    Optional<Book> findByIdWithDetails(@Param("id") Long id);

    boolean existsByIsbn13(String isbn13);
}
