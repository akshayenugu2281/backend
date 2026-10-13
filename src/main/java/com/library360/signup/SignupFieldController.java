package com.library360.signup;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class SignupFieldController {
    private final JdbcTemplate jdbc;
    public record FieldRule(String accountType, String fieldKey, String label, boolean enabled, boolean required, int sortOrder) {}
    private static final Set<String> TYPES = Set.of("STUDENT", "FACULTY");
    private static final Set<String> KEYS = Set.of("phoneNumber", "dateOfBirth", "admissionNumber", "rollNumber", "branch", "academicYear", "className", "section", "facultyId", "employeeNumber", "department", "designation");

    @GetMapping("/api/v1/auth/signup-fields")
    public List<FieldRule> publicFields(@RequestParam(defaultValue="STUDENT") String accountType) {
        String type = normalizeType(accountType);
        return jdbc.query("SELECT account_type,field_key,label,enabled,required_flag,sort_order FROM signup_field_settings WHERE account_type=? AND enabled=TRUE ORDER BY sort_order,id",
            (rs, i) -> new FieldRule(rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getBoolean(5), rs.getInt(6)), type);
    }

    @GetMapping("/api/v1/admin/signup-fields")
    public List<FieldRule> allFields(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        return jdbc.query("SELECT account_type,field_key,label,enabled,required_flag,sort_order FROM signup_field_settings ORDER BY account_type,sort_order,id",
            (rs, i) -> new FieldRule(rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getBoolean(5), rs.getInt(6)));
    }

    @PutMapping("/api/v1/admin/signup-fields")
    @Transactional
    public Map<String,Object> saveFields(@AuthenticationPrincipal Jwt jwt, @RequestBody List<FieldRule> input) {
        requireAdmin(jwt);
        if (input == null || input.size() > 60) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at most 60 signup field rules.");
        Set<String> seen = new HashSet<>();
        for (FieldRule f : input) {
            String type = normalizeType(f.accountType());
            if (!KEYS.contains(f.fieldKey()) || f.label() == null || f.label().isBlank() || f.label().length() > 100 || f.sortOrder() < 0 || f.sortOrder() > 1000)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A signup field rule is invalid.");
            String key = type + ":" + f.fieldKey();
            if (!seen.add(key)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate signup field rule: " + key);
            if (f.required() && !f.enabled()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A disabled field cannot be required.");
            int n = jdbc.update("UPDATE signup_field_settings SET label=?,enabled=?,required_flag=?,sort_order=?,updated_at=CURRENT_TIMESTAMP WHERE account_type=? AND field_key=?",
                f.label().trim(), f.enabled(), f.required(), f.sortOrder(), type, f.fieldKey());
            if (n == 0) {
                jdbc.update("INSERT INTO signup_field_settings(account_type,field_key,label,enabled,required_flag,sort_order,updated_at) VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                    type, f.fieldKey(), f.label().trim(), f.enabled(), f.required(), f.sortOrder());
            }
        }
        return Map.of("updated", input.size(), "message", "Signup form settings saved. Changes apply to new registrations.");
    }
    private static String normalizeType(String s) { String type = s == null ? "" : s.trim().toUpperCase(Locale.ROOT); if (!TYPES.contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account type must be STUDENT or FACULTY."); return type; }
    private static void requireAdmin(Jwt jwt) { List<String> a = jwt.getClaimAsStringList("authorities"); if (a == null || !a.contains("ROLE_ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator role required."); }
}
