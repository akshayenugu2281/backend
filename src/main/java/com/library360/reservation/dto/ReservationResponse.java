package com.library360.reservation.dto;

import com.library360.reservation.Reservation;
import com.library360.reservation.ReservationStatus;

import java.time.Instant;

public record ReservationResponse(
    Long id,
    Long bookId,
    String bookTitle,
    String bookIsbn,
    Long memberId,
    String memberCode,
    String memberName,
    ReservationStatus status,
    Instant queuedAt,
    Instant readyAt,
    Instant expiresAt,
    String assignedBarcode,
    String assignedAccession,
    Integer queuePosition,
    int queueLength,
    Instant estimatedAvailableAt
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
            r.getId(),
            r.getBook() != null ? r.getBook().getId() : null,
            r.getBook() != null ? r.getBook().getTitle() : null,
            r.getBook() != null ? r.getBook().getIsbn13() : null,
            r.getMember() != null ? r.getMember().getId() : null,
            r.getMember() != null ? r.getMember().getMemberCode() : null,
            (r.getMember() != null && r.getMember().getUser() != null) ? r.getMember().getUser().getFullName() : null,
            r.getStatus(),
            r.getQueuedAt(),
            r.getReadyAt(),
            r.getExpiresAt(),
            r.getAssignedCopy() != null ? r.getAssignedCopy().getBarcode() : null,
            r.getAssignedCopy() != null ? r.getAssignedCopy().getAccessionNumber() : null,
            null,
            0,
            null
        );
    }

    public ReservationResponse withQueue(int position, int length, Instant estimatedAvailableAt) {
        return new ReservationResponse(
            id, bookId, bookTitle, bookIsbn, memberId, memberCode, memberName, status,
            queuedAt, readyAt, expiresAt, assignedBarcode, assignedAccession,
            position, length, estimatedAvailableAt);
    }
}
