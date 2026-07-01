package com.secretariapay.api.service;

import com.secretariapay.api.dto.dashboard.DashboardSummaryResponse;
import com.secretariapay.api.dto.me.MeResponse;
import com.secretariapay.api.dto.me.MyCompanyResponse;
import com.secretariapay.api.entity.TransportCompany;
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
    private final DashboardService dashboardService;

    public MeService(
            UserRepository userRepository,
            DashboardService dashboardService
    ) {
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
    }

    @Transactional(readOnly = true)
    public MeResponse getMe() {
        return toMeResponse(getCurrentUserOrThrow());
    }

    @Transactional(readOnly = true)
    public MyCompanyResponse getMyCompany() {
        User user = getCurrentUserOrThrow();

        TransportCompany company = user.getTransportCompany();

        if (company == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Usuário autenticado não possui empresa vinculada."
            );
        }

        return toMyCompanyResponse(company);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getMyDashboardSummary() {
        User user = getCurrentUserOrThrow();

        if (UserRole.ADMIN.equals(user.getRole())) {
            return dashboardService.getSummary();
        }

        if (UserRole.COMPANY_ADMIN.equals(user.getRole())
                && user.getTransportCompany() != null) {
            return dashboardService.getCompanySummary(
                    user.getTransportCompany().getId()
            );
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Usuário não possui permissão para acessar dashboard."
        );
    }

    private User getCurrentUserOrThrow() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado."
            );
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário autenticado não encontrado."
                ));
    }

    private MeResponse toMeResponse(User user) {
        boolean isAdmin = UserRole.ADMIN.equals(user.getRole());
        boolean isOperator = UserRole.OPERATOR.equals(user.getRole());
        boolean isCompanyAdmin = UserRole.COMPANY_ADMIN.equals(user.getRole());
        boolean isActive = UserStatus.ACTIVE.equals(user.getStatus());

        MeResponse response = new MeResponse()
                .setId(user.getId())
                .setFullName(user.getFullName())
                .setEmail(user.getEmail())
                .setRole(user.getRole())
                .setStatus(user.getStatus())
                .setActive(isActive)
                .setAdmin(isAdmin)
                .setOperator(isOperator)
                .setCompanyAdmin(isCompanyAdmin)
                .setCanAccessGlobalDashboard(isAdmin)
                .setCanAccessCompanyDashboard(
                        isAdmin
                                || (isCompanyAdmin && user.getTransportCompany() != null)
                )
                .setCanBoardTickets(isAdmin || isOperator)
                .setCreatedAt(user.getCreatedAt())
                .setUpdatedAt(user.getUpdatedAt());

        if (user.getTransportCompany() != null) {
            response
                    .setTransportCompanyId(user.getTransportCompany().getId())
                    .setTransportCompanyName(user.getTransportCompany().getName())
                    .setTransportCompanyTradeName(user.getTransportCompany().getTradeName());
        }

        return response;
    }

    private MyCompanyResponse toMyCompanyResponse(TransportCompany company) {
        return new MyCompanyResponse()
                .setId(company.getId())
                .setName(company.getName())
                .setTradeName(company.getTradeName())
                .setDocumentNumber(company.getDocumentNumber())
                .setEmail(company.getEmail())
                .setPhone(company.getPhone())
                .setWhatsapp(company.getWhatsapp())
                .setLogoUrl(company.getLogoUrl())
                .setStatus(company.getStatus())
                .setCreatedAt(company.getCreatedAt())
                .setUpdatedAt(company.getUpdatedAt());
    }
}
