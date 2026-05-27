package backend.web.core.security;

import backend.web.core.annotation.RequireFunction;
import backend.web.core.repository.auth.CmsRoleFuncRepository;
import backend.web.core.repository.auth.CmsRoleRepository;
import backend.web.core.repository.auth.CmsUserRepository;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

public class FunctionAuthorizationManager implements AuthorizationManager<MethodInvocation> {
    private final CmsUserRepository userRepository;
    private final CmsRoleRepository roleRepository;
    private final CmsRoleFuncRepository roleFuncRepository;

    public FunctionAuthorizationManager(
            CmsUserRepository userRepository,
            CmsRoleRepository roleRepository,
            CmsRoleFuncRepository roleFuncRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.roleFuncRepository = roleFuncRepository;
    }

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authSupplier, MethodInvocation invocation) {
        var requireFunction = findAnnotation(invocation);
        if (requireFunction == null) {
            return new AuthorizationDecision(true);
        }

        var auth = authSupplier.get();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return new AuthorizationDecision(false);
        }

        var userIdObj = jwtAuth.getClaims().get("userId");
        if (!(userIdObj instanceof Number userId)) {
            return new AuthorizationDecision(false);
        }

        var user = userRepository.findById(userId.longValue()).orElse(null);
        if (user == null || user.getRoleId() == null) {
            return new AuthorizationDecision(false);
        }

        var role = roleRepository.findById(user.getRoleId()).orElse(null);
        if (role != null && Boolean.TRUE.equals(role.getIsAdmin())) {
            return new AuthorizationDecision(true);
        }

        return new AuthorizationDecision(
                roleFuncRepository.existsByRoleIdAndFuncCode(user.getRoleId(), requireFunction.value()));
    }

    private RequireFunction findAnnotation(MethodInvocation invocation) {
        var annotation = AnnotationUtils.findAnnotation(invocation.getMethod(), RequireFunction.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(invocation.getMethod().getDeclaringClass(), RequireFunction.class);
        }
        return annotation;
    }
}
