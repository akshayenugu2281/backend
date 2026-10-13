package com.library360.auth.dto;

import lombok.Builder;

@Builder
public record TokenResponse(
    String accessToken,
    long expiresIn,
    UserSummary user
) {}
