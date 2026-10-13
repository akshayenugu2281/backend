package com.library360.admin;

import com.library360.member.LoanPolicy;
import com.library360.member.LoanPolicyRepository;
import com.library360.member.MemberType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Admin-only operational controls for college circulation and overdue policy. */
@RestController
@RequestMapping("/api/v1/admin/loan-policies")
@RequiredArgsConstructor
public class LoanPolicyAdminController {
    private final LoanPolicyRepository policies;

    public record PolicyRequest(
        @Min(1) @Max(50) int maxLoans,
        @Min(1) @Max(365) int loanDays,
        @Min(0) @Max(20) int maxRenewals,
        @Min(0) @Max(100) int maxReservations,
        @NotNull @DecimalMin("0.00") @DecimalMax("10000.00") BigDecimal finePerDay,
        @NotNull @DecimalMin("0.00") @DecimalMax("100000.00") BigDecimal fineCap,
        @NotNull @DecimalMin("0.00") @DecimalMax("100000.00") BigDecimal blockFineThreshold,
        @Min(1) @Max(336) int holdHours
    ) {}
    public record PolicyResponse(String memberType, int maxLoans, int loanDays, int maxRenewals,
        int maxReservations, BigDecimal finePerDay, BigDecimal fineCap,
        BigDecimal blockFineThreshold, int holdHours) {
        static PolicyResponse from(LoanPolicy p) {
            return new PolicyResponse(p.getMemberType().name(), p.getMaxLoans(), p.getLoanDays(),
                p.getMaxRenewals(), p.getMaxReservations(), p.getFinePerDay(), p.getFineCap(),
                p.getBlockFineThreshold(), p.getHoldHours());
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<PolicyResponse> list() {
        return policies.findAll().stream()
            .filter(p -> p.getMemberType() == MemberType.STUDENT || p.getMemberType() == MemberType.FACULTY)
            .sorted(Comparator.comparing(p -> p.getMemberType().name()))
            .map(PolicyResponse::from).toList();
    }

    @PutMapping("/{type}")
    @Transactional
    public PolicyResponse update(@PathVariable String type, @Valid @RequestBody PolicyRequest request) {
        MemberType memberType;
        try { memberType = MemberType.valueOf(type.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy type must be STUDENT or FACULTY."); }
        if (memberType != MemberType.STUDENT && memberType != MemberType.FACULTY)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Student and Faculty policies can be configured here.");
        if (request.fineCap().compareTo(request.finePerDay()) < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fine cap cannot be lower than one day's fine.");
        LoanPolicy policy = policies.findByMemberType(memberType)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan policy is not configured for this account type."));
        policy.setMaxLoans(request.maxLoans()); policy.setLoanDays(request.loanDays());
        policy.setMaxRenewals(request.maxRenewals()); policy.setMaxReservations(request.maxReservations());
        policy.setFinePerDay(request.finePerDay()); policy.setFineCap(request.fineCap());
        policy.setBlockFineThreshold(request.blockFineThreshold()); policy.setHoldHours(request.holdHours());
        return PolicyResponse.from(policies.save(policy));
    }
}
