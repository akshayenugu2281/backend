package com.library360.auth.dto;

import java.util.List;

public record MeResponse(
    Long id,
    String email,
    String name,
    List<String> authorities
) {}
