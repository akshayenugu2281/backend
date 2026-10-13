package com.library360.auth;

import com.library360.auth.dto.LoginRequest;
import com.library360.auth.dto.MeResponse;
import com.library360.auth.dto.PublicRegisterRequest;
import com.library360.auth.dto.TokenResponse;
import com.library360.config.JwtProperties;
import com.library360.member.Member;
import com.library360.member.MemberRepository;
import com.library360.member.MemberStatus;
import com.library360.member.MemberType;
import com.library360.user.RoleRepository;
import com.library360.user.User;
import com.library360.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String COOKIE_NAME = "l360_rt";
    private final AuthService authService;
    private final UserRepository users;
    private final RoleRepository roles;
    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokens;
    private final JdbcTemplate jdbc;
    private final JwtProperties props;

    @Value("${app.auth.refresh-cookie-secure:true}")
    private boolean secureCookie;

    @Value("${app.auth.refresh-cookie-same-site:}")
    private String cookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest http) {
        
        var result = authService.login(req, http.getHeader("User-Agent"), getClientIp(http));
        return withCookie(result);
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody PublicRegisterRequest req) {
        String type = req.accountType().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("STUDENT", "FACULTY").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Public signup supports Student and Faculty only. Librarian/Admin accounts must be provisioned by an authorized administrator.");
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        String password = req.password();
        if (!password.matches("(?=.*[A-Za-z])(?=.*[0-9]).{6,72}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Password must be at least 6 characters and include both letters and numbers.");
        }
        if (req.dateOfBirth() != null && (req.dateOfBirth().isAfter(LocalDate.now().minusYears(16)) || req.dateOfBirth().isBefore(LocalDate.now().minusYears(100)))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid date of birth.");
        }
        Map<String, String> supplied = Map.ofEntries(
            Map.entry("phoneNumber", Objects.requireNonNullElse(clean(req.phoneNumber()), "")),
            Map.entry("dateOfBirth", req.dateOfBirth() == null ? "" : req.dateOfBirth().toString()),
            Map.entry("admissionNumber", Objects.requireNonNullElse(clean(req.admissionNumber()), "")),
            Map.entry("rollNumber", Objects.requireNonNullElse(clean(req.rollNumber()), "")),
            Map.entry("branch", Objects.requireNonNullElse(clean(req.branch()), "")),
            Map.entry("academicYear", Objects.requireNonNullElse(clean(req.academicYear()), "")),
            Map.entry("className", Objects.requireNonNullElse(clean(req.className()), "")),
            Map.entry("section", Objects.requireNonNullElse(clean(req.section()), "")),
            Map.entry("facultyId", Objects.requireNonNullElse(clean(req.facultyId()), "")),
            Map.entry("employeeNumber", Objects.requireNonNullElse(clean(req.employeeNumber()), "")),
            Map.entry("department", Objects.requireNonNullElse(clean(req.department()), "")),
            Map.entry("designation", Objects.requireNonNullElse(clean(req.designation()), ""))
        );
        var required = jdbc.query("SELECT field_key,label FROM signup_field_settings WHERE account_type=? AND enabled=TRUE AND required_flag=TRUE",
            (rs, rowNum) -> Map.entry(rs.getString("field_key"), rs.getString("label")), type);
        for (var field : required) {
            String value = supplied.getOrDefault(field.getKey(), "");
            if (value == null || value.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field.getValue() + " is required.");
        }
        if (clean(req.admissionNumber()) != null && users.existsByAdmissionNumberIgnoreCase(req.admissionNumber().trim())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Admission number is already registered.");
        if (clean(req.rollNumber()) != null && users.existsByRollNumberIgnoreCase(req.rollNumber().trim())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Roll number is already registered.");
        if (clean(req.facultyId()) != null && users.existsByFacultyIdIgnoreCase(req.facultyId().trim())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Faculty ID is already registered.");
        if (clean(req.employeeNumber()) != null && users.existsByEmployeeNumberIgnoreCase(req.employeeNumber().trim())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee number is already registered.");
        String fullName = (req.firstName().trim() + " " + req.lastName().trim()).trim();
        if (fullName.length() > 120) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name and last name together must be 120 characters or fewer.");
        User user = new User(email, passwordEncoder.encode(password), fullName);
        user.setFirstName(req.firstName().trim()); user.setLastName(req.lastName().trim());
        user.setPhoneNumber(clean(req.phoneNumber())); user.setDateOfBirth(req.dateOfBirth());
        user.setAdmissionNumber(clean(req.admissionNumber())); user.setRollNumber(clean(req.rollNumber()));
        user.setBranch(clean(req.branch())); user.setAcademicYear(clean(req.academicYear()));
        user.setClassName(clean(req.className())); user.setSection(clean(req.section()));
        user.setFacultyId(clean(req.facultyId())); user.setEmployeeNumber(clean(req.employeeNumber()));
        user.setDepartment(clean(req.department())); user.setDesignation(clean(req.designation()));
        user.setApprovalStatus("PENDING");
        var role = roles.findByName(type).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Account role configuration is missing."));
        user.getRoles().add(role);
        users.saveAndFlush(user);
        String memberCode = (type.equals("STUDENT") ? "STU-" : "FAC-") + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        Member member = new Member(); member.setUser(user); member.setMemberCode(memberCode);
        member.setMemberType(type.equals("STUDENT") ? MemberType.STUDENT : MemberType.FACULTY);
        member.setStatus(MemberStatus.ACTIVE); member.setDepartment(clean(type.equals("FACULTY") ? req.department() : req.branch()));
        member.setPhone(clean(req.phoneNumber())); member.setValidUntil(LocalDate.now().plusYears(type.equals("STUDENT") ? 4 : 10));
        members.save(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Registration submitted. Your account must be verified by the library team before sign-in.",
            "accountType", type, "approvalStatus", "PENDING", "email", email));
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record ForgotPasswordRequest(
        @jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank String email,
        String phoneNumber,
        java.time.LocalDate dateOfBirth,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 6, max = 72) String newPassword) {}

    @PostMapping("/forgot-password")
    @Transactional(noRollbackFor = org.springframework.web.server.ResponseStatusException.class)
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found for that email address."));
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is disabled. Contact the library office.");
        }
        if (!"APPROVED".equalsIgnoreCase(user.getApprovalStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has not been approved yet. Contact the library office for help signing in.");
        }
        if (user.isLocked(java.time.Instant.now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Too many failed attempts. Try again later.");
        }
        boolean phoneOnFile = user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank();
        boolean dobOnFile = user.getDateOfBirth() != null;
        if (!phoneOnFile && !dobOnFile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "We cannot verify this account automatically. Contact the library office to reset your password.");
        }
        // Every identity anchor on file must match. One generic message avoids
        // revealing which factor failed.
        String phone = clean(req.phoneNumber());
        boolean verified = true;
        if (phoneOnFile) {
            verified = phone != null && phone.equals(user.getPhoneNumber().trim()) && verified;
        }
        if (dobOnFile) {
            verified = req.dateOfBirth() != null && req.dateOfBirth().equals(user.getDateOfBirth()) && verified;
        }
        if (!verified) {
            user.recordFailedLogin(5, java.time.Duration.ofMinutes(15), java.time.Instant.now());
            users.save(user);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification failed. Check your details and try again.");
        }
        String newPassword = req.newPassword();
        if (!newPassword.matches("(?=.*[A-Za-z])(?=.*[0-9]).{6,72}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use at least 6 characters with both letters and numbers.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a password different from your current password.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.resetFailedLogins();
        users.save(user);
        refreshTokens.revokeAllForUser(user.getId());
        return Map.of("message", "Your password has been reset. Sign in with your new password.");
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = COOKIE_NAME, required = false) String rt,
            HttpServletRequest http) {
        
        var result = authService.refresh(rt, http.getHeader("User-Agent"), getClientIp(http));
        return withCookie(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE_NAME, required = false) String rt) {
        authService.logout(rt);
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO))
            .build();
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new MeResponse(
            Long.valueOf(jwt.getSubject()),
            jwt.getClaimAsString("email"),
            jwt.getClaimAsString("name"),
            jwt.getClaimAsStringList("authorities")
        );
    }

    private ResponseEntity<TokenResponse> withCookie(AuthService.AuthResult r) {
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie(r.refreshToken(), props.refreshTtl()))
            .body(TokenResponse.builder()
                .accessToken(r.accessToken())
                .expiresIn(props.accessTtl().toSeconds())
                .user(r.user())
                .build());
    }

    private String cookie(String value, Duration maxAge) {
        String sameSite = (cookieSameSite != null && !cookieSameSite.isBlank())
            ? cookieSameSite
            : (secureCookie ? "None" : "Lax");
        return ResponseCookie.from(COOKIE_NAME, value)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite(sameSite)
            .path("/api/v1/auth")
            .maxAge(maxAge)
            .build()
            .toString();
    }

    private String getClientIp(HttpServletRequest request) {
        // Only trust the servlet container's resolved remote address. Forwarded headers
        // must be normalized by a configured trusted reverse proxy, not accepted blindly.
        return request.getRemoteAddr();
    }
}
