package com.library360.reservation.dto;

import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
    @NotNull(message = "Book ID is required")
    Long bookId,
    Long memberId
) {}
