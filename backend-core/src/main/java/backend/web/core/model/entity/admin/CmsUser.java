package backend.web.core.model.entity.admin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "cms_user")
public class CmsUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    @Column(name = "password_hash", length = 500)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    private String status;

    @Column(name = "is_change")
    private Integer isChange;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "role_id")
    private Long roleId;
}