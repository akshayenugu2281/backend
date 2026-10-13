package com.library360.member;

import com.library360.common.BusinessException;
import com.library360.common.ErrorCode;
import com.library360.common.PageResponse;
import com.library360.member.dto.MemberResponse;
import com.library360.member.dto.RegisterMemberRequest;
import com.library360.reservation.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final ReservationService reservationService;
    private final com.library360.auth.RefreshTokenRepository refreshTokens;

    @GetMapping
    @PreAuthorize("hasAuthority('member:read')")
    public ResponseEntity<PageResponse<MemberResponse>> listMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Member> members = memberRepository.findAllMembers(PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)), Sort.by("memberCode")));
        return ResponseEntity.ok(PageResponse.from(members, MemberResponse::from));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('member:write')")
    public ResponseEntity<MemberResponse> registerMember(@Valid @RequestBody RegisterMemberRequest req) {
        MemberResponse res = memberService.registerMember(req);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('member:read')")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    @GetMapping("/by-code/{code}")
    @PreAuthorize("hasAuthority('member:read')")
    public ResponseEntity<MemberResponse> getMemberByCode(@PathVariable String code) {
        Member member = memberRepository.findByMemberCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getCurrentMember(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return ResponseEntity.ok(MemberResponse.from(member));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('member:write')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> suspendMember(@PathVariable Long id) {
        Member member = memberRepository.lockById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.setStatus(MemberStatus.SUSPENDED);
        reservationService.cancelAllForMember(member.getId());
        // A suspended borrower's sessions must die immediately — refresh rotation
        // only checks the user flag, so revoke explicitly here.
        if (member.getUser() != null) {
            refreshTokens.revokeAllForUser(member.getUser().getId());
        }
        return ResponseEntity.noContent().build();
    }

}
