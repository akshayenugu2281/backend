package com.library360.member;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loan_policies")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
public class LoanPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false, unique = true, length = 20)
    private MemberType memberType;

    @Column(name = "max_loans", nullable = false)
    private int maxLoans;

    @Column(name = "loan_days", nullable = false)
    private int loanDays;

    @Column(name = "max_renewals", nullable = false)
    private int maxRenewals;

    @Column(name = "max_reservations", nullable = false)
    private int maxReservations;

    @Column(name = "fine_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal finePerDay;

    @Column(name = "fine_cap", nullable = false, precision = 10, scale = 2)
    private BigDecimal fineCap;

    @Column(name = "block_fine_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal blockFineThreshold;

    @Column(name = "hold_hours", nullable = false)
    private int holdHours = 12;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
