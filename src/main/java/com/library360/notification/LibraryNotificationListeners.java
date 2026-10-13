package com.library360.notification;

import com.library360.circulation.LoanIssuedEvent;
import com.library360.circulation.LoanReturnedEvent;
import com.library360.reservation.ReservationReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.sql.Timestamp;

/** Sends event-driven, private in-app notices only after the circulation transaction commits. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LibraryNotificationListeners {
    private final JdbcTemplate jdbc;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanIssued(LoanIssuedEvent event) {
        safely("issued loan " + event.loanId(), () -> {
            var rows = jdbc.query("SELECT b.title,l.due_at FROM loans l JOIN book_copies c ON c.id=l.copy_id JOIN books b ON b.id=c.book_id WHERE l.id=?",
                (rs,n) -> new String[]{rs.getString("title"), rs.getTimestamp("due_at").toInstant().toString()}, event.loanId());
            if (!rows.isEmpty()) insert(event.userId(), "LOAN_ISSUED", "Book issued successfully",
                "'" + rows.get(0)[0] + "' was issued to your library account. Due date: " + rows.get(0)[1] + ". Open My Loans for details.", "/circulation");
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanReturned(LoanReturnedEvent event) {
        safely("returned loan " + event.loanId(), () -> {
            var rows = jdbc.query("SELECT m.user_id,b.title FROM loans l JOIN members m ON m.id=l.member_id JOIN book_copies c ON c.id=l.copy_id JOIN books b ON b.id=c.book_id WHERE l.id=?",
                (rs,n) -> new Object[]{rs.getLong("user_id"), rs.getString("title")}, event.loanId());
            if (!rows.isEmpty()) insert((Long)rows.get(0)[0], "LOAN_RETURNED", "Book return recorded",
                "The return of '" + rows.get(0)[1] + "' has been recorded. Check your fines page for any outstanding balance.", "/fines");
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationReady(ReservationReadyEvent event) {
        safely("ready reservation " + event.reservationId(), () -> {
            var rows = jdbc.query("SELECT b.title,r.expires_at FROM reservations r JOIN books b ON b.id=r.book_id WHERE r.id=?",
                (rs,n) -> new String[]{rs.getString("title"), rs.getTimestamp("expires_at") == null ? "" : rs.getTimestamp("expires_at").toInstant().toString()}, event.reservationId());
            if (!rows.isEmpty()) insert(event.userId(), "RESERVATION_READY", "Reserved book is ready",
                "'" + rows.get(0)[0] + "' is ready for collection. Please collect it before " + rows.get(0)[1] + ".", "/reservations");
        });
    }

    private void insert(Long userId, String type, String title, String body, String link) {
        jdbc.update("INSERT INTO notifications(user_id,type,title,body,link,\"read\",created_at) VALUES(?,?,?,?,?,FALSE,CURRENT_TIMESTAMP)",
            userId, type, title.length()>150?title.substring(0,150):title, body.length()>500?body.substring(0,500):body, link);
    }

    private void safely(String operation, Runnable action) {
        try { action.run(); } catch (Exception ex) { log.error("Could not create notification after {}", operation, ex); }
    }
}
