package com.library360.fine;

import com.library360.common.PageResponse;
import com.library360.common.BusinessException;
import com.library360.common.ErrorCode;
import com.library360.fine.dto.FineResponse;
import com.library360.fine.dto.PayFineRequest;
import com.library360.fine.dto.WaiveFineRequest;
import com.library360.member.Member;
import com.library360.member.MemberRepository;
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
@RequestMapping("/api/v1/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineRepository fineRepository;
    private final FineService fineService;
    private final MemberRepository memberRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('fine:read', 'fine:collect', 'ROLE_LIBRARIAN', 'ROLE_ADMIN')")
    public ResponseEntity<PageResponse<FineResponse>> listFines(
            @RequestParam(required = false) FineStatus status,
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Fine> fines = fineRepository.findFines(status, memberId, PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)), Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PageResponse.from(fines, FineResponse::from));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyAuthority('fine:collect', 'ROLE_LIBRARIAN', 'ROLE_ADMIN')")
    public ResponseEntity<FineResponse> payFine(
            @PathVariable Long id,
            @Valid @RequestBody PayFineRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        Long staffId = Long.valueOf(jwt.getSubject());
        FineResponse res = fineService.payFine(id, req, staffId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/waive")
    @PreAuthorize("hasAnyAuthority('fine:waive', 'ROLE_ADMIN')")
    public ResponseEntity<FineResponse> waiveFine(
            @PathVariable Long id,
            @Valid @RequestBody WaiveFineRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        Long staffId = Long.valueOf(jwt.getSubject());
        FineResponse res = fineService.waiveFine(id, req, staffId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/my-fines")
    public ResponseEntity<PageResponse<FineResponse>> getMyFines(
            @RequestParam(required = false) FineStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Page<Fine> fines = fineRepository.findFines(status, member.getId(), PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)), Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PageResponse.from(fines, FineResponse::from));
    }
}
