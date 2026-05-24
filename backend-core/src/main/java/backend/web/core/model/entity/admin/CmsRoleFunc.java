package backend.web.core.model.entity.admin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cms_role_func")
public class CmsRoleFunc {

    @EmbeddedId
    private CmsRoleFuncId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private CmsRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("funcId")
    @JoinColumn(name = "func_id")
    private CmsFunction function;
}
