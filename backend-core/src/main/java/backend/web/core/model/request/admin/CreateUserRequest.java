package backend.web.core.model.request.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private Long roleId;
    private String status;
}
