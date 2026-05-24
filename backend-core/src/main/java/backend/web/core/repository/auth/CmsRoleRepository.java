package backend.web.core.repository.auth;

import backend.web.core.model.entity.admin.CmsRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CmsRoleRepository extends JpaRepository<CmsRole, Long> {
}
