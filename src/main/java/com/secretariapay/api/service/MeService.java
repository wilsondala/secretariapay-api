package com.secretariapay.api.service;

import com.secretariapay.api.dto.me.MeResponse;
import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.UserStatus;
import com.secretariapay.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MeService {

    private final UserRepository userRepository;

    public MeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse getMe() {
        return toResponse(getCurrentUserOrThrow());
    }

    private User getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilizador não autenticado.");
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Utilizador autenticado não encontrado."
                ));
    }

    private MeResponse toResponse(User user) {
        UserRole role = user.getRole();
        boolean globalAdmin = UserRole.ADMIN_GLOBAL.equals(role);
        boolean institutionAdmin = UserRole.ADMIN_INSTITUTION.equals(role)
                || UserRole.ADMIN_IMETRO.equals(role);
        boolean canAccessFinancialDashboard = globalAdmin
                || institutionAdmin
                || UserRole.DIRECAO.equals(role)
                || UserRole.FINANCEIRO.equals(role)
                || UserRole.TESOURARIA.equals(role)
                || UserRole.DCR_COORDENACAO.equals(role)
                || UserRole.AUDITORIA.equals(role)
                || UserRole.TIC.equals(role);
        boolean canManageUsers = globalAdmin
                || institutionAdmin
                || UserRole.DIRECAO.equals(role)
                || UserRole.TIC.equals(role);

        MeResponse response = new MeResponse()
                .setId(user.getId())
                .setFullName(user.getFullName())
                .setEmail(user.getEmail())
                .setRole(role)
                .setStatus(user.getStatus())
                .setActive(UserStatus.ACTIVE.equals(user.getStatus()))
                .setGlobalAdmin(globalAdmin)
                .setInstitutionAdmin(institutionAdmin)
                .setCanAccessFinancialDashboard(canAccessFinancialDashboard)
                .setCanManageUsers(canManageUsers)
                .setCreatedAt(user.getCreatedAt())
                .setUpdatedAt(user.getUpdatedAt());

        if (user.getInstitution() != null) {
            response
                    .setInstitutionId(user.getInstitution().getId())
                    .setInstitutionName(user.getInstitution().getName());
        }

        return response;
    }
}
