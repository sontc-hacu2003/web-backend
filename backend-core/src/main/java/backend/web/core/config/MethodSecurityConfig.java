package backend.web.core.config;

import backend.web.core.annotation.RequireFunction;
import backend.web.core.repository.auth.CmsRoleFuncRepository;
import backend.web.core.repository.auth.CmsRoleRepository;
import backend.web.core.repository.auth.CmsUserRepository;
import backend.web.core.security.FunctionAuthorizationManager;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;

import java.lang.reflect.Method;

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class MethodSecurityConfig {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor requireFunctionAdvisor(
            @Lazy CmsUserRepository userRepository,
            @Lazy CmsRoleRepository roleRepository,
            @Lazy CmsRoleFuncRepository roleFuncRepository) {
        var authorizationManager = new FunctionAuthorizationManager(userRepository, roleRepository, roleFuncRepository);
        Pointcut pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return AnnotationUtils.findAnnotation(method, RequireFunction.class) != null
                        || AnnotationUtils.findAnnotation(targetClass, RequireFunction.class) != null;
            }
        };
        return new AuthorizationManagerBeforeMethodInterceptor(pointcut, authorizationManager);
    }
}
