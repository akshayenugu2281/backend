package com.library360.reservation;

import com.library360.common.PageResponse;
import com.library360.common.BusinessException;
import com.library360.common.ErrorCode;
import com.library360.member.Member;
import com.library360.member.MemberRepository;
import com.library360.reservation.dto.CreateReservationRequest;
import com.library360.reservation.dto.ReservationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final MemberRepository memberRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('reservation:create')")
    public ResponseEntity<ReservationResponse> reserve(
            @Valid @RequestBody CreateReservationRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());
        Member ownMember = memberRepository.findByUserId(userId).orElse(null);
        java.util.List<String> authorities = jwt.getClaimAsStringList("authorities");
        boolean isStaff = authorities != null && authorities.stream()
            .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_LIBRARIAN"));

        final Long targetMemberId;
        if (isStaff) {
            targetMemberId = req.memberId() != null ? req.memberId() : (ownMember == null ? null : ownMember.getId());
        } else {
            if (ownMember == null) {
                throw new com.library360.common.BusinessException(com.library360.common.ErrorCode.MEMBER_NOT_FOUND);
            }
            if (req.memberId() != null && !req.memberId().equals(ownMember.getId())) {
                throw new com.library360.common.BusinessException(com.library360.common.ErrorCode.ACCESS_DENIED);
            }
            targetMemberId = ownMember.getId();
        }

        ReservationResponse res = reservationService.createReservation(
            new CreateReservationRequest(req.bookId(), targetMemberId), targetMemberId);
        return ResponseEntity.ok(res);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_LIBRARIAN', 'ROLE_ADMIN')")
    public ResponseEntity<PageResponse<ReservationResponse>> listReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
        return ResponseEntity.ok(reservationService.getAllReservations(pageable));
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<ReservationResponse>> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
        return ResponseEntity.ok(reservationService.getMemberReservations(member.getId(), pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());
        Member member = memberRepository.findByUserId(userId).orElse(null);
        Long memberId = member != null ? member.getId() : null;

        boolean isStaff = jwt.getClaimAsStringList("authorities") != null &&
                jwt.getClaimAsStringList("authorities").stream().anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_LIBRARIAN"));

        reservationService.cancelReservation(id, memberId, isStaff);
        return ResponseEntity.noContent().build();
    }
}
