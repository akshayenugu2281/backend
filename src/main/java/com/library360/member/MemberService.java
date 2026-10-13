package com.library360.member;

import com.library360.common.BusinessException;
import com.library360.common.ErrorCode;
import com.library360.member.dto.MemberResponse;
import com.library360.member.dto.RegisterMemberRequest;
import com.library360.user.Role;
import com.library360.user.RoleRepository;
import com.library360.user.User;
import com.library360.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public MemberResponse registerMember(RegisterMemberRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String memberCode = req.memberCode().trim().toUpperCase(Locale.ROOT);
        String fullName = req.fullName().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        if (memberRepository.findByMemberCode(memberCode).isPresent()) {
            throw new BusinessException(ErrorCode.MEMBER_CODE_EXISTS);
        }

        LocalDate validUntil = req.validUntil() != null ? req.validUntil() : LocalDate.now(clock).plusYears(1);
        if (validUntil.isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.MEMBER_INACTIVE);
        }

        if (!req.password().matches("(?=.*[A-Za-z])(?=.*[0-9]).{6,72}")) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Use at least 6 characters with both letters and numbers.");
        }

        User user = new User(email, passwordEncoder.encode(req.password()), fullName);

        String roleName = req.memberType() == MemberType.FACULTY ? "FACULTY" : "STUDENT";
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role " + roleName + " not configured"));
        user.getRoles().add(role);

        User savedUser = userRepository.save(user);

        Member member = new Member();
        member.setUser(savedUser);
        member.setMemberCode(memberCode);
        member.setMemberType(req.memberType());
        member.setDepartment(req.department());
        member.setPhone(req.phone());
        member.setAddress(req.address());
        member.setStatus(MemberStatus.ACTIVE);
        member.setValidUntil(validUntil);

        Member savedMember = memberRepository.save(member);
        log.info("Registered member {} ({}) for user {}", savedMember.getMemberCode(), savedMember.getMemberType(), savedUser.getEmail());
        return MemberResponse.from(savedMember);
    }
}
