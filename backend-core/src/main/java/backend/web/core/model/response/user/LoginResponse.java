package backend.web.core.model.response.user;

import backend.web.core.model.dto.admin.CmsFunctionDto;
import backend.web.core.model.dto.admin.CmsUserDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {
    private CmsUserDto user;
    private String token;
    private List<CmsFunctionDto> functions;
}
