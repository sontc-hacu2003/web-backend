package backend.web.core.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.ThreadContext;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String JWT_CLAIMS_ATTRIBUTE = "jwtClaims";
    public static final String JWT_SUBJECT_ATTRIBUTE = "jwtSubject";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Base64.Decoder base64UrlDecoder = Base64.getUrlDecoder();
    private final Clock clock = Clock.systemUTC();
    private final String jwtSecret;
    private final List<String> excludedPaths;

    public JwtAuthenticationFilter(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt.secret}") String jwtSecret,
            @Value("${app.security.jwt.excluded-paths}") String excludedPaths) {
        this.objectMapper = objectMapper;
        this.jwtSecret = jwtSecret;
        this.excludedPaths = Arrays.stream(excludedPaths.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        var requestPath = request.getRequestURI();
        var contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }

        for (var excludedPath : excludedPaths) {
            if (pathMatcher.match(excludedPath, requestPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            writeUnauthorizedResponse(response, "Missing bearer token");
            return;
        }

        try {
            var token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
            var claims = validateToken(token);
            var subject = claims.get("sub");
            if (subject instanceof String userName && StringUtils.hasText(userName)) {
                request.setAttribute(JWT_SUBJECT_ATTRIBUTE, userName);
                ThreadContext.put("userName", userName);
                MDC.put("userName", userName);
            }
            request.setAttribute(JWT_CLAIMS_ATTRIBUTE, claims);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            writeUnauthorizedResponse(response, e.getMessage());
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

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        var responseBody = new HashMap<String, Object>();
        responseBody.put("status", HttpStatus.UNAUTHORIZED.value());
        responseBody.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        responseBody.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}
