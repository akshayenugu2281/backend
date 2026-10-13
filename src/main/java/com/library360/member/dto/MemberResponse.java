package com.library360.member.dto;

import com.library360.member.Member;
import com.library360.member.MemberStatus;
import com.library360.member.MemberType;

import java.time.LocalDate;

public record MemberResponse(
    Long id,
    Long userId,
    String email,
    String fullName,
    String memberCode,
    MemberType memberType,
    String department,
    String phone,
    String address,
    MemberStatus status,
    LocalDate validUntil
) {
    public static MemberResponse from(Member m) {
        return new MemberResponse(
            m.getId(),
            m.getUser() != null ? m.getUser().getId() : null,
            m.getUser() != null ? m.getUser().getEmail() : null,
            m.getUser() != null ? m.getUser().getFullName() : null,
            m.getMemberCode(),
            m.getMemberType(),
            m.getDepartment(),
            m.getPhone(),
            m.getAddress(),
            m.getStatus(),
            m.getValidUntil()
        );
    }
}
