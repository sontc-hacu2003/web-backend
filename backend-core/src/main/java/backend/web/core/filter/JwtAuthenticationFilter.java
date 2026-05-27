package backend.web.core.filter;

import backend.web.core.security.JwtAuthenticationToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper;
    private final Base64.Decoder base64UrlDecoder = Base64.getUrlDecoder();
    private final Clock clock = Clock.systemUTC();
    private final String jwtSecret;

    public JwtAuthenticationFilter(ObjectMapper objectMapper, String jwtSecret) {
        this.objectMapper = objectMapper;
        this.jwtSecret = jwtSecret;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            try {
                var token = authHeader.substring(BEARER_PREFIX.length()).trim();
                var claims = validateToken(token);
                var subject = claims.get("sub") instanceof String s ? s : null;

                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(subject, claims));

                if (StringUtils.hasText(subject)) {
                    ThreadContext.put("userName", subject);
                    MDC.put("userName", subject);
                }
            } catch (IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            ThreadContext.remove("userName");
            MDC.remove("userName");
        }
    }

    private Map<String, Object> validateToken(String token) {
        var tokenParts = token.split("\\.", -1);
        if (tokenParts.length != 3) {
            throw new IllegalArgumentException("Invalid token format");
        }

        var header = readJson(tokenParts[0]);
        var claims = readJson(tokenParts[1]);
        var algorithm = String.valueOf(header.get("alg"));
        if (!algorithm.startsWith("HS")) {
            throw new IllegalArgumentException("Unsupported token algorithm");
        }

        verifySignature(tokenParts, algorithm);
        verifyTimeClaims(claims);
        return claims;
    }

    private Map<String, Object> readJson(String encodedJson) {
        try {
            return objectMapper.readValue(base64UrlDecoder.decode(encodedJson), MAP_TYPE);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token content");
        }
    }

    private void verifySignature(String[] tokenParts, String algorithm) {
        try {
            var macAlgorithm = switch (algorithm) {
                case "HS256" -> "HmacSHA256";
                case "HS384" -> "HmacSHA384";
                case "HS512" -> "HmacSHA512";
                default -> throw new IllegalArgumentException("Unsupported token algorithm");
            };
            var mac = Mac.getInstance(macAlgorithm);
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), macAlgorithm));
            var signedContent = tokenParts[0] + "." + tokenParts[1];
            var expectedSignature = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
            var actualSignature = base64UrlDecoder.decode(tokenParts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw new IllegalArgumentException("Invalid token signature");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IllegalStateException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Token signature verification failed");
        }
    }

    private void verifyTimeClaims(Map<String, Object> claims) {
        var currentEpochSeconds = clock.instant().getEpochSecond();
        var expiration = getLongClaim(claims, "exp");
        if (expiration != null && expiration <= currentEpochSeconds) {
            throw new IllegalArgumentException("Token expired");
        }

        var notBefore = getLongClaim(claims, "nbf");
        if (notBefore != null && notBefore > currentEpochSeconds) {
            throw new IllegalArgumentException("Token is not active");
        }
    }

    private Long getLongClaim(Map<String, Object> claims, String claimName) {
        var value = claims.get(claimName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
