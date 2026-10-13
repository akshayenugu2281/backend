package com.library360.catalog;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM BookCopy c JOIN FETCH c.book WHERE c.barcode = :barcode")
    Optional<BookCopy> lockByBarcode(@Param("barcode") String barcode);

    Optional<BookCopy> findByBarcode(String barcode);

    Optional<BookCopy> findByAccessionNumber(String accessionNumber);

    boolean existsByBarcode(String barcode);

    long countByBookIdAndStatus(Long bookId, CopyStatus status);

    List<BookCopy> findByBookId(Long bookId);

    @Query("SELECT c.book.id, c.status, COUNT(c) FROM BookCopy c WHERE c.book.id IN :ids GROUP BY c.book.id, c.status")
    List<Object[]> countByBookIdsGrouped(@Param("ids") java.util.Collection<Long> ids);
}
