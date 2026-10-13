package com.library360.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** In-app deadline alerts; no email/SMS credentials are required. Repeated runs are idempotent per user/type/day. */
@Component @RequiredArgsConstructor @Slf4j
public class NotificationMaintenance {
    private final JdbcTemplate jdbc;

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void sendDailyBookDeadlineAlerts() {
        try {
            int dueSoon = jdbc.update("INSERT INTO notifications(user_id,type,title,body,link,`read`,created_at) " +
                "SELECT DISTINCT u.id,'DUE_SOON_REMINDER','Book due soon','A book on your library account is due within the next 48 hours. Open My Loans to confirm the title and due date, then return or renew it if eligible.','/circulation',FALSE,NOW(6) " +
                "FROM users u JOIN members m ON m.user_id=u.id JOIN loans l ON l.member_id=m.id " +
                "WHERE u.enabled=TRUE AND u.approval_status='APPROVED' AND l.status IN ('ACTIVE','OVERDUE') " +
                "AND l.due_at>=NOW(6) AND l.due_at<DATE_ADD(NOW(6),INTERVAL 48 HOUR) " +
                "AND NOT EXISTS(SELECT 1 FROM notifications n WHERE n.user_id=u.id AND n.type='DUE_SOON_REMINDER' AND DATE(n.created_at)=CURRENT_DATE())");
            int overdue = jdbc.update("INSERT INTO notifications(user_id,type,title,body,link,`read`,created_at) " +
                "SELECT DISTINCT u.id,'OVERDUE_REMINDER','Library book overdue','One or more books on your account are overdue. Please return them or ask the librarian about renewal and the applicable late fee.','/circulation',FALSE,NOW(6) " +
                "FROM users u JOIN members m ON m.user_id=u.id JOIN loans l ON l.member_id=m.id " +
                "WHERE u.enabled=TRUE AND u.approval_status='APPROVED' AND l.status IN ('ACTIVE','OVERDUE') AND l.due_at<NOW(6) " +
                "AND NOT EXISTS(SELECT 1 FROM notifications n WHERE n.user_id=u.id AND n.type='OVERDUE_REMINDER' AND DATE(n.created_at)=CURRENT_DATE())");
            log.info("Library deadline alerts created: dueSoon={}, overdue={}", dueSoon, overdue);
        } catch (Exception ex) {
            // Database unavailability must not crash application startup/scheduling thread.
            log.error("Daily library deadline alerts failed. Verify database connectivity and migrations.", ex);
        }
    }
}
