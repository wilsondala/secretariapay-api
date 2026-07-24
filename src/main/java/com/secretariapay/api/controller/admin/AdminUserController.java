package com.secretariapay.api.controller.admin;

import com.secretariapay.api.dto.admin.AdminUserRequest;
import com.secretariapay.api.dto.admin.AdminUserResponse;
import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.UserStatus;
import com.secretariapay.api.service.admin.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','TIC','ROLE_TIC')")
public class AdminUserController {
    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminUserResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/roles")
    public List<String> roles() {
        return Arrays.stream(UserRole.values()).map(Enum::name).toList();
    }

    @GetMapping("/statuses")
    public List<String> statuses() {
        return Arrays.stream(UserStatus.values()).map(Enum::name).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse create(@RequestBody AdminUserRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public AdminUserResponse update(@PathVariable UUID id, @RequestBody AdminUserRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public AdminUserResponse changeStatus(@PathVariable UUID id, @RequestParam UserStatus status) {
        return service.changeStatus(id, status);
    }
}
