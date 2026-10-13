package com.library360.reservation;

public record ReservationReadyEvent(Long reservationId, Long userId) {}
