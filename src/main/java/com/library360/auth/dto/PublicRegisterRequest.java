package com.library360.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Public registration requests are always pending review. Role names from clients are validated against Student/Faculty only. */
public record PublicRegisterRequest(
    @NotBlank @Email @Size(max = 160) String email,
    @NotBlank @Size(min = 6, max = 72) String password,
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName,
    @NotBlank String accountType,
    @Size(max = 30) String phoneNumber,
    LocalDate dateOfBirth,
    @Size(max = 40) String admissionNumber,
    @Size(max = 40) String rollNumber,
    @Size(max = 80) String branch,
    @Size(max = 30) String academicYear,
    @Size(max = 50) String className,
    @Size(max = 20) String section,
    @Size(max = 40) String facultyId,
    @Size(max = 40) String employeeNumber,
    @Size(max = 100) String department,
    @Size(max = 100) String designation
) {}
