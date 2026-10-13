package com.library360.circulation;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Loan l JOIN FETCH l.copy JOIN FETCH l.member JOIN FETCH l.member.user WHERE l.id = :id")
    Optional<Loan> lockById(@Param("id") Long id);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.member.id = :memberId AND l.status IN ('ACTIVE', 'OVERDUE')")
    long countActiveByMember(@Param("memberId") Long memberId);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Loan l " +
           "WHERE l.member.id = :memberId AND l.copy.book.id = :bookId AND l.status IN ('ACTIVE', 'OVERDUE')")
    boolean existsActiveForMemberAndBook(@Param("memberId") Long memberId, @Param("bookId") Long bookId);

    @Query("SELECT l FROM Loan l JOIN FETCH l.copy c JOIN FETCH c.book WHERE l.copy.id = :copyId AND l.status IN ('ACTIVE', 'OVERDUE')")
    Optional<Loan> findOpenByCopy(@Param("copyId") Long copyId);

    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') AND l.dueAt < :now")
    List<Loan> findOverdueCandidates(@Param("now") Instant now);

    @Query("SELECT MIN(l.dueAt) FROM Loan l WHERE l.copy.book.id = :bookId AND l.status IN ('ACTIVE', 'OVERDUE')")
    Optional<Instant> earliestDueByBook(@Param("bookId") Long bookId);

    @Query("SELECT l FROM Loan l JOIN FETCH l.copy c JOIN FETCH c.book JOIN FETCH l.member m JOIN FETCH m.user " +
           "WHERE (:status IS NULL OR l.status = :status) " +
           "AND (:memberId IS NULL OR l.member.id = :memberId)")
    Page<Loan> findLoans(@Param("status") LoanStatus status, 
                         @Param("memberId") Long memberId, 
                         Pageable pageable);
}
