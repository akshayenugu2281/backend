package com.library360.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByFullName(String fullName);

    @Query("SELECT a FROM Author a WHERE LOWER(a.fullName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Author> search(@Param("query") String query, Pageable pageable);
}
