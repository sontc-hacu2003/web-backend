package backend.web.auth.controller;

import backend.web.auth.service.AuthService;
import backend.web.core.model.request.auth.ResetPasswordRequest;
import backend.web.core.model.request.auth.SigninRequest;
import backend.web.core.model.request.auth.SignupRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.web.core.model.response.base.BaseResponse;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "backend.cors.allowed-origins")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<BaseResponse> signup(@RequestBody SignupRequest request) {
        var response = authService.signup(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<BaseResponse> signin(@RequestBody SigninRequest request) {
        var response = authService.signin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        var response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}
