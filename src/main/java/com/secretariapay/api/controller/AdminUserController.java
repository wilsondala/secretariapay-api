package com.secretariapay.api.controller;

import com.secretariapay.api.dto.admin.UserAdminCreateRequest;
import com.secretariapay.api.dto.admin.UserAdminResponse;
import com.secretariapay.api.dto.admin.UserAdminUpdateRoleRequest;
import com.secretariapay.api.dto.admin.UserAdminUpdateStatusRequest;
import com.secretariapay.api.dto.admin.UserAdminUpdateWhatsappRequest;
import com.secretariapay.api.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller legado mantido temporariamente para compatibilidade com os fluxos
 * administrativos antigos. A API principal de gestão de utilizadores está em
 * com.secretariapay.api.controller.admin.AdminUserController.
 *
 * O nome explícito do bean e a rota /legacy-users evitam conflito com o novo
 * controller sem remover funcionalidades antigas.
 */
@RestController("legacyAdminUserController")
@RequestMapping("/api/v1/admin/legacy-users")
@PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'ADMIN_GLOBAL', 'ROLE_ADMIN_GLOBAL')")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<UserAdminResponse> findAll() {
        return userAdminService.findAll();
    }

    @GetMapping("/{id}")
    public UserAdminResponse findById(@PathVariable UUID id) {
        return userAdminService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAdminResponse create(
            @Valid @RequestBody UserAdminCreateRequest request
    ) {
        return userAdminService.create(request);
    }

    @PatchMapping("/{id}/role")
    public UserAdminResponse updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UserAdminUpdateRoleRequest request
    ) {
        return userAdminService.updateRole(id, request);
    }

    @PatchMapping("/{id}/status")
    public UserAdminResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UserAdminUpdateStatusRequest request
    ) {
        return userAdminService.updateStatus(id, request);
    }

    @PatchMapping("/{id}/whatsapp")
    public UserAdminResponse updateWhatsapp(
            @PathVariable UUID id,
            @Valid @RequestBody UserAdminUpdateWhatsappRequest request
    ) {
        return userAdminService.updateWhatsapp(id, request);
    }

    @PatchMapping("/{id}/whatsapp/verify")
    public UserAdminResponse verifyWhatsapp(@PathVariable UUID id) {
        return userAdminService.verifyWhatsapp(id);
    }

    @DeleteMapping("/{id}/whatsapp")
    public UserAdminResponse clearWhatsapp(@PathVariable UUID id) {
        return userAdminService.clearWhatsapp(id);
    }

    @DeleteMapping("/{id}")
    public UserAdminResponse deactivate(@PathVariable UUID id) {
        return userAdminService.deactivate(id);
    }
}
