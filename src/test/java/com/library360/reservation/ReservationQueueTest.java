package com.library360.reservation;

import com.library360.catalog.Book;
import com.library360.catalog.BookCopy;
import com.library360.catalog.BookCopyRepository;
import com.library360.catalog.BookRepository;
import com.library360.catalog.CopyStatus;
import com.library360.common.PageResponse;
import com.library360.member.LoanPolicy;
import com.library360.member.LoanPolicyRepository;
import com.library360.member.Member;
import com.library360.member.MemberRepository;
import com.library360.member.MemberStatus;
import com.library360.member.MemberType;
import com.library360.reservation.dto.ReservationResponse;
import com.library360.user.Role;
import com.library360.user.RoleRepository;
import com.library360.user.User;
import com.library360.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Queue ordering (faculty priority, FIFO within tier), server-computed positions,
 * 12-hour hold expiry with automatic reassignment, and copy release.
 * Uses explicit timestamps — never waits real hours.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationQueueTest {

    @Autowired ReservationService service;
    @Autowired ReservationRepository reservations;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired MemberRepository members;
    @Autowired LoanPolicyRepository policies;
    @Autowired BookRepository books;
    @Autowired BookCopyRepository copies;

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime());

    private Role studentRole;
    private Role facultyRole;
    private Member studentA;
    private Member studentB;
    private Member faculty;
    private Book book;

    @BeforeEach
    void seed() {
        studentRole = roles.save(new Role("STUDENT"));
        facultyRole = roles.save(new Role("FACULTY"));
        policy(MemberType.STUDENT, 12, 20);
        policy(MemberType.FACULTY, 12, 30);

        studentA = member("qa.stu.a", studentRole, MemberType.STUDENT, "QA-STU-A");
        studentB = member("qa.stu.b", studentRole, MemberType.STUDENT, "QA-STU-B");
        faculty = member("qa.fac", facultyRole, MemberType.FACULTY, "QA-FAC");

        book = new Book();
        book.setTitle("Queue Theory " + SEQ.incrementAndGet());
        books.save(book);
    }

    private void policy(MemberType type, int holdHours, int loanDays) {
        LoanPolicy p = new LoanPolicy();
        p.setMemberType(type);
        p.setMaxLoans(5);
        p.setLoanDays(loanDays);
        p.setMaxRenewals(2);
        p.setMaxReservations(5);
        p.setFinePerDay(new BigDecimal("5.00"));
        p.setFineCap(new BigDecimal("200.00"));
        p.setBlockFineThreshold(new BigDecimal("100.00"));
        p.setHoldHours(holdHours);
        policies.save(p);
    }

    private Member member(String email, Role role, MemberType type, String code) {
        User u = new User(email + SEQ.incrementAndGet() + "@college.edu", "hash", "QA User");
        u.setApprovalStatus("APPROVED");
        u.setEnabled(true);
        u.getRoles().add(role);
        users.save(u);
        Member m = new Member();
        m.setUser(u);
        m.setMemberCode(code + SEQ.get());
        m.setMemberType(type);
        m.setStatus(MemberStatus.ACTIVE);
        m.setValidUntil(LocalDate.now().plusYears(1));
        return members.save(m);
    }

    private Reservation queued(Member m, Instant at) {
        Reservation r = new Reservation();
        r.setBook(book);
        r.setMember(m);
        r.setStatus(ReservationStatus.QUEUED);
        r.setQueuedAt(at);
        return reservations.save(r);
    }

    private BookCopy copy(CopyStatus status) {
        BookCopy c = new BookCopy();
        c.setBook(book);
        c.setBarcode("QA-BC-" + SEQ.incrementAndGet());
        c.setAccessionNumber("QA-ACC-" + SEQ.get());
        c.setStatus(status);
        return copies.save(c);
    }

    @Test
    void facultyJumpsQueueDespiteLaterRequest() {
        Instant base = Instant.now();
        queued(studentA, base);
        queued(faculty, base.plus(1, ChronoUnit.HOURS));
        Reservation next = service.findNextQueued(book.getId()).orElseThrow();
        assertEquals(faculty.getId(), next.getMember().getId(), "faculty priority must win over earlier student");
    }

    @Test
    void fifoWithinSameTier() {
        Instant base = Instant.now();
        Reservation first = queued(studentA, base);
        queued(studentB, base.plus(1, ChronoUnit.HOURS));
        Reservation next = service.findNextQueued(book.getId()).orElseThrow();
        assertEquals(first.getId(), next.getId(), "earlier request wins within a tier");
    }

    @Test
    void suspendedAccountsAreSkipped() {
        Instant base = Instant.now();
        Reservation blocked = queued(studentA, base);
        blocked.getMember().setStatus(MemberStatus.SUSPENDED);
        queued(studentB, base.plus(1, ChronoUnit.HOURS));
        Reservation next = service.findNextQueued(book.getId()).orElseThrow();
        assertEquals(studentB.getId(), next.getMember().getId(), "ineligible head must be skipped");
    }

    @Test
    void positionsFollowAllocationOrder() {
        Instant base = Instant.now();
        queued(studentA, base);
        queued(faculty, base.plus(1, ChronoUnit.HOURS));
        PageResponse<ReservationResponse> page =
            service.getMemberReservations(studentA.getId(), PageRequest.of(0, 20));
        assertEquals(1, page.content().size());
        ReservationResponse row = page.content().get(0);
        assertEquals(2, row.queuePosition(), "student sits behind the faculty request");
        assertEquals(2, row.queueLength());
    }

    @Test
    void expiredHoldReassignsToNextWithFreshTwelveHourDeadline() {
        Instant now = Instant.now();
        BookCopy c = copy(CopyStatus.ON_HOLD);
        Reservation ready = new Reservation();
        ready.setBook(book);
        ready.setMember(studentA);
        ready.setStatus(ReservationStatus.READY);
        ready.setQueuedAt(now.minus(2, ChronoUnit.DAYS));
        ready.setAssignedCopy(c);
        ready.setReadyAt(now.minus(13, ChronoUnit.HOURS));
        ready.setExpiresAt(now.minus(1, ChronoUnit.HOURS));
        reservations.save(ready);
        Reservation waiting = queued(studentB, now.minus(1, ChronoUnit.HOURS));

        service.expireStaleHolds(now);

        assertEquals(ReservationStatus.EXPIRED,
            reservations.findById(ready.getId()).orElseThrow().getStatus());
        Reservation promoted = reservations.findById(waiting.getId()).orElseThrow();
        assertEquals(ReservationStatus.READY, promoted.getStatus());
        assertEquals(c.getId(), promoted.getAssignedCopy().getId(), "same copy moves to the next candidate");
        assertEquals(CopyStatus.ON_HOLD, copies.findById(c.getId()).orElseThrow().getStatus());
        long holdHours = ChronoUnit.HOURS.between(now, promoted.getExpiresAt());
        assertTrue(holdHours >= 11 && holdHours <= 12, "fresh 12-hour window, was " + holdHours);
    }

    @Test
    void expiredHoldWithEmptyQueueReleasesCopy() {
        Instant now = Instant.now();
        BookCopy c = copy(CopyStatus.ON_HOLD);
        Reservation ready = new Reservation();
        ready.setBook(book);
        ready.setMember(studentA);
        ready.setStatus(ReservationStatus.READY);
        ready.setQueuedAt(now.minus(2, ChronoUnit.DAYS));
        ready.setAssignedCopy(c);
        ready.setReadyAt(now.minus(13, ChronoUnit.HOURS));
        ready.setExpiresAt(now.minus(1, ChronoUnit.HOURS));
        reservations.save(ready);

        service.expireStaleHolds(now);

        assertEquals(ReservationStatus.EXPIRED,
            reservations.findById(ready.getId()).orElseThrow().getStatus());
        assertEquals(CopyStatus.AVAILABLE, copies.findById(c.getId()).orElseThrow().getStatus());
    }

    @Test
    void schedulerReRunIsIdempotent() {
        Instant now = Instant.now();
        BookCopy c = copy(CopyStatus.ON_HOLD);
        Reservation ready = new Reservation();
        ready.setBook(book);
        ready.setMember(studentA);
        ready.setStatus(ReservationStatus.READY);
        ready.setQueuedAt(now.minus(2, ChronoUnit.DAYS));
        ready.setAssignedCopy(c);
        ready.setReadyAt(now.minus(13, ChronoUnit.HOURS));
        ready.setExpiresAt(now.minus(1, ChronoUnit.HOURS));
        reservations.save(ready);

        service.expireStaleHolds(now);
        service.expireStaleHolds(now.plus(1, ChronoUnit.MINUTES));

        List<Reservation> all = reservations.findAll();
        long expired = all.stream().filter(r -> r.getStatus() == ReservationStatus.EXPIRED).count();
        assertEquals(1, expired, "no duplicate expirations on re-run");
        assertEquals(CopyStatus.AVAILABLE, copies.findById(c.getId()).orElseThrow().getStatus());
    }
}
