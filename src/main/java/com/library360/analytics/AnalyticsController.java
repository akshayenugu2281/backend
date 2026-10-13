package com.library360.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

@RestController @RequestMapping("/api/v1/analytics") @RequiredArgsConstructor
public class AnalyticsController {
  private final JdbcTemplate jdbc;
  @GetMapping("/overview")
  public Map<String,Object> overview(@AuthenticationPrincipal Jwt jwt) {
    requireStaff(jwt);
    Map<String,Object> counts=new LinkedHashMap<>();
    counts.put("totalTitles", scalar("SELECT COUNT(*) FROM books WHERE deleted=FALSE"));
    counts.put("totalCopies", scalar("SELECT COUNT(*) FROM book_copies c JOIN books b ON b.id=c.book_id WHERE b.deleted=FALSE"));
    counts.put("availableCopies", scalar("SELECT COUNT(*) FROM book_copies c JOIN books b ON b.id=c.book_id WHERE b.deleted=FALSE AND c.status='AVAILABLE'"));
    counts.put("issuedCopies", scalar("SELECT COUNT(*) FROM loans WHERE status IN ('ACTIVE','OVERDUE')"));
    counts.put("overdueLoans", scalar("SELECT COUNT(*) FROM loans WHERE status IN ('ACTIVE','OVERDUE') AND due_at<CURRENT_TIMESTAMP"));
    counts.put("dueToday", scalar("SELECT COUNT(*) FROM loans WHERE status IN ('ACTIVE','OVERDUE') AND CAST(due_at AS DATE)=CURRENT_DATE"));
    counts.put("activeReservations", scalar("SELECT COUNT(*) FROM reservations WHERE status IN ('QUEUED','READY')"));
    counts.put("activeMembers", scalar("SELECT COUNT(*) FROM members WHERE status='ACTIVE'"));
    counts.put("openFineBalance", decimal("SELECT COALESCE(SUM(GREATEST(amount-paid_amount,0)),0) FROM fines WHERE status IN ('OPEN','PARTIAL')"));
    counts.put("totalUsers", scalar("SELECT COUNT(*) FROM users"));
    counts.put("activeUsers", scalar("SELECT COUNT(*) FROM users WHERE enabled=TRUE AND approval_status='APPROVED'"));
    counts.put("pendingApprovals", scalar("SELECT COUNT(*) FROM users WHERE approval_status='PENDING'"));
    counts.put("suspendedUsers", scalar("SELECT COUNT(*) FROM users WHERE enabled=FALSE"));
    counts.put("studentUsers", scalar("SELECT COUNT(DISTINCT ur.user_id) FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.name='STUDENT'"));
    counts.put("facultyUsers", scalar("SELECT COUNT(DISTINCT ur.user_id) FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.name='FACULTY'"));
    counts.put("librarianUsers", scalar("SELECT COUNT(DISTINCT ur.user_id) FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.name='LIBRARIAN'"));
    counts.put("adminUsers", scalar("SELECT COUNT(DISTINCT ur.user_id) FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.name='ADMIN'"));
    counts.put("dueSoon", jdbc.queryForList("SELECT l.id loanId,u.id memberUserId,b.title bookTitle,u.full_name memberName,u.email,m.member_type memberType,m.member_code memberCode,u.admission_number admissionNumber,u.roll_number rollNumber,u.faculty_id facultyId,u.branch,u.class_name className,u.section_name section,l.due_at dueAt,GREATEST(CURRENT_DATE - CAST(l.due_at AS DATE),0) overdueDays,(SELECT COALESCE(SUM(GREATEST(f.amount-f.paid_amount,0)),0) FROM fines f WHERE f.loan_id=l.id AND f.status IN ('OPEN','PARTIAL')) outstandingFine FROM loans l JOIN members m ON m.id=l.member_id JOIN users u ON u.id=m.user_id JOIN book_copies c ON c.id=l.copy_id JOIN books b ON b.id=c.book_id WHERE u.enabled=TRUE AND u.approval_status='APPROVED' AND l.status IN ('ACTIVE','OVERDUE') AND l.due_at < CURRENT_TIMESTAMP + INTERVAL '7 day' ORDER BY l.due_at ASC LIMIT 100"));
    counts.put("monthlyCirculation", jdbc.queryForList("SELECT TO_CHAR(issued_at,'YYYY-MM') month,COUNT(*) issued FROM loans WHERE issued_at>=CURRENT_DATE - INTERVAL '6 month' GROUP BY TO_CHAR(issued_at,'YYYY-MM') ORDER BY month"));
    counts.put("popularBooks", jdbc.queryForList("SELECT b.id,b.title,COUNT(*) loanCount FROM loans l JOIN book_copies c ON c.id=l.copy_id JOIN books b ON b.id=c.book_id GROUP BY b.id,b.title ORDER BY loanCount DESC LIMIT 10"));
    return counts;
  }
  private Number scalar(String sql){Long n=jdbc.queryForObject(sql,Long.class);return n==null?0L:n;}
  private BigDecimal decimal(String sql){BigDecimal n=jdbc.queryForObject(sql,BigDecimal.class);return n==null?BigDecimal.ZERO:n;}
  private static void requireStaff(Jwt jwt){List<String>a=jwt.getClaimAsStringList("authorities");if(a==null||(!a.contains("ROLE_ADMIN")&&!a.contains("ROLE_LIBRARIAN")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Librarian or Admin role required.");}
}
