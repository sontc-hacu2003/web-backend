package backend.web.core.model.request.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private String fullName;
    private Long roleId;
}
