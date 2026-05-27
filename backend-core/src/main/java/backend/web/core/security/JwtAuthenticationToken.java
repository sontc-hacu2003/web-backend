package backend.web.core.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;
import java.util.Map;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private final String subject;
    private final Map<String, Object> claims;

    public JwtAuthenticationToken(String subject, Map<String, Object> claims) {
        super(Collections.emptyList());
        this.subject = subject;
        this.claims = Map.copyOf(claims);
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return subject;
    }

    public String getSubject() {
        return subject;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }
}
