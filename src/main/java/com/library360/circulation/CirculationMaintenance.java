package com.library360.circulation;

import com.library360.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
@Slf4j
public class CirculationMaintenance {

    private final CirculationService circulationService;
    private final ReservationService reservationService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.maintenance.overdue-interval-ms:300000}", initialDelayString = "${app.maintenance.initial-delay-ms:30000}")
    public void accrueOverdueFines() {
        int updated = circulationService.refreshOverdueAndFines(clock.instant());
        if (updated > 0) {
            log.info("Refreshed overdue status and fine accrual for {} loan(s)", updated);
        }
    }

    @Scheduled(fixedDelayString = "${app.maintenance.reservation-expiry-interval-ms:60000}", initialDelayString = "${app.maintenance.initial-delay-ms:30000}")
    public void expireReservationHolds() {
        reservationService.expireStaleHolds(clock.instant());
    }
}
