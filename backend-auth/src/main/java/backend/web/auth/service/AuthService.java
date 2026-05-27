package backend.web.auth.service;

import backend.web.core.helper.AuthHelper;
import backend.web.core.helper.PasswordHash;
import backend.web.core.model.dto.admin.CmsFunctionDto;
import backend.web.core.model.dto.admin.CmsUserDto;
import backend.web.core.model.entity.admin.CmsFunction;
import backend.web.core.model.entity.admin.CmsUser;
import backend.web.core.model.request.auth.ResetPasswordRequest;
import backend.web.core.model.request.auth.SigninRequest;
import backend.web.core.model.request.auth.SignupRequest;
import backend.web.core.model.response.base.BaseResponse;
import backend.web.core.model.response.user.LoginResponse;
import backend.web.core.repository.auth.CmsFunctionRepository;
import backend.web.core.repository.auth.CmsRoleRepository;
import backend.web.core.repository.auth.CmsUserRepository;
import backend.web.core.utility.LogUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final CmsUserRepository userRepository;
    private final CmsRoleRepository roleRepository;
    private final CmsFunctionRepository functionRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration-seconds}")
    private long jwtExpirationSeconds;

    @Transactional
    public BaseResponse signup(SignupRequest request) {
        try {
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
            user.setPasswordHash(PasswordHash.hash(request.getPassword()));
            user.setFullName(username);
            user.setStatus("1");
            user.setIsChange(0);
            user.setUuid(UUID.randomUUID().toString());
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            userRepository.save(user);
            LogUtils.info("User registered: " + username);
            return BaseResponse.successResponse();
        } catch (Exception e) {
            LogUtils.error("Signup failed", e);
            return BaseResponse.serverError();
        }
    }

    @Transactional
    public BaseResponse signin(SigninRequest request) {
        try {
            var login = resolveLogin(request);
            if (!StringUtils.hasText(login)) {
                return BaseResponse.errorResponse("Email hoặc username là bắt buộc");
            }

            var user = findUserByLogin(login);
            if (user == null || !PasswordHash.verify(request.getPassword(), user.getPasswordHash())) {
                return BaseResponse.errorResponse("Sai username/email hoặc mật khẩu");
            }

            if (!"1".equals(user.getStatus())) {
                return BaseResponse.errorResponse("Tài khoản đã bị khoá");
            }

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            var token = generateJwtToken(user);
            var isAdmin = isAdminRole(user.getRoleId());
            var allFunctions = isAdmin ? functionRepository.findAll()
                    : functionRepository.findByRoleId(user.getRoleId());
            var functions = allFunctions.stream()
                    .filter(x -> Objects.equals(x.getFuncLevel(), 1L))
                    .sorted(Comparator.comparing(CmsFunction::getFuncOrder, Comparator.nullsLast(Long::compareTo)))
                    .map(x -> new CmsFunctionDto(x, AuthHelper.getChildrenFunction(x, allFunctions)))
                    .toList();

            LogUtils.info("User logged in: " + user.getUsername());
            return BaseResponse.successResponse(new LoginResponse(CmsUserDto.from(user), token, functions));
        } catch (Exception e) {
            LogUtils.error("Signin failed", e);
            return BaseResponse.serverError();
        }
    }

    @Transactional
    public BaseResponse resetPassword(ResetPasswordRequest request) {
        try {
            var user = findUserByLogin(request.getLogin());
            if (user == null || !PasswordHash.verify(request.getOldPassword(), user.getPasswordHash())) {
                return BaseResponse.errorResponse("Sai thông tin đăng nhập hoặc mật khẩu cũ");
            }

            user.setPasswordHash(PasswordHash.hash(request.getNewPassword()));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            LogUtils.info("Password reset for: " + user.getUsername());
            return BaseResponse.successResponse("Đổi mật khẩu thành công");
        } catch (Exception e) {
            LogUtils.error("Reset password failed", e);
            return BaseResponse.serverError();
        }
    }

    private boolean isAdminRole(Long roleId) {
        if (roleId == null)
            return false;
        return roleRepository.findById(roleId)
                .map(role -> Boolean.TRUE.equals(role.getIsAdmin()))
                .orElse(false);
    }

    private String resolveLogin(SigninRequest request) {
        if (StringUtils.hasText(request.getLogin())) {
            return request.getLogin().trim();
        }
        return StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null;
    }

    private CmsUser findUserByLogin(String login) {
        var normalizedLogin = login.trim().toLowerCase();

        if (normalizedLogin.contains("@")) {
            return userRepository.findByEmailIgnoreCase(normalizedLogin).orElse(null);
        }
        return userRepository.findByUsernameIgnoreCase(normalizedLogin).orElse(null);
    }

    private String generateJwtToken(CmsUser user) throws Exception {
        var now = Instant.now();
        var header = Map.of("alg", "HS256", "typ", "JWT");
        var claims = Map.of(
                "sub", user.getUsername(),
                "userId", user.getId(),
                "email", user.getEmail(),
                "iat", now.getEpochSecond(),
                "exp", now.plusSeconds(jwtExpirationSeconds).getEpochSecond());
        var encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));
        var encodedClaims = base64UrlEncode(objectMapper.writeValueAsBytes(claims));
        var signedContent = encodedHeader + "." + encodedClaims;
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        return signedContent + "." + base64UrlEncode(mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
