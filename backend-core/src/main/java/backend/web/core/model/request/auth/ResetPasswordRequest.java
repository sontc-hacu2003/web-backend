package backend.web.core.model.request.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    private String login;
    private String newPassword;
}
