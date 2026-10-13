package com.library360.auth;

import com.library360.auth.dto.LoginRequest;
import com.library360.auth.dto.UserSummary;
import com.library360.common.BusinessException;
import com.library360.common.ErrorCode;
import com.library360.config.JwtProperties;
import com.library360.user.User;
import com.library360.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int MAX_FAILED_LOGINS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final AuthenticationManager authManager;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokenService;
    private final JwtProperties props;
    private final Clock clock;

    public record AuthResult(String accessToken, String refreshToken, UserSummary user) {}

    // Failed-attempt counters must commit even when the API intentionally returns an error.
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResult login(LoginRequest req, String ua, String ip) {
        String normalizedEmail = req.email().trim().toLowerCase(Locale.ROOT);
        User user = users.lockByEmail(normalizedEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (!"APPROVED".equalsIgnoreCase(user.getApprovalStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_PENDING);
        }

        if (user.isLocked(clock.instant())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // A lock that has already expired must not linger: clear stale counters so one
        // later failure cannot relock the account immediately.
        if (user.getLockedUntil() != null) {
            user.resetFailedLogins();
        }

        try {
            authManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, req.password())
            );

            // Optional UI role selection is verified only after credentials authenticate.
            // The selection is never used as an authorization source.
            if (req.accountType() != null && !req.accountType().isBlank()) {
                String requestedRole = req.accountType().trim().toUpperCase(Locale.ROOT);
                boolean hasRole = user.getRoles().stream().anyMatch(role -> role.getName().equals(requestedRole));
                if (!hasRole) throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }

            user.resetFailedLogins();
            return issue(user, UUID.randomUUID().toString(), ua, ip);

        } catch (AuthenticationException e) {
            user.recordFailedLogin(MAX_FAILED_LOGINS, LOCK_DURATION, clock.instant());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    // Reuse detection revokes the token family before returning an error; preserve that revocation.
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResult refresh(String rawToken, String ua, String ip) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken current = refreshTokens.lockByTokenHash(sha256(rawToken))
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (current.isRevoked()) {
            log.warn("Attempted reuse of revoked refresh token for family {}", current.getFamilyId());
            refreshTokens.revokeFamily(current.getFamilyId());
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REUSED);
        }

        if (current.getExpiresAt().isBefore(clock.instant())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!current.getUser().isEnabled()) {
            refreshTokens.revokeFamily(current.getFamilyId());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        current.setRevoked(true);
        return issue(current.getUser(), current.getFamilyId(), ua, ip);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            refreshTokens.findByTokenHash(sha256(rawToken))
                .ifPresent(t -> refreshTokens.revokeFamily(t.getFamilyId()));
        }
    }

    private AuthResult issue(User user, String familyId, String ua, String ip) {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = RefreshToken.builder()
            .user(user)
            .tokenHash(sha256(raw))
            .familyId(familyId)
            .expiresAt(clock.instant().plus(props.refreshTtl()))
            .createdAt(clock.instant())
            .userAgent(ua)
            .ipAddress(ip)
            .build();
            
        refreshTokens.save(token);

        return new AuthResult(
            tokenService.createAccessToken(user),
            raw,
            UserSummary.from(user, tokenService.getAuthorities(user))
        );
    }

    static String sha256(String v) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
