package com.library360.fine.dto;

import com.library360.fine.Fine;
import com.library360.fine.FineStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record FineResponse(
    Long id,
    Long loanId,
    Long memberId,
    String memberCode,
    String memberName,
    String reason,
    BigDecimal amount,
    BigDecimal paidAmount,
    FineStatus status,
    Instant createdAt
) {
    public static FineResponse from(Fine fine) {
        return FineResponse.builder()
            .id(fine.getId())
            .loanId(fine.getLoan() != null ? fine.getLoan().getId() : null)
            .memberId(fine.getMember().getId())
            .memberCode(fine.getMember().getMemberCode())
            .memberName(fine.getMember().getUser().getFullName())
            .reason(fine.getReason().name())
            .amount(fine.getAmount())
            .paidAmount(fine.getPaidAmount())
            .status(fine.getStatus())
            .createdAt(fine.getCreatedAt())
            .build();
    }
}
