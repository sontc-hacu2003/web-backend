package backend.web.core.filter;

import backend.web.core.model.request.base.EncryptedRequest;
import backend.web.core.service.MiddlewareCryptoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MiddlewareEncryptionFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final MiddlewareCryptoService cryptoService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final boolean enabled;
    private final List<String> excludedPaths;

    public MiddlewareEncryptionFilter(
            ObjectMapper objectMapper,
            MiddlewareCryptoService cryptoService,
            @Value("${app.middleware.encryption.enabled:true}") boolean enabled,
            @Value("${app.middleware.encryption.excluded-paths:/auth/public-key,/login,/register,/error}") String excludedPaths) {
        this.objectMapper = objectMapper;
        this.cryptoService = cryptoService;
        this.enabled = enabled;
        this.excludedPaths = Arrays.stream(excludedPaths.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || "OPTIONS".equalsIgnoreCase(request.getMethod()) || isExcludedPath(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var parsedRequest = readEncryptedRequest(request);
        if (parsedRequest.encryptedRequest() == null) {
            filterChain.doFilter(new CachedBodyHttpServletRequest(request, parsedRequest.body()), response);
            return;
        }

        var encryptedRequest = parsedRequest.encryptedRequest();
        var decryptedBody = cryptoService.decrypt(encryptedRequest);
        var decryptedRequest = new CachedBodyHttpServletRequest(request, decryptedBody);
        var wrappedResponse = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(decryptedRequest, wrappedResponse);
        writeEncryptedResponse(wrappedResponse, encryptedRequest.p());
    }

    private ParsedEncryptedRequest readEncryptedRequest(HttpServletRequest request) throws IOException {
        var body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        if (!StringUtils.hasText(body)) {
            return new ParsedEncryptedRequest(null, body);
        }

        EncryptedRequest encryptedRequest;
        try {
            encryptedRequest = objectMapper.readValue(body, EncryptedRequest.class);
        } catch (IOException e) {
            return new ParsedEncryptedRequest(null, body);
        }

        if (!StringUtils.hasText(encryptedRequest.d()) || !StringUtils.hasText(encryptedRequest.s()) || !StringUtils.hasText(encryptedRequest.p())) {
            return new ParsedEncryptedRequest(null, body);
        }

        return new ParsedEncryptedRequest(encryptedRequest, body);
    }

    private void writeEncryptedResponse(ContentCachingResponseWrapper response, String clientPublicKey) throws IOException {
        var responseBody = response.getContentAsByteArray();
        if (responseBody.length == 0) {
            response.copyBodyToResponse();
            return;
        }

        var encryptedResponse = cryptoService.encrypt(new String(responseBody, StandardCharsets.UTF_8), clientPublicKey);
        var encryptedResponseBody = objectMapper.writeValueAsString(encryptedResponse);
        var encryptedResponseBytes = encryptedResponseBody.getBytes(StandardCharsets.UTF_8);

        response.resetBuffer();
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentLength(encryptedResponseBytes.length);
        response.getOutputStream().write(encryptedResponseBytes);
        response.copyBodyToResponse();
    }

    private boolean isExcludedPath(HttpServletRequest request) {
        var requestPath = request.getRequestURI();
        var contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }

        var normalizedRequestPath = requestPath;

        return excludedPaths.stream().anyMatch((excludedPath) -> pathMatcher.match(excludedPath, normalizedRequestPath));
    }

    private record ParsedEncryptedRequest(EncryptedRequest encryptedRequest, String body) {
    }
}
