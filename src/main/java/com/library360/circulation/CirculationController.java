package com.library360.circulation;

import com.library360.circulation.dto.IssueRequest;
import com.library360.circulation.dto.LoanResponse;
import com.library360.circulation.dto.ReturnRequest;
import com.library360.circulation.dto.ReturnResponse;
import com.library360.common.PageResponse;
import com.library360.common.BusinessException;
import com.library360.common.ErrorCode;
import com.library360.member.Member;
import com.library360.member.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/circulation")
@RequiredArgsConstructor
public class CirculationController {

    private final CirculationService circulationService;
    private final MemberRepository memberRepository;

    @PostMapping("/issue")
    @PreAuthorize("hasAuthority('loan:issue')")
    public ResponseEntity<LoanResponse> issueBook(
            @Valid @RequestBody IssueRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long staffId = Long.valueOf(jwt.getSubject());
        LoanResponse res = circulationService.issue(req, staffId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/return")
    @PreAuthorize("hasAuthority('loan:return')")
    public ResponseEntity<ReturnResponse> returnBook(
            @Valid @RequestBody ReturnRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        
        Long staffId = Long.valueOf(jwt.getSubject());
        ReturnResponse res = circulationService.returnBook(req, staffId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/renew/{id}")
    @PreAuthorize("hasAnyAuthority('loan:renew', 'ROLE_STUDENT', 'ROLE_FACULTY', 'ROLE_LIBRARIAN', 'ROLE_ADMIN')")
    public ResponseEntity<LoanResponse> renewLoan(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());
        var authorities = jwt.getClaimAsStringList("authorities");
        boolean isStaff = authorities != null && authorities.stream()
            .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_LIBRARIAN"));
        LoanResponse res = circulationService.renew(id, userId, isStaff);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/loans")
    @PreAuthorize("hasAnyAuthority('ROLE_LIBRARIAN', 'ROLE_ADMIN')")
    public ResponseEntity<PageResponse<LoanResponse>> listLoans(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<LoanResponse> res = circulationService.findLoans(
                status, memberId, PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)), Sort.by("issuedAt").descending())
        );
        return ResponseEntity.ok(res);
    }

    @GetMapping("/my-loans")
    public ResponseEntity<PageResponse<LoanResponse>> getMyLoans(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        PageResponse<LoanResponse> res = circulationService.findLoans(
                status, member.getId(), PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)), Sort.by("issuedAt").descending())
        );
        return ResponseEntity.ok(res);
    }
}
