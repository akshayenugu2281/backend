package com.library360.fine;

import com.library360.circulation.Loan;
import com.library360.common.BusinessException;
import com.library360.common.ErrorCode;
import com.library360.fine.dto.FineResponse;
import com.library360.fine.dto.PayFineRequest;
import com.library360.fine.dto.WaiveFineRequest;
import com.library360.member.LoanPolicy;
import com.library360.member.LoanPolicyRepository;
import com.library360.user.User;
import com.library360.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FineService {

    private final FineRepository fines;
    private final PaymentRepository payments;
    private final LoanPolicyRepository policies;
    private final UserRepository users;
    private final Clock clock;

    public BigDecimal outstandingFor(Long memberId) {
        return fines.sumOutstandingByMember(memberId);
    }

    @Transactional
    public void accrueOverdue(Loan loan, Instant now) {
        LoanPolicy policy = policies.getByMemberType(loan.getMember().getMemberType());
        
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(loan.getDueAt(), now));
        if (daysLate == 0) return;

        BigDecimal amount = policy.getFinePerDay()
                .multiply(BigDecimal.valueOf(daysLate))
                .min(policy.getFineCap());

        Fine fine = fines.findByLoanAndReason(loan.getId(), FineReason.OVERDUE)
            .orElseGet(() -> {
                Fine f = new Fine();
                f.setLoan(loan);
                f.setMember(loan.getMember());
                f.setReason(FineReason.OVERDUE);
                return f;
            });

        // A waived fine is final. Otherwise, keep the balance synchronized while the loan
        // remains overdue; additional late days can reopen a previously fully paid balance.
        if (fine.getStatus() != FineStatus.WAIVED) {
            fine.setAmount(amount);
            int paidVsDue = fine.getPaidAmount().compareTo(amount);
            if (paidVsDue >= 0) {
                fine.setStatus(FineStatus.PAID);
            } else if (fine.getPaidAmount().signum() > 0) {
                fine.setStatus(FineStatus.PARTIAL);
            } else {
                fine.setStatus(FineStatus.OPEN);
            }
            fines.save(fine);
        }
    }

    @Transactional
    public Optional<Fine> settleOverdue(Loan loan, Instant now) {
        accrueOverdue(loan, now); // Final accrual calculation
        return fines.findByLoanAndReason(loan.getId(), FineReason.OVERDUE);
    }

    @Transactional
    public FineResponse payFine(Long fineId, PayFineRequest req, Long staffUserId) {
        Fine fine = fines.lockById(fineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FINE_NOT_FOUND));

        if (fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.WAIVED) {
            throw new BusinessException(ErrorCode.FINE_ALREADY_SETTLED);
        }

        BigDecimal balance = fine.getOutstandingBalance();
        if (req.amount().compareTo(balance) > 0) {
            throw new BusinessException(ErrorCode.PAYMENT_EXCEEDS_BALANCE);
        }

        User staff = users.findById(staffUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Payment payment = new Payment();
        payment.setFine(fine);
        payment.setAmount(req.amount());
        payment.setMethod(req.method());
        payment.setReceiptNo("RCP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        payment.setCollectedBy(staff);
        payment.setPaidAt(clock.instant());
        payments.save(payment);

        fine.setPaidAmount(fine.getPaidAmount().add(req.amount()));
        if (fine.getPaidAmount().compareTo(fine.getAmount()) >= 0) {
            fine.setStatus(FineStatus.PAID);
        } else {
            fine.setStatus(FineStatus.PARTIAL);
        }

        Fine saved = fines.save(fine);
        log.info("Payment of {} recorded for fine {} by staff {}", req.amount(), fineId, staffUserId);
        return FineResponse.from(saved);
    }

    @Transactional
    public FineResponse waiveFine(Long fineId, WaiveFineRequest req, Long staffUserId) {
        Fine fine = fines.lockById(fineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FINE_NOT_FOUND));

        if (fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.WAIVED) {
            throw new BusinessException(ErrorCode.FINE_ALREADY_SETTLED);
        }

        User staff = users.findById(staffUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        fine.setStatus(FineStatus.WAIVED);
        fine.setWaiveNote(req.reason());
        fine.setWaivedBy(staff);

        Fine saved = fines.save(fine);
        log.info("Fine {} waived by staff {} with note: {}", fineId, staffUserId, req.reason());
        return FineResponse.from(saved);
    }
}
