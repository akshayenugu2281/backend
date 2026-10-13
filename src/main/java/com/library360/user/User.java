package com.library360.user;

import com.library360.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "admission_number", length = 40, unique = true) private String admissionNumber;
    @Column(name = "roll_number", length = 40, unique = true) private String rollNumber;
    @Column(name = "branch", length = 80) private String branch;
    @Column(name = "academic_year", length = 30) private String academicYear;
    @Column(name = "class_name", length = 50) private String className;
    @Column(name = "section_name", length = 20) private String section;
    @Column(name = "date_of_birth") private java.time.LocalDate dateOfBirth;
    @Column(name = "phone_number", length = 30) private String phoneNumber;
    @Column(name = "profile_updated_at") private Instant profileUpdatedAt;
    @Column(name = "first_name", length = 80) private String firstName;
    @Column(name = "last_name", length = 80) private String lastName;
    @Column(name = "faculty_id", length = 40, unique = true) private String facultyId;
    @Column(name = "employee_number", length = 40, unique = true) private String employeeNumber;
    @Column(name = "designation", length = 100) private String designation;
    @Column(name = "department", length = 100) private String department;
    @Column(name = "approval_status", nullable = false, length = 20) private String approvalStatus = "APPROVED";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "failed_logins", nullable = false)
    private int failedLogins = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public boolean isLocked() {
        return isLocked(Instant.now());
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailedLogin(int maxAttempts, java.time.Duration lockDuration) {
        recordFailedLogin(maxAttempts, lockDuration, Instant.now());
    }

    public void recordFailedLogin(int maxAttempts, java.time.Duration lockDuration, Instant now) {
        this.failedLogins++;
        if (this.failedLogins >= maxAttempts) {
            this.lockedUntil = now.plus(lockDuration);
        }
    }

    public void resetFailedLogins() {
        this.failedLogins = 0;
        this.lockedUntil = null;
    }
}
