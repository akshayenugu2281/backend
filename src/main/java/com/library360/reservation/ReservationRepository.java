package com.library360.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reservation r " +
           "WHERE r.book.id = :bookId AND r.status = 'QUEUED' AND r.member.status = com.library360.member.MemberStatus.ACTIVE " +
           "AND r.member.user.enabled = true AND r.member.user.approvalStatus = 'APPROVED'")
    boolean existsQueuedForBook(@Param("bookId") Long bookId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reservation r " +
           "WHERE r.book.id = :bookId AND r.member.id <> :memberId AND r.status = 'QUEUED' " +
           "AND r.member.status = com.library360.member.MemberStatus.ACTIVE AND r.member.user.enabled = true " +
           "AND r.member.user.approvalStatus = 'APPROVED'")
    boolean existsQueuedForBookExcluding(@Param("bookId") Long bookId, @Param("memberId") Long memberId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.member JOIN FETCH r.member.user " +
           "WHERE r.assignedCopy.id = :copyId AND r.status = 'READY'")
    Optional<Reservation> findReadyByCopy(@Param("copyId") Long copyId);

    // Native MySQL LIMIT prevents provider-specific JPQL parsing differences. Faculty receive first priority;
    // queue time breaks ties within the same priority tier.
    @Query(value = "SELECT r.* FROM reservations r " +
           "JOIN members m ON m.id=r.member_id JOIN users u ON u.id=m.user_id " +
           "WHERE r.book_id=:bookId AND r.status='QUEUED' AND m.status='ACTIVE' " +
           "AND u.enabled=TRUE AND u.approval_status='APPROVED' " +
           "ORDER BY CASE WHEN m.member_type='FACULTY' THEN 0 ELSE 1 END ASC, r.queued_at ASC LIMIT 1",
           nativeQuery = true)
    Optional<Reservation> findNextQueued(@Param("bookId") Long bookId);

    // Locked variant used by the allocation path so two concurrent returns/cancels/expiries
    // cannot assign the same copy twice. JPQL (not native) because Hibernate rejects
    // lock modes on native queries. Callers pass a size-1 page and take the head.
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r JOIN r.member m JOIN m.user u " +
           "WHERE r.book.id = :bookId AND r.status = 'QUEUED' AND m.status = com.library360.member.MemberStatus.ACTIVE " +
           "AND u.enabled = true AND u.approvalStatus = 'APPROVED' " +
           "ORDER BY CASE WHEN m.memberType = com.library360.member.MemberType.FACULTY THEN 0 ELSE 1 END ASC, r.queuedAt ASC, r.id ASC")
    List<Reservation> lockQueuedHead(@Param("bookId") Long bookId, org.springframework.data.domain.Pageable pageable);

    // Eligible queue ids in allocation order — used for server-computed queue positions.
    @Query(value = "SELECT r.id FROM reservations r " +
           "JOIN members m ON m.id=r.member_id JOIN users u ON u.id=m.user_id " +
           "WHERE r.book_id=:bookId AND r.status='QUEUED' AND m.status='ACTIVE' " +
           "AND u.enabled=TRUE AND u.approval_status='APPROVED' " +
           "ORDER BY CASE WHEN m.member_type='FACULTY' THEN 0 ELSE 1 END ASC, r.queued_at ASC, r.id ASC LIMIT 500",
           nativeQuery = true)
    List<Long> orderedEligibleQueueIds(@Param("bookId") Long bookId);

    @Query("SELECT r.book.id, COUNT(r) FROM Reservation r " +
           "WHERE r.book.id IN :ids AND r.status = 'QUEUED' " +
           "AND r.member.status = com.library360.member.MemberStatus.ACTIVE " +
           "AND r.member.user.enabled = true AND r.member.user.approvalStatus = 'APPROVED' " +
           "GROUP BY r.book.id")
    List<Object[]> countEligibleQueuedByBookIds(@Param("ids") java.util.Collection<Long> ids);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'READY' AND r.expiresAt < :now")
    List<Reservation> findExpiredHolds(@Param("now") Instant now);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.book JOIN FETCH r.member JOIN FETCH r.member.user " +
           "WHERE r.member.id = :memberId ORDER BY r.queuedAt DESC")
    Page<Reservation> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.member.id = :memberId AND r.status IN ('QUEUED', 'READY')")
    long countActiveByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reservation r " +
           "WHERE r.member.id = :memberId AND r.book.id = :bookId AND r.status IN ('QUEUED', 'READY')")
    boolean existsActiveForMemberAndBook(@Param("memberId") Long memberId, @Param("bookId") Long bookId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.book JOIN FETCH r.member JOIN FETCH r.member.user " +
           "WHERE r.member.id = :memberId AND r.status IN ('QUEUED','READY') ORDER BY r.queuedAt ASC")
    List<Reservation> findActiveByMemberId(@Param("memberId") Long memberId);

    @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.book JOIN FETCH r.member JOIN FETCH r.member.user ORDER BY r.queuedAt DESC",
           countQuery = "SELECT COUNT(r) FROM Reservation r")
    Page<Reservation> findAllWithDetails(Pageable pageable);
}
