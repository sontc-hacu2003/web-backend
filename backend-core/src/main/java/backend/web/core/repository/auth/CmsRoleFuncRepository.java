package backend.web.core.repository.auth;

import backend.web.core.model.entity.admin.CmsRoleFunc;
import backend.web.core.model.entity.admin.CmsRoleFuncId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CmsRoleFuncRepository extends JpaRepository<CmsRoleFunc, CmsRoleFuncId> {

    @Query("""
            select count(rf) > 0
            from CmsRoleFunc rf
            where rf.id.roleId = :roleId
            and rf.function.funcCode = :funcCode
            """)
    boolean existsByRoleIdAndFuncCode(@Param("roleId") Long roleId, @Param("funcCode") String funcCode);
}
