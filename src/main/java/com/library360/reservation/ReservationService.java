package com.library360.reservation;

import com.library360.catalog.Book;
import com.library360.catalog.BookRepository;
import com.library360.catalog.BookCopyRepository;
import com.library360.catalog.CopyStatus;
import com.library360.catalog.BookCopy;
import com.library360.circulation.LoanRepository;
import com.library360.fine.FineService;
import com.library360.member.MemberStatus;
import com.library360.common.BusinessException;
import com.library360.common.ErrorCode;
import com.library360.common.PageResponse;
import com.library360.member.LoanPolicy;
import com.library360.member.LoanPolicyRepository;
import com.library360.member.Member;
import com.library360.member.MemberRepository;
import com.library360.reservation.dto.CreateReservationRequest;
import com.library360.reservation.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservations;
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final LoanRepository loans;
    private final MemberRepository members;
    private final LoanPolicyRepository policies;
    private final FineService fineService;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public boolean existsQueuedForBook(Long bookId) {
        return reservations.existsQueuedForBook(bookId);
    }

    public boolean existsQueuedForBookExcluding(Long bookId, Long memberId) {
        return reservations.existsQueuedForBookExcluding(bookId, memberId);
    }

    public Optional<Reservation> findReadyByCopy(Long copyId) {
        return reservations.findReadyByCopy(copyId);
    }

    public Optional<Reservation> findNextQueued(Long bookId) {
        return reservations.findNextQueued(bookId);
    }

    public Optional<Reservation> findNextQueuedForUpdate(Long bookId) {
        return reservations.lockQueuedHead(bookId, org.springframework.data.domain.PageRequest.of(0, 1))
            .stream().findFirst();
    }

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest req, Long memberId) {
        Book book = books.findByIdWithDetails(req.bookId())
            .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        // The controller resolves this ID from the caller's identity. A supplied request ID
        // is only a fallback for internal callers; a non-null resolved ID always wins.
        Long targetMemberId = memberId != null ? memberId : req.memberId();
        if (targetMemberId == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        Member member = members.lockById(targetMemberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != MemberStatus.ACTIVE || member.getUser() == null || !member.getUser().isEnabled()
                || !"APPROVED".equalsIgnoreCase(member.getUser().getApprovalStatus())
                || member.getValidUntil().isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.MEMBER_INACTIVE);
        }

        LoanPolicy policy = policies.getByMemberType(member.getMemberType());
        var outstanding = fineService.outstandingFor(member.getId());
        if (outstanding.signum() > 0 && outstanding.compareTo(policy.getBlockFineThreshold()) >= 0) {
            throw new BusinessException(ErrorCode.FINES_OUTSTANDING);
        }

        long activeReservations = reservations.countActiveByMemberId(member.getId());
        if (activeReservations >= policy.getMaxReservations()) {
            throw new BusinessException(ErrorCode.RESERVATION_LIMIT_REACHED);
        }
        if (reservations.existsActiveForMemberAndBook(member.getId(), book.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_RESERVED);
        }
        if (loans.existsActiveForMemberAndBook(member.getId(), book.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_BORROWED);
        }
        if (copies.countByBookIdAndStatus(book.getId(), CopyStatus.AVAILABLE) > 0) {
            throw new BusinessException(ErrorCode.COPIES_AVAILABLE);
        }

        Reservation res = new Reservation();
        res.setBook(book);
        res.setMember(member);
        res.setStatus(ReservationStatus.QUEUED);
        res.setQueuedAt(clock.instant());

        Reservation saved = reservations.save(res);
        log.info("Reservation {} created for member {} on book {}", saved.getId(), member.getId(), book.getId());
        return enrich(saved);
    }

    @Transactional
    public void cancelReservation(Long reservationId, Long memberId, boolean isStaff) {
        Reservation res = reservations.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!isStaff && !res.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        // Only live queue entries can be cancelled. Fulfilled reservations back an active
        // loan — cancelling one would flip an on-loan copy back to AVAILABLE.
        if (res.getStatus() != ReservationStatus.QUEUED && res.getStatus() != ReservationStatus.READY) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        BookCopy releasedCopy = res.getAssignedCopy();
        Long bookId = res.getBook().getId();
        res.cancel();
        if (releasedCopy != null) {
            assignNextReservation(bookId, releasedCopy, clock.instant());
        }
        reservations.save(res);
        log.info("Reservation {} cancelled", reservationId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getMemberReservations(Long memberId, Pageable pageable) {
        Page<Reservation> page = reservations.findByMemberId(memberId, pageable);
        return new PageResponse<>(enrichAll(page.getContent()), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getAllReservations(Pageable pageable) {
        Page<Reservation> page = reservations.findAllWithDetails(pageable);
        return new PageResponse<>(enrichAll(page.getContent()), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    private ReservationResponse enrich(Reservation reservation) {
        return enrichAll(java.util.List.of(reservation)).get(0);
    }

    /**
     * Server-computed queue positions and honest availability estimates.
     * Positions follow the same faculty-first, FIFO ordering as allocation.
     * The estimate is the earliest moment a copy could plausibly free up for this
     * requester — labelled an estimate in the UI, never a promise.
     */
    private List<ReservationResponse> enrichAll(List<Reservation> rows) {
        Map<Long, List<Long>> queues = new HashMap<>();
        Map<Long, Long> available = new HashMap<>();
        Map<Long, Optional<Instant>> earliestDue = new HashMap<>();
        Map<com.library360.member.MemberType, Integer> loanDays = new HashMap<>();
        Instant now = clock.instant();
        List<ReservationResponse> out = new java.util.ArrayList<>(rows.size());
        for (Reservation r : rows) {
            if (r.getStatus() != ReservationStatus.QUEUED || r.getBook() == null) {
                out.add(ReservationResponse.from(r));
                continue;
            }
            Long bookId = r.getBook().getId();
            List<Long> queue = queues.computeIfAbsent(bookId, reservations::orderedEligibleQueueIds);
            int index = queue.indexOf(r.getId());
            if (index < 0) {
                // Eligible when queued, but not currently (e.g. suspended) — no position shown.
                out.add(ReservationResponse.from(r));
                continue;
            }
            int position = index + 1;
            long free = available.computeIfAbsent(bookId,
                id -> copies.countByBookIdAndStatus(id, CopyStatus.AVAILABLE));
            Instant estimate = null;
            if (free >= position) {
                estimate = now;
            } else {
                Optional<Instant> earliest = earliestDue.computeIfAbsent(bookId, loans::earliestDueByBook);
                if (earliest.isPresent()) {
                    int deficit = position - (int) free;
                    int days = loanDays.computeIfAbsent(r.getMember().getMemberType(),
                        t -> policies.findByMemberType(t)
                            .map(com.library360.member.LoanPolicy::getLoanDays).orElse(20));
                    estimate = earliest.get().plus(days * (long) (deficit - 1), ChronoUnit.DAYS);
                }
            }
            out.add(ReservationResponse.from(r).withQueue(position, queue.size(), estimate));
        }
        return out;
    }

    @Transactional
    public void expireStaleHolds(Instant now) {
        List<Reservation> expired = reservations.findExpiredHolds(now);
        for (Reservation r : expired) {
            log.info("Expiring stale hold for reservation {}", r.getId());
            BookCopy releasedCopy = r.getAssignedCopy();
            Long bookId = r.getBook().getId();
            r.expire();
            if (releasedCopy != null) {
                assignNextReservation(bookId, releasedCopy, now);
            }
        }
    }


    /** Cancel outstanding queue entries and release held copies when a borrower loses eligibility. */
    @Transactional
    public void cancelAllForMember(Long memberId) {
        List<Reservation> active = reservations.findActiveByMemberId(memberId);
        for (Reservation reservation : active) {
            BookCopy assigned = reservation.getAssignedCopy();
            Long bookId = reservation.getBook().getId();
            reservation.cancel();
            if (assigned != null) {
                Optional<Reservation> next = findNextQueuedForUpdate(bookId);
                if (next.isPresent()) {
                    Reservation nextReservation = next.get();
                    nextReservation.markReady(assigned, clock.instant(),
                        clock.instant().plus(holdHoursFor(nextReservation.getMember().getMemberType()), ChronoUnit.HOURS));
                    assigned.setStatus(CopyStatus.ON_HOLD);
                    events.publishEvent(new ReservationReadyEvent(nextReservation.getId(), nextReservation.getMember().getUser().getId()));
                } else {
                    assigned.setStatus(CopyStatus.AVAILABLE);
                }
            }
        }
    }

    private void assignNextReservation(Long bookId, BookCopy copy, Instant now) {
        // Locked selection: concurrent returns/cancels/expiries serialize here so one
        // copy is never assigned to two reservations.
        Optional<Reservation> next = findNextQueuedForUpdate(bookId);
        if (next.isEmpty()) {
            copy.setStatus(CopyStatus.AVAILABLE);
            return;
        }

        Reservation reservation = next.get();
        reservation.markReady(copy, now,
            now.plus(holdHoursFor(reservation.getMember().getMemberType()), ChronoUnit.HOURS));
        copy.setStatus(CopyStatus.ON_HOLD);
        events.publishEvent(new ReservationReadyEvent(reservation.getId(), reservation.getMember().getUser().getId()));
        log.info("Copy {} reassigned to reservation {}", copy.getBarcode(), reservation.getId());
    }

    /** Pickup window for a member type; falls back to the 12-hour default when no policy row exists. */
    private int holdHoursFor(com.library360.member.MemberType memberType) {
        return policies.findByMemberType(memberType)
            .map(com.library360.member.LoanPolicy::getHoldHours).orElse(12);
    }
}
