package com.library360.approval;

import com.library360.member.Member;
import com.library360.member.MemberRepository;
import com.library360.user.User;
import com.library360.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {
    private final UserRepository users;
    private final MemberRepository members;
    private final JdbcTemplate jdbc;
    public record PendingAccount(Long id,String email,String fullName,String accountType,String phoneNumber,String admissionNumber,String rollNumber,String facultyId,String employeeNumber,String department,String designation,String branch,String academicYear,String className,String section,String createdAt) {}
    public record DecisionRequest(String decision, String note) {}

    @GetMapping
    public List<PendingAccount> pending(@AuthenticationPrincipal Jwt jwt) {
        requireStaff(jwt);
        return jdbc.query("SELECT u.id,u.email,u.full_name,COALESCE(r.name,'UNKNOWN') account_type,u.phone_number,u.admission_number,u.roll_number,u.faculty_id,u.employee_number,u.department,u.designation,u.branch,u.academic_year,u.class_name,u.section_name AS section,u.created_at FROM users u LEFT JOIN user_roles ur ON ur.user_id=u.id LEFT JOIN roles r ON r.id=ur.role_id WHERE u.approval_status='PENDING' ORDER BY u.created_at ASC",
          (rs,i)->new PendingAccount(rs.getLong("id"),rs.getString("email"),rs.getString("full_name"),rs.getString("account_type"),rs.getString("phone_number"),rs.getString("admission_number"),rs.getString("roll_number"),rs.getString("faculty_id"),rs.getString("employee_number"),rs.getString("department"),rs.getString("designation"),rs.getString("branch"),rs.getString("academic_year"),rs.getString("class_name"),rs.getString("section"),rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toInstant().toString()));
    }

    @PatchMapping("/{id}")
    @Transactional
    public Map<String,Object> decide(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestBody DecisionRequest req) {
        requireStaff(jwt);
        User user = users.findByIdWithRoles(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found."));
        if (!"PENDING".equalsIgnoreCase(user.getApprovalStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"This registration has already been reviewed.");
        String decision = req == null || req.decision() == null ? "" : req.decision().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVE","REJECT").contains(decision)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Decision must be APPROVE or REJECT.");
        if (decision.equals("APPROVE")) {
            user.setApprovalStatus("APPROVED"); user.setEnabled(true); users.save(user);
            members.findByUserId(user.getId()).ifPresent(m -> { m.setStatus(com.library360.member.MemberStatus.ACTIVE); members.save(m); });
            jdbc.update("INSERT INTO notifications(user_id,type,title,body,link,\"read\",created_at) VALUES(?,?,?,?,?,FALSE,CURRENT_TIMESTAMP)", user.getId(), "ACCOUNT_APPROVED", "Library account approved", "Your college library account has been verified. You can now sign in to Library360.", "/login");
            return Map.of("id",id,"status","APPROVED","message","Account approved. The user can now sign in.");
        }
        user.setApprovalStatus("REJECTED"); user.setEnabled(false); users.save(user);
        members.findByUserId(user.getId()).ifPresent(m -> { m.setStatus(com.library360.member.MemberStatus.SUSPENDED); members.save(m); });
        jdbc.update("INSERT INTO notifications(user_id,type,title,body,link,\"read\",created_at) VALUES(?,?,?,?,?,FALSE,CURRENT_TIMESTAMP)", user.getId(), "ACCOUNT_REJECTED", "Library account requires follow-up", "Your registration was not approved. Please contact the library office for clarification.", "/help");
        return Map.of("id",id,"status","REJECTED","message","Registration rejected and sign-in disabled.");
    }
    private static void requireStaff(Jwt jwt) { List<String>a=jwt.getClaimAsStringList("authorities");if(a==null||(!a.contains("ROLE_ADMIN")&&!a.contains("ROLE_LIBRARIAN")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Librarian or Admin role required."); }
}
