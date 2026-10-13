package com.library360.auth;

import com.library360.config.JwtProperties;
import com.library360.user.Permission;
import com.library360.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final Clock clock;

    public String createAccessToken(User user) {
        Instant now = clock.instant();

        List<String> authorities = getAuthorities(user);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(props.issuer())
            .subject(String.valueOf(user.getId()))
            .issuedAt(now)
            .expiresAt(now.plus(props.accessTtl()))
            .id(UUID.randomUUID().toString())
            .claim("email", user.getEmail())
            .claim("name", user.getFullName())
            .claim("authorities", authorities)
            .build();

        return encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            claims
        )).getTokenValue();
    }

    public List<String> getAuthorities(User user) {
        return Stream.concat(
                user.getRoles().stream().map(r -> "ROLE_" + r.getName()),
                user.getRoles().stream()
                    .flatMap(r -> r.getPermissions().stream())
                    .map(Permission::getCode)
            )
            .distinct()
            .toList();
    }
}
