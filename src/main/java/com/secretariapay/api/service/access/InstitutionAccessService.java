package com.secretariapay.api.service.access;

import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("institutionAccessService")
public class InstitutionAccessService {

    public boolean isGlobalAdmin() {
        User user = currentUser();

        if (user == null || user.getRole() == null) {
            return false;
        }

        return UserRole.ADMIN_GLOBAL.equals(user.getRole())
                || UserRole.ADMIN.equals(user.getRole());
    }

    public boolean canAccessInstitution(UUID institutionId) {
        User user = currentUser();

        if (user == null || institutionId == null) {
            return false;
        }

        if (isGlobalAdmin()) {
            return true;
        }

        return user.belongsToInstitution(institutionId);
    }

    public boolean canManageInstitution(UUID institutionId) {
        User user = currentUser();

        if (user == null || user.getRole() == null) {
            return false;
        }

        if (isGlobalAdmin()) {
            return true;
        }

        boolean institutionAdmin = UserRole.ADMIN_INSTITUTION.equals(user.getRole());
        return institutionAdmin && user.belongsToInstitution(institutionId);
    }

    public boolean canAccessFinancialModule(UUID institutionId) {
        User user = currentUser();

        if (user == null || user.getRole() == null) {
            return false;
        }

        if (isGlobalAdmin()) {
            return true;
        }

        boolean allowedRole = UserRole.ADMIN_INSTITUTION.equals(user.getRole())
                || UserRole.DIRECAO.equals(user.getRole())
                || UserRole.FINANCEIRO.equals(user.getRole())
                || UserRole.TESOURARIA.equals(user.getRole());

        return allowedRole && user.belongsToInstitution(institutionId);
    }

    public boolean canAccessAcademicModule(UUID institutionId) {
        User user = currentUser();

        if (user == null || user.getRole() == null) {
            return false;
        }

        if (isGlobalAdmin()) {
            return true;
        }

        boolean allowedRole = UserRole.ADMIN_INSTITUTION.equals(user.getRole())
                || UserRole.DIRECAO.equals(user.getRole())
                || UserRole.SECRETARIA.equals(user.getRole())
                || UserRole.OPERADOR_ATENDIMENTO.equals(user.getRole());

        return allowedRole && user.belongsToInstitution(institutionId);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }

        return user;
    }
}
