package com.library360.circulation;

import com.library360.catalog.BookCopy;
import com.library360.common.BaseEntity;
import com.library360.member.Member;
import com.library360.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "loans")
@Getter @Setter
@NoArgsConstructor
public class Loan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copy_id")
    private BookCopy copy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "renew_count", nullable = false)
    private int renewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "returned_by")
    private User returnedBy;

    @Column(length = 300)
    private String notes;

    public static Loan open(BookCopy copy, Member member, Instant issuedAt, Instant dueAt, User issuedBy) {
        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setMember(member);
        loan.setIssuedAt(issuedAt);
        loan.setDueAt(dueAt);
        loan.setIssuedBy(issuedBy);
        loan.setStatus(LoanStatus.ACTIVE);
        return loan;
    }

    public void close(Instant returnedAt, User returnedBy) {
        this.returnedAt = returnedAt;
        this.returnedBy = returnedBy;
        this.status = LoanStatus.RETURNED;
    }

    public void renew(Duration duration) {
        this.renewCount++;
        this.dueAt = this.dueAt.plus(duration);
        this.status = LoanStatus.ACTIVE; // Reset from overdue if allowed
    }

    public boolean isOverdue(Instant now) {
        return now.isAfter(dueAt) && status != LoanStatus.RETURNED && status != LoanStatus.LOST;
    }

    public void markOverdue() {
        if (status == LoanStatus.ACTIVE) {
            this.status = LoanStatus.OVERDUE;
        }
    }
}
