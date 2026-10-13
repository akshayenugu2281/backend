package com.library360.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final JdbcTemplate jdbc;

    public record SendRequest(Long userId, String title, String body, String link, boolean allStudents) {}
    public record Notice(Long id, String type, String title, String body, String link, boolean read, String createdAt) {}
    public record Recipient(long userId, String fullName, String email, String memberCode,
                            String memberType, String phoneNumber, String collegeId, String department) {}

    @GetMapping
    public List<Notice> mine(@AuthenticationPrincipal Jwt jwt) {
        long id = Long.parseLong(jwt.getSubject());
        return jdbc.query("SELECT id,type,title,body,link,`read`,created_at FROM notifications WHERE user_id=? ORDER BY created_at DESC LIMIT 100",
            (rs,n) -> new Notice(rs.getLong("id"), rs.getString("type"), rs.getString("title"), rs.getString("body"),
                rs.getString("link"), rs.getBoolean("read"), rs.getTimestamp("created_at").toInstant().toString()), id);
    }

    /** Staff-only, bounded recipient search. It returns only the minimum identity fields needed to address a reminder. */
    @GetMapping("/recipients")
    public List<Recipient> recipients(@AuthenticationPrincipal Jwt jwt, @RequestParam(required=false) String q) {
        requireStaff(jwt);
        String term = clean(q);
        String pattern = term == null ? null : "%" + term.toLowerCase() + "%";
        return jdbc.query("SELECT DISTINCT u.id,u.full_name,u.email,m.member_code,m.member_type," +
                "COALESCE(u.phone_number,m.phone) AS phone_number," +
                "COALESCE(u.admission_number,u.roll_number,u.faculty_id,u.employee_number,m.member_code) AS college_id," +
                "COALESCE(u.department,u.branch,'') AS department " +
                "FROM users u JOIN members m ON m.user_id=u.id " +
                "JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id " +
                "WHERE u.enabled=TRUE AND u.approval_status='APPROVED' AND m.status='ACTIVE' " +
                "AND r.name IN ('STUDENT','FACULTY') " +
                "AND (? IS NULL OR LOWER(u.full_name) LIKE ? OR LOWER(u.email) LIKE ? OR " +
                "LOWER(COALESCE(u.admission_number,'')) LIKE ? OR LOWER(COALESCE(u.roll_number,'')) LIKE ? OR " +
                "LOWER(COALESCE(u.faculty_id,'')) LIKE ? OR LOWER(COALESCE(u.employee_number,'')) LIKE ? OR " +
                "LOWER(m.member_code) LIKE ?) ORDER BY u.full_name LIMIT 50",
            (rs,n) -> new Recipient(rs.getLong("id"), rs.getString("full_name"), rs.getString("email"),
                rs.getString("member_code"), rs.getString("member_type"), rs.getString("phone_number"),
                rs.getString("college_id"), rs.getString("department")), pattern, pattern, pattern, pattern, pattern, pattern, pattern, pattern);
    }

    @PostMapping("/{id}/read")
    public Map<String,Boolean> read(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        int n=jdbc.update("UPDATE notifications SET `read`=TRUE,read_at=NOW(6) WHERE id=? AND user_id=?",id,Long.parseLong(jwt.getSubject()));
        if(n==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found.");
        return Map.of("read",true);
    }

    @PostMapping("/remind-overdue")
    public Map<String,Object> remindOverdue(@AuthenticationPrincipal Jwt jwt) {
        requireStaff(jwt);
        int count=jdbc.update("INSERT INTO notifications(user_id,type,title,body,link,`read`,created_at) " +
            "SELECT DISTINCT u.id,'OVERDUE_REMINDER','Library book overdue','One or more books linked to your account are past their due date. Please return them or contact the librarian to discuss renewal and applicable fines.','/circulation',FALSE,NOW(6) " +
            "FROM users u JOIN members m ON m.user_id=u.id JOIN loans l ON l.member_id=m.id " +
            "WHERE u.enabled=TRUE AND u.approval_status='APPROVED' AND l.status IN ('ACTIVE','OVERDUE') AND l.due_at<NOW(6) " +
            "AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.user_id=u.id AND n.type='OVERDUE_REMINDER' AND n.created_at>=DATE_SUB(NOW(6),INTERVAL 1 DAY))");
        return Map.of("sent",count);
    }

    @PostMapping("/remind-upcoming")
    public Map<String,Object> remindUpcoming(@AuthenticationPrincipal Jwt jwt) {
        requireStaff(jwt);
        int count=jdbc.update("INSERT INTO notifications(user_id,type,title,body,link,`read`,created_at) " +
            "SELECT DISTINCT u.id,'DUE_SOON_REMINDER','Book due soon','A book on your account is due within 48 hours. Please check My Loans and return or renew the item if eligible.','/circulation',FALSE,NOW(6) " +
            "FROM users u JOIN members m ON m.user_id=u.id JOIN loans l ON l.member_id=m.id " +
            "WHERE u.enabled=TRUE AND u.approval_status='APPROVED' AND l.status IN ('ACTIVE','OVERDUE') AND l.due_at>=NOW(6) AND l.due_at<DATE_ADD(NOW(6),INTERVAL 48 HOUR) " +
            "AND NOT EXISTS(SELECT 1 FROM notifications n WHERE n.user_id=u.id AND n.type='DUE_SOON_REMINDER' AND n.created_at>=DATE_SUB(NOW(6),INTERVAL 24 HOUR))");
        return Map.of("sent",count);
    }

    @PostMapping("/send")
    public Map<String,Object> send(@AuthenticationPrincipal Jwt jwt, @RequestBody SendRequest req) {
        requireStaff(jwt);
        if (req == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Notification body is required.");
        String title=clean(req.title()), body=clean(req.body());
        if(title==null||title.length()>150||body==null||body.length()>500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Title (1–150 characters) and message (1–500 characters) are required.");
        String link=clean(req.link());
        if(link!=null&&(link.length()>200||!link.startsWith("/")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Link must be a relative in-app path up to 200 characters.");
        int count;
        if(req.allStudents()) {
            count=jdbc.update("INSERT INTO notifications(user_id,type,title,body,link,`read`,created_at) " +
                "SELECT DISTINCT u.id,'LIBRARY_REMINDER',?,?,?,FALSE,NOW(6) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id " +
                "WHERE r.name='STUDENT' AND u.enabled=TRUE AND u.approval_status='APPROVED'",title,body,link);
        } else {
            if(req.userId()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a recipient or select all students.");
            count=jdbc.update("INSERT INTO notifications(user_id,type,title,body,link,`read`,created_at) " +
                "SELECT DISTINCT u.id,'LIBRARY_REMINDER',?,?,?,FALSE,NOW(6) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id " +
                "JOIN members m ON m.user_id=u.id WHERE u.id=? AND u.enabled=TRUE AND u.approval_status='APPROVED' AND m.status='ACTIVE' AND r.name IN ('STUDENT','FACULTY')",
                title,body,link,req.userId());
            if(count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active recipient not found.");
        }
        return Map.of("sent",count);
    }

    private static void requireStaff(Jwt jwt) {
        List<String> authorities=jwt.getClaimAsStringList("authorities");
        if(authorities==null||(!authorities.contains("ROLE_ADMIN")&&!authorities.contains("ROLE_LIBRARIAN")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Librarian or Admin role required.");
    }
    private static String clean(String s) { return s==null||s.isBlank()?null:s.trim(); }
}
