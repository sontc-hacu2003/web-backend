package backend.web.auth.controller;

import backend.web.auth.service.CmsUserService;
import backend.web.core.annotation.RequireFunction;
import backend.web.core.model.request.admin.CreateUserRequest;
import backend.web.core.model.request.admin.UpdateUserRequest;
import backend.web.core.model.response.base.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class CmsUserController {
    private final CmsUserService cmsUserService;

    @GetMapping
    @RequireFunction("ADMIN_USER_VIEW")
    public ResponseEntity<BaseResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(cmsUserService.listUsers(page, size, keyword));
    }

    @GetMapping("/{id}")
    @RequireFunction("ADMIN_USER_VIEW")
    public ResponseEntity<BaseResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(cmsUserService.getUser(id));
    }

    @PostMapping
    @RequireFunction("ADMIN_USER_CREATE")
    public ResponseEntity<BaseResponse> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(cmsUserService.createUser(request));
    }

    @PutMapping("/{id}")
    @RequireFunction("ADMIN_USER_EDIT")
    public ResponseEntity<BaseResponse> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(cmsUserService.updateUser(id, request));
    }
}
