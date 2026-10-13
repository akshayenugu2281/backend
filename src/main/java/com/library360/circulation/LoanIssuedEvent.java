package com.library360.circulation;

public record LoanIssuedEvent(Long loanId, Long userId, Long staffUserId) {}
