package backend.web.core.model.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SigninRequest {
    private String login;
    private String email;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    private String password;
}
