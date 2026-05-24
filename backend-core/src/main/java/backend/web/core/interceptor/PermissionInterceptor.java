package backend.web.core.interceptor;

import backend.web.core.annotation.RequireFunction;
import backend.web.core.filter.JwtAuthenticationFilter;
import backend.web.core.model.response.base.BaseResponse;
import backend.web.core.repository.auth.CmsRoleFuncRepository;
import backend.web.core.repository.auth.CmsRoleRepository;
import backend.web.core.repository.auth.CmsUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {
    private final CmsUserRepository userRepository;
    private final CmsRoleRepository roleRepository;
    private final CmsRoleFuncRepository roleFuncRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        var requireFunction = handlerMethod.getMethodAnnotation(RequireFunction.class);
        if (requireFunction == null) {
            requireFunction = handlerMethod.getBeanType().getAnnotation(RequireFunction.class);
        }
        if (requireFunction == null) {
            return true;
        }

        @SuppressWarnings("unchecked")
        var claims = (Map<String, Object>) request.getAttribute(JwtAuthenticationFilter.JWT_CLAIMS_ATTRIBUTE);
        if (claims == null) {
            writeForbiddenResponse(response);
            return false;
        }

        var userIdObj = claims.get("userId");
        if (!(userIdObj instanceof Number userId)) {
            writeForbiddenResponse(response);
            return false;
        }

        var user = userRepository.findById(userId.longValue()).orElse(null);
        if (user == null || user.getRoleId() == null) {
            writeForbiddenResponse(response);
            return false;
        }

        // Admin role bypasses all permission checks
        var role = roleRepository.findById(user.getRoleId()).orElse(null);
        if (role != null && Boolean.TRUE.equals(role.getIsAdmin())) {
            return true;
        }

        if (!roleFuncRepository.existsByRoleIdAndFuncCode(user.getRoleId(), requireFunction.value())) {
            writeForbiddenResponse(response);
            return false;
        }

        return true;
    }

    private void writeForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(BaseResponse.errorResponse("Không có quyền truy cập")));
    }
}
