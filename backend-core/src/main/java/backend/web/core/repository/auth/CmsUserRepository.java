package backend.web.core.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.web.core.model.entity.admin.CmsUser;

import java.util.Optional;

public interface CmsUserRepository extends JpaRepository<CmsUser, Long> {
    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    Optional<CmsUser> findByEmailIgnoreCase(String email);
}
