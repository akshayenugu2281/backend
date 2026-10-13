package com.library360.auth.dto;

import com.library360.user.User;
import lombok.Builder;

import java.util.List;

@Builder
public record UserSummary(
    Long id,
    String email,
    String name,
    String avatarUrl,
    List<String> authorities
) {
    public static UserSummary from(User user, List<String> authorities) {
        return UserSummary.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getFullName())
            .avatarUrl(user.getAvatarUrl())
            .authorities(authorities)
            .build();
    }
}
