package com.library360.member.dto;

import com.library360.member.MemberType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterMemberRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    String password,

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name must be at most 120 characters")
    String fullName,

    @NotBlank(message = "Member code is required")
    @Size(max = 30, message = "Member code must be at most 30 characters")
    String memberCode,

    @NotNull(message = "Member type is required")
    MemberType memberType,

    @Size(max = 100, message = "Department must be at most 100 characters")
    String department,
    @Size(max = 30, message = "Phone must be at most 30 characters")
    String phone,
    @Size(max = 500, message = "Address must be at most 500 characters")
    String address,
    LocalDate validUntil
) {}