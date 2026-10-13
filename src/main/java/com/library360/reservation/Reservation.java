package com.library360.reservation;

import com.library360.catalog.Book;
import com.library360.catalog.BookCopy;
import com.library360.catalog.CopyStatus;
import com.library360.common.BaseEntity;
import com.library360.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "reservations")
@Getter @Setter
@NoArgsConstructor
public class Reservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.QUEUED;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_copy_id")
    private BookCopy assignedCopy;

    public void markReady(BookCopy copy, Instant now, Instant expiresAt) {
        this.status = ReservationStatus.READY;
        this.assignedCopy = copy;
        this.readyAt = now;
        this.expiresAt = expiresAt;
    }

    public void fulfill(Instant now) {
        this.status = ReservationStatus.FULFILLED;
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
        if (this.assignedCopy != null) {
            this.assignedCopy.setStatus(CopyStatus.AVAILABLE);
            this.assignedCopy = null;
        }
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
        if (this.assignedCopy != null) {
            this.assignedCopy.setStatus(CopyStatus.AVAILABLE);
            this.assignedCopy = null;
        }
    }
}
