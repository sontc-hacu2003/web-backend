package backend.web.core.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogContextFilter extends OncePerRequestFilter {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            putContext("ip", getClientIp(request));
            putContext("uri", request.getRequestURI());
            putContext("traceId", getTraceId(request));
            filterChain.doFilter(request, response);
        } finally {
            ThreadContext.clearAll();
            MDC.clear();
        }
    }

    private void putContext(String key, String value) {
        ThreadContext.put(key, value);
        MDC.put(key, value);
    }

    private String getTraceId(HttpServletRequest request) {
        var traceId = request.getHeader(TRACE_ID_HEADER);
        return StringUtils.hasText(traceId) ? traceId : UUID.randomUUID().toString();
    }

    private String getClientIp(HttpServletRequest request) {
        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        var realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp : request.getRemoteAddr();
    }
}
