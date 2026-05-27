package backend.web.core.model.dto.admin;

import backend.web.core.model.entity.admin.CmsUser;

import java.time.LocalDateTime;

public record CmsUserDto(
        Long id,
        String username,
        String email,
        String fullName,
        Long roleId,
        String status,
        String avatarUrl,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static CmsUserDto from(CmsUser user) {
        return new CmsUserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRoleId(),
                user.getStatus(),
                user.getAvatarUrl(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
