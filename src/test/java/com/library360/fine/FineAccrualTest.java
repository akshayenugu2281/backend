package com.library360.fine;

import com.library360.catalog.Book;
import com.library360.catalog.BookCopy;
import com.library360.catalog.BookCopyRepository;
import com.library360.catalog.BookRepository;
import com.library360.catalog.CopyStatus;
import com.library360.circulation.Loan;
import com.library360.circulation.LoanRepository;
import com.library360.member.LoanPolicy;
import com.library360.member.LoanPolicyRepository;
import com.library360.member.Member;
import com.library360.member.MemberRepository;
import com.library360.member.MemberStatus;
import com.library360.member.MemberType;
import com.library360.user.Role;
import com.library360.user.RoleRepository;
import com.library360.user.User;
import com.library360.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/** Deterministic overdue-fine math: daily rate, maximum cap, idempotent re-accrual. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FineAccrualTest {

    @Autowired FineService fines;
    @Autowired FineRepository fineRepository;
    @Autowired LoanRepository loans;
    @Autowired BookCopyRepository copies;
    @Autowired BookRepository books;
    @Autowired MemberRepository members;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired LoanPolicyRepository policies;

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime());

    private Member member;
    private User staff;

    @BeforeEach
    void seed() {
        Role studentRole = roles.save(new Role("STUDENT"));
        LoanPolicy p = new LoanPolicy();
        p.setMemberType(MemberType.STUDENT);
        p.setMaxLoans(5);
        p.setLoanDays(20);
        p.setMaxRenewals(2);
        p.setMaxReservations(5);
        p.setFinePerDay(new BigDecimal("5.00"));
        p.setFineCap(new BigDecimal("200.00"));
        p.setBlockFineThreshold(new BigDecimal("100.00"));
        p.setHoldHours(12);
        policies.save(p);

        User u = new User("fine.qa" + SEQ.incrementAndGet() + "@college.edu", "hash", "Fine QA");
        u.setApprovalStatus("APPROVED");
        u.setEnabled(true);
        u.getRoles().add(studentRole);
        users.save(u);
        member = new Member();
        member.setUser(u);
        member.setMemberCode("FINE-QA-" + SEQ.get());
        member.setMemberType(MemberType.STUDENT);
        member.setStatus(MemberStatus.ACTIVE);
        member.setValidUntil(LocalDate.now().plusYears(1));
        members.save(member);

        staff = new User("fine.staff" + SEQ.incrementAndGet() + "@college.edu", "hash", "Fine Staff");
        staff.setApprovalStatus("APPROVED");
        staff.setEnabled(true);
        users.save(staff);
    }

    private Loan loanOverdueByDays(long days) {
        Book book = new Book();
        book.setTitle("Fine Math " + SEQ.incrementAndGet());
        books.save(book);
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setBarcode("FINE-BC-" + SEQ.incrementAndGet());
        copy.setAccessionNumber("FINE-ACC-" + SEQ.get());
        copy.setStatus(CopyStatus.ON_LOAN);
        copies.save(copy);
        Instant now = Instant.now();
        Loan loan = Loan.open(copy, member, now.minus(days + 20, ChronoUnit.DAYS),
            now.minus(days, ChronoUnit.DAYS), staff);
        return loans.save(loan);
    }

    @Test
    void fiveDaysLateAtFivePerDayIsTwentyFive() {
        Loan loan = loanOverdueByDays(5);
        fines.accrueOverdue(loan, Instant.now());
        Fine fine = fineRepository.findByLoanAndReason(loan.getId(), FineReason.OVERDUE).orElseThrow();
        assertEquals(0, new BigDecimal("25.00").compareTo(fine.getAmount()));
        assertEquals(FineStatus.OPEN, fine.getStatus());
    }

    @Test
    void capEnforcedAtTwoHundred() {
        Loan loan = loanOverdueByDays(100);
        fines.accrueOverdue(loan, Instant.now());
        Fine fine = fineRepository.findByLoanAndReason(loan.getId(), FineReason.OVERDUE).orElseThrow();
        assertEquals(0, new BigDecimal("200.00").compareTo(fine.getAmount()));
    }

    @Test
    void reAccrualIsIdempotent() {
        Loan loan = loanOverdueByDays(5);
        fines.accrueOverdue(loan, Instant.now());
        fines.accrueOverdue(loan, Instant.now());
        assertEquals(1, fineRepository.findAll().size(), "one fine row per loan+reason");
    }

    @Test
    void onTimeLoanAccruesNothing() {
        Loan loan = loanOverdueByDays(0);
        fines.accrueOverdue(loan, Instant.now());
        assertTrue(fineRepository.findAll().isEmpty());
    }
}
