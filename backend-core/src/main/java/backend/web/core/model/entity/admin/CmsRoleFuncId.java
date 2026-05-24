package backend.web.core.model.entity.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class CmsRoleFuncId implements Serializable {
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "func_id")
    private Long funcId;
}
