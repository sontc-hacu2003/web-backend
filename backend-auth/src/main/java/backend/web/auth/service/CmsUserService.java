package backend.web.auth.service;

import backend.web.core.helper.PasswordHash;
import backend.web.core.model.entity.admin.CmsUser;
import backend.web.core.model.request.admin.CreateUserRequest;
import backend.web.core.model.request.admin.UpdateUserRequest;
import backend.web.core.model.response.base.BaseResponse;
import backend.web.core.repository.auth.CmsUserRepository;
import backend.web.core.utility.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CmsUserService {
    private final CmsUserRepository userRepository;

    private static final int MAX_PAGE_SIZE = 100;

    public BaseResponse listUsers(int page, int size, String keyword) {
        try {
            var effectiveSize = Math.min(size, MAX_PAGE_SIZE);
            var pageable = PageRequest.of(page, effectiveSize, Sort.by(Sort.Direction.DESC, "created_at"));
            var normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
            var userPage = userRepository.searchUsers(normalizedKeyword, pageable);
            var listData = userPage.getContent().stream().map(this::toUserMap).toList();
            return BaseResponse.successResponse(Map.of("listData", listData, "totalData", userPage.getTotalElements()));
        } catch (Exception e) {
            LogUtils.error(e);
            return BaseResponse.serverError();
        }
    }

    public BaseResponse getUser(Long id) {
        try {
            var user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return BaseResponse.errorResponse("Không tìm thấy người dùng");
            }
            return BaseResponse.successResponse(toUserMap(user));
        } catch (Exception e) {
            LogUtils.error(e);
            return BaseResponse.serverError();
        }
    }

    @Transactional
    public BaseResponse createUser(CreateUserRequest request) {
        try {
            var validationResult = validateCreateRequest(request);
            if (!validationResult.getCode().equals(BaseResponse.Success)) return validationResult;

            var username = request.getUsername().trim();
            var email = request.getEmail().trim().toLowerCase();

            if (userRepository.existsByUsernameIgnoreCase(username)) {
                return BaseResponse.errorResponse("Username đã tồn tại");
            }
            if (userRepository.existsByEmailIgnoreCase(email)) {
                return BaseResponse.errorResponse("Email đã tồn tại");
            }

            var now = LocalDateTime.now();
            var user = new CmsUser();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(new PasswordHash().hash(request.getPassword()));
            user.setFullName(StringUtils.hasText(request.getFullName()) ? request.getFullName().trim() : username);
            user.setRoleId(request.getRoleId());
            user.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "1");
            user.setIsChange(0);
            user.setUuid(UUID.randomUUID().toString());
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            userRepository.save(user);
            LogUtils.info("Admin created user: " + username);
            return BaseResponse.successResponse("Tạo người dùng thành công");
        } catch (Exception e) {
            LogUtils.error(e);
            return BaseResponse.serverError();
        }
    }

    @Transactional
    public BaseResponse updateUser(Long id, UpdateUserRequest request) {
        try {
            if (request == null) return BaseResponse.errorResponse("Request body là bắt buộc");
            var user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return BaseResponse.errorResponse("Không tìm thấy người dùng");
            }

            if (StringUtils.hasText(request.getFullName())) {
                user.setFullName(request.getFullName().trim());
            }
            if (request.getRoleId() != null) {
                user.setRoleId(request.getRoleId());
            }
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            LogUtils.info("Admin updated user: " + user.getUsername());
            return BaseResponse.successResponse("Cập nhật người dùng thành công");
        } catch (Exception e) {
            LogUtils.error(e);
            return BaseResponse.serverError();
        }
    }

    private BaseResponse validateCreateRequest(CreateUserRequest request) {
        if (request == null) return BaseResponse.errorResponse("Request body là bắt buộc");
        if (!StringUtils.hasText(request.getUsername())) return BaseResponse.errorResponse("Username là bắt buộc");
        if (!StringUtils.hasText(request.getEmail())) return BaseResponse.errorResponse("Email là bắt buộc");
        if (!StringUtils.hasText(request.getPassword()) || request.getPassword().length() < 8) {
            return BaseResponse.errorResponse("Mật khẩu phải có ít nhất 8 ký tự");
        }
        return BaseResponse.successResponse();
    }

    private Map<String, Object> toUserMap(CmsUser user) {
        var map = new HashMap<String, Object>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("roleId", user.getRoleId());
        map.put("status", user.getStatus());
        map.put("avatarUrl", user.getAvatarUrl());
        map.put("lastLoginAt", user.getLastLoginAt());
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", user.getUpdatedAt());
        return map;
    }
}
