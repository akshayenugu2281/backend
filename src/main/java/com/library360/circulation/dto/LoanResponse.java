package com.library360.circulation.dto;

import com.library360.circulation.Loan;
import com.library360.circulation.LoanStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record LoanResponse(
    Long id,
    Long copyId,
    String copyBarcode,
    String barcode,
    Long bookId,
    String bookTitle,
    String bookIsbn,
    Long memberId,
    String memberCode,
    String memberName,
    Instant issuedAt,
    Instant dueAt,
    Instant returnedAt,
    int renewCount,
    LoanStatus status
) {
    public static LoanResponse from(Loan loan) {
        var copy = loan.getCopy();
        var book = copy.getBook();
        var member = loan.getMember();
        return LoanResponse.builder()
            .id(loan.getId())
            .copyId(copy.getId())
            .copyBarcode(copy.getBarcode())
            .barcode(copy.getBarcode())
            .bookId(book.getId())
            .bookTitle(book.getTitle())
            .bookIsbn(book.getIsbn13())
            .memberId(member.getId())
            .memberCode(member.getMemberCode())
            .memberName(member.getUser().getFullName())
            .issuedAt(loan.getIssuedAt())
            .dueAt(loan.getDueAt())
            .returnedAt(loan.getReturnedAt())
            .renewCount(loan.getRenewCount())
            .status(loan.getStatus())
            .build();
    }
}
