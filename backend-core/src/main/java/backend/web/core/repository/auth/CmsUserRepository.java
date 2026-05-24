package backend.web.core.repository.auth;

import backend.web.core.model.entity.admin.CmsUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CmsUserRepository extends JpaRepository<CmsUser, Long> {
    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    Optional<CmsUser> findByEmailIgnoreCase(String email);

    Optional<CmsUser> findByUsernameIgnoreCase(String username);

    @Query(
        value = """
                SELECT * FROM cms_user
                WHERE CAST(:keyword AS text) IS NULL
                OR LOWER(CAST(username AS text)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(CAST(email AS text)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                """,
        countQuery = """
                SELECT COUNT(*) FROM cms_user
                WHERE CAST(:keyword AS text) IS NULL
                OR LOWER(CAST(username AS text)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(CAST(email AS text)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                """,
        nativeQuery = true
    )
    Page<CmsUser> searchUsers(@Param("keyword") String keyword, Pageable pageable);
}
