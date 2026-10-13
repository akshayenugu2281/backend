package com.library360.fine;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Fine f JOIN FETCH f.member JOIN FETCH f.member.user WHERE f.id = :id")
    Optional<Fine> lockById(@Param("id") Long id);

    @Query("SELECT f FROM Fine f WHERE f.loan.id = :loanId AND f.reason = :reason")
    Optional<Fine> findByLoanAndReason(@Param("loanId") Long loanId, @Param("reason") FineReason reason);

    @Query("SELECT COALESCE(SUM(f.amount - f.paidAmount), 0) FROM Fine f WHERE f.member.id = :memberId AND f.status IN ('OPEN', 'PARTIAL')")
    BigDecimal sumOutstandingByMember(@Param("memberId") Long memberId);

    @Query("SELECT f FROM Fine f JOIN FETCH f.member JOIN FETCH f.member.user " +
           "WHERE (:status IS NULL OR f.status = :status) " +
           "AND (:memberId IS NULL OR f.member.id = :memberId) " +
           "ORDER BY f.createdAt DESC")
    Page<Fine> findFines(@Param("status") FineStatus status, 
                         @Param("memberId") Long memberId, 
                         Pageable pageable);
}
