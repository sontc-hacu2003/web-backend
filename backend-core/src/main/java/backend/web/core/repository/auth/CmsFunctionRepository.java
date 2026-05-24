package backend.web.core.repository.auth;

import backend.web.core.model.entity.admin.CmsFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CmsFunctionRepository extends JpaRepository<CmsFunction, Long> {

    @Query("""
            select rf.function
            from CmsRoleFunc rf
            where rf.id.roleId = :roleId
            order by rf.function.funcOrder asc
            """)
    List<CmsFunction> findByRoleId(@Param("roleId") Long roleId);
}
