package com.library360.member;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m JOIN FETCH m.user WHERE m.id = :id")
    Optional<Member> lockById(@Param("id") Long id);

    @Query("SELECT m FROM Member m JOIN FETCH m.user WHERE m.user.id = :userId")
    Optional<Member> findByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM Member m JOIN FETCH m.user WHERE m.memberCode = :memberCode")
    Optional<Member> findByMemberCode(@Param("memberCode") String memberCode);

    @Query(value = "SELECT m FROM Member m JOIN FETCH m.user WHERE m.user.approvalStatus = 'APPROVED'",
           countQuery = "SELECT COUNT(m) FROM Member m WHERE m.user.approvalStatus = 'APPROVED'")
    Page<Member> findAllMembers(Pageable pageable);
}
