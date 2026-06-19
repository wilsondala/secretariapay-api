package com.vairapido.api.service;

import com.vairapido.api.entity.Trip;
import com.vairapido.api.entity.User;
import com.vairapido.api.entity.enums.UserRole;
import com.vairapido.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("companyAccessService")
public class CompanyAccessService {

    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public CompanyAccessService(
            UserRepository userRepository,
            EntityManager entityManager
    ) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    public boolean isAdmin() {
        User user = getCurrentUser();

        return user != null && UserRole.ADMIN.equals(user.getRole());
    }

    public boolean isOperator() {
        User user = getCurrentUser();

        return user != null && UserRole.OPERATOR.equals(user.getRole());
    }

    public boolean isCompanyAdmin() {
        User user = getCurrentUser();

        return user != null && UserRole.COMPANY_ADMIN.equals(user.getRole());
    }

    public boolean canAccessCompany(UUID companyId) {
        User user = getCurrentUser();

        if (user == null || companyId == null) {
            return false;
        }

        if (UserRole.ADMIN.equals(user.getRole())) {
            return true;
        }

        if (!UserRole.COMPANY_ADMIN.equals(user.getRole())) {
            return false;
        }

        if (user.getTransportCompany() == null) {
            return false;
        }

        return companyId.equals(user.getTransportCompany().getId());
    }

    public boolean canAccessTrip(UUID tripId) {
        User user = getCurrentUser();

        if (user == null || tripId == null) {
            return false;
        }

        if (UserRole.ADMIN.equals(user.getRole())) {
            return true;
        }

        if (!UserRole.COMPANY_ADMIN.equals(user.getRole())) {
            return false;
        }

        if (user.getTransportCompany() == null) {
            return false;
        }

        Trip trip = entityManager.find(Trip.class, tripId);

        if (trip == null || trip.getTransportCompany() == null) {
            return false;
        }

        return user.getTransportCompany()
                .getId()
                .equals(trip.getTransportCompany().getId());
    }

    public boolean canBoardTickets() {
        User user = getCurrentUser();

        if (user == null) {
            return false;
        }

        return UserRole.ADMIN.equals(user.getRole())
                || UserRole.OPERATOR.equals(user.getRole());
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElse(null);
    }
}