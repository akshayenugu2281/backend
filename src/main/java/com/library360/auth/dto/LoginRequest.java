package com.library360.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    String password,

    @jakarta.validation.constraints.Pattern(regexp = "(?i)ADMIN|LIBRARIAN|FACULTY|STUDENT", message = "Choose a valid account type")
    String accountType
) {}
