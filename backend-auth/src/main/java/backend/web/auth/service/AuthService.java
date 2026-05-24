package backend.web.auth.service;

import backend.web.core.helper.AuthHelper;
import backend.web.core.model.dto.admin.CmsFunctionDto;
import backend.web.core.model.entity.admin.CmsUser;
import backend.web.core.model.entity.admin.CmsFunction;

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

import backend.web.core.helper.PasswordHash;
import backend.web.core.repository.auth.CmsFunctionRepository;
import backend.web.core.repository.auth.CmsUserRepository;
import backend.web.core.model.request.auth.SigninRequest;
import backend.web.core.model.request.auth.SignupRequest;
import backend.web.core.model.response.base.BaseResponse;
import backend.web.core.model.response.user.LoginResponse;
import backend.web.core.utility.LogUtils;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final CmsUserRepository userRepository;
    private final CmsFunctionRepository functionRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration-seconds}")
    private long jwtExpirationSeconds;

    @Transactional
    public BaseResponse signup(SignupRequest request) {
        try {
            var response = validateSignupRequest(request);
            if (!response.getCode().equals(BaseResponse.Success)) return response;

            var username = request.getUsername().trim();
            var email = request.getEmail().trim().toLowerCase();

            if (userRepository.existsByUsernameIgnoreCase(username)) {
                return BaseResponse.errorResponse("Username already exists");
            }

            if (userRepository.existsByEmailIgnoreCase(email)) {
                return BaseResponse.errorResponse("Email already exists");
            }

            var now = LocalDateTime.now();
            var user = new CmsUser();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(new PasswordHash().hash(request.getPassword()));
            user.setFullName(username);
            user.setStatus("1");
            user.setIsChange(0);
            user.setUuid(UUID.randomUUID().toString());
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            var result = userRepository.save(user);
            if (result.getId() != null) {
                return BaseResponse.successResponse();
            }
            return BaseResponse.errorResponse("Failed to create user");
        }
        catch (Exception e) {
            LogUtils.error(e);
            return BaseResponse.errorResponse("Failed to create user");
        }
    }

    @Transactional
    public BaseResponse signin(SigninRequest request) {
        try {
            var response = validateSigninRequest(request);
            if (!response.getCode().equals(BaseResponse.Success)) return response;

            var email = request.getEmail().trim().toLowerCase();
            var user = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (user == null || !new PasswordHash().verify(request.getPassword(), user.getPasswordHash())) {
                return BaseResponse.errorResponse("Invalid email or password");
            }

            if (!"1".equals(user.getStatus())) {
                return BaseResponse.errorResponse("Account is inactive");
            }

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            var token = generateJwtToken(user);
            var userData = new HashMap<String, Object>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("fullName", user.getFullName());
            userData.put("roleId", user.getRoleId());

            var isAdmin = "1".equalsIgnoreCase(String.valueOf(user.getRoleId()));
            var allFunctions = isAdmin ? functionRepository.findAll() : functionRepository.findByRoleId(user.getRoleId());
            var functions = allFunctions.stream()
                    .filter(x -> Objects.equals(x.getFuncLevel(), 1L))
                    .sorted(Comparator.comparing(CmsFunction::getFuncOrder, Comparator.nullsLast(Long::compareTo)))
                    .map(x -> new CmsFunctionDto(x, AuthHelper.getChildrenFunction(x, allFunctions)))
                    .toList();

            return BaseResponse.successResponse(new LoginResponse(userData, token, functions));
        } catch (Exception e) {
            LogUtils.error(e.getMessage(), e);
            return BaseResponse.errorResponse("Login failed");
        }
    }

    private BaseResponse validateSignupRequest(SignupRequest request) {
        if (request == null) {
            return BaseResponse.errorResponse("Request body is required");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            return BaseResponse.errorResponse("Username is required");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            return BaseResponse.errorResponse("Email is required");
        }
        if (!StringUtils.hasText(request.getPassword()) || request.getPassword().length() < 8) {
            return BaseResponse.errorResponse("Password must be at least 8 characters");
        }
        return BaseResponse.successResponse();
    }

    private BaseResponse validateSigninRequest(SigninRequest request) {
        if (request == null) {
            return BaseResponse.errorResponse("Request body is required");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            return BaseResponse.errorResponse("Email is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            return BaseResponse.errorResponse("Password is required");
        }
        return BaseResponse.successResponse();
    }

    private String generateJwtToken(CmsUser user) throws Exception {
        var now = Instant.now();
        var header = Map.of("alg", "HS256", "typ", "JWT");
        var claims = Map.of(
                "sub", user.getUsername(),
                "userId", user.getId(),
                "email", user.getEmail(),
                "iat", now.getEpochSecond(),
                "exp", now.plusSeconds(jwtExpirationSeconds).getEpochSecond()
        );
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
