package com.library360.circulation;

public record LoanReturnedEvent(Long loanId, Long staffUserId) {}
