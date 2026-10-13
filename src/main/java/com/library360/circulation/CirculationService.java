package com.library360.circulation;

import com.library360.catalog.BookCopy;
import com.library360.catalog.BookCopyRepository;
import com.library360.catalog.CopyStatus;
import com.library360.circulation.dto.IssueRequest;
import com.library360.circulation.dto.LoanResponse;
import com.library360.circulation.dto.ReturnRequest;
import com.library360.circulation.dto.ReturnResponse;
import com.library360.common.BusinessException;
import com.library360.common.ErrorCode;
import com.library360.fine.Fine;
import com.library360.fine.FineService;
import com.library360.fine.dto.FineResponse;
import com.library360.member.*;
import com.library360.reservation.Reservation;
import com.library360.reservation.ReservationReadyEvent;
import com.library360.reservation.ReservationService;
import com.library360.user.User;
import com.library360.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CirculationService {

    private final LoanRepository loans;
    private final BookCopyRepository copies;
    private final MemberRepository members;
    private final LoanPolicyRepository policies;
    private final FineService fineService;
    private final ReservationService reservationService;
    private final UserRepository users;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @Transactional
    public LoanResponse issue(IssueRequest req, Long staffUserId) {
        BookCopy copy = copies.lockByBarcode(req.barcode())
                .orElseThrow(() -> new BusinessException(ErrorCode.COPY_NOT_FOUND));

        if (copy.getStatus() != CopyStatus.AVAILABLE && copy.getStatus() != CopyStatus.ON_HOLD) {
            throw new BusinessException(ErrorCode.COPY_NOT_AVAILABLE);
        }

        Member member = members.lockById(req.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        validateMemberEligibility(member);

        LoanPolicy policy = policies.getByMemberType(member.getMemberType());
        
        long activeLoans = loans.countActiveByMember(member.getId());
        if (activeLoans >= policy.getMaxLoans()) {
            throw new BusinessException(ErrorCode.LOAN_LIMIT_REACHED);
        }

        // Check if copy is on hold for someone else
        if (copy.getStatus() == CopyStatus.ON_HOLD) {
            Reservation res = reservationService.findReadyByCopy(copy.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COPY_NOT_AVAILABLE));

            if (!res.getMember().getId().equals(member.getId())) {
                throw new BusinessException(ErrorCode.COPY_ON_HOLD_FOR_ANOTHER);
            }
            res.fulfill(clock.instant());
        }

        // If not on hold, check if there are other people queued for this book
        if (copy.getStatus() == CopyStatus.AVAILABLE && reservationService.existsQueuedForBookExcluding(copy.getBook().getId(), member.getId())) {
            throw new BusinessException(ErrorCode.COPY_RESERVED_FOR_OTHERS);
        }

        User staff = users.findById(staffUserId).orElseThrow();
        Instant now = clock.instant();
        Instant due = now.plus(policy.getLoanDays(), ChronoUnit.DAYS);

        Loan loan = Loan.open(copy, member, now, due, staff);
        copy.setStatus(CopyStatus.ON_LOAN);

        loans.save(loan);
        
        events.publishEvent(new LoanIssuedEvent(loan.getId(), member.getUser().getId(), staffUserId));

        return LoanResponse.from(loan);
    }

    @Transactional
    public ReturnResponse returnBook(ReturnRequest req, Long staffUserId) {
        BookCopy copy = copies.lockByBarcode(req.barcode())
                .orElseThrow(() -> new BusinessException(ErrorCode.COPY_NOT_FOUND));

        Loan loan = loans.findOpenByCopy(copy.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_OPEN_LOAN));

        User staff = users.findById(staffUserId).orElseThrow();
        Instant now = clock.instant();

        loan.close(now, staff);
        
        // Final fine calculation
        Optional<Fine> finalFine = fineService.settleOverdue(loan, now);

        // Check if we need to put it on hold for the next person.
        // Locked selection so a concurrent cancel/expiry cannot assign the same copy twice.
        Optional<Reservation> nextRes = reservationService.findNextQueuedForUpdate(copy.getBook().getId());
        if (nextRes.isPresent()) {
            Reservation res = nextRes.get();
            int holdHours = policies.findByMemberType(res.getMember().getMemberType())
                .map(LoanPolicy::getHoldHours).orElse(12);
            res.markReady(copy, now, now.plus(holdHours, ChronoUnit.HOURS));
            copy.setStatus(CopyStatus.ON_HOLD);
            events.publishEvent(new ReservationReadyEvent(res.getId(), res.getMember().getUser().getId()));
        } else {
            copy.setStatus(CopyStatus.AVAILABLE);
        }

        events.publishEvent(new LoanReturnedEvent(loan.getId(), staffUserId));

        return new ReturnResponse(
            LoanResponse.from(loan),
            finalFine.map(FineResponse::from).orElse(null)
        );
    }

    @Transactional
    public LoanResponse renew(Long loanId, Long userId, boolean isStaff) {
        Loan loan = loans.lockById(loanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND));

        if (!isStaff && (loan.getMember().getUser() == null
                || !loan.getMember().getUser().getId().equals(userId))) {
            // Do not reveal whether another member's loan exists.
            throw new BusinessException(ErrorCode.LOAN_NOT_FOUND);
        }
        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new BusinessException(ErrorCode.NO_OPEN_LOAN);
        }

        Instant now = clock.instant();
        if (loan.isOverdue(now)) {
            throw new BusinessException(ErrorCode.LOAN_OVERDUE);
        }

        LoanPolicy policy = policies.getByMemberType(loan.getMember().getMemberType());
        if (loan.getRenewCount() >= policy.getMaxRenewals()) {
            throw new BusinessException(ErrorCode.RENEWAL_LIMIT_REACHED);
        }

        if (reservationService.existsQueuedForBook(loan.getCopy().getBook().getId())) {
            throw new BusinessException(ErrorCode.TITLE_HAS_RESERVATIONS);
        }

        loan.renew(java.time.Duration.ofDays(policy.getLoanDays()));
        loans.save(loan);
        log.info("Loan {} renewed by user {}", loanId, userId);
        return LoanResponse.from(loan);
    }

    @Transactional
    public int refreshOverdueAndFines(Instant now) {
        var candidates = loans.findOverdueCandidates(now);
        // Bound a single tick: a huge overdue backlog must not hold one long transaction.
        // Leftovers are picked up on the next scheduler run.
        List<Loan> batch = candidates.size() > 2000 ? candidates.subList(0, 2000) : candidates;
        if (candidates.size() > 2000) {
            log.warn("Overdue backlog {} exceeds per-tick cap; processing 2000 now", candidates.size());
        }
        for (Loan loan : batch) {
            loan.markOverdue();
            fineService.accrueOverdue(loan, now);
        }
        return batch.size();
    }

    @Transactional(readOnly = true)
    public com.library360.common.PageResponse<LoanResponse> findLoans(LoanStatus status, Long memberId, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Loan> page = loans.findLoans(status, memberId, pageable);
        return com.library360.common.PageResponse.from(page, LoanResponse::from);
    }

    private void validateMemberEligibility(Member member) {
        if (member.getStatus() != MemberStatus.ACTIVE || member.getUser() == null || !member.getUser().isEnabled()
                || !"APPROVED".equalsIgnoreCase(member.getUser().getApprovalStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_INACTIVE);
        }

        if (member.getValidUntil().isBefore(java.time.LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.MEMBER_INACTIVE); // Or create EXPIRED error
        }

        LoanPolicy policy = policies.getByMemberType(member.getMemberType());
        BigDecimal outstandingFines = fineService.outstandingFor(member.getId());
        
        if (outstandingFines.signum() > 0 && outstandingFines.compareTo(policy.getBlockFineThreshold()) >= 0) {
            throw new BusinessException(ErrorCode.FINES_OUTSTANDING);
        }
    }
}
