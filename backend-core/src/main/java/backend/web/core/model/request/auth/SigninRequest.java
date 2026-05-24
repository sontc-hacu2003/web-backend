package backend.web.core.model.request.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SigninRequest {
    private String email;
    private String password;
}
