package com.vairapido.api.service;

import com.vairapido.api.dto.admin.UserAdminCreateRequest;
import com.vairapido.api.dto.admin.UserAdminResponse;
import com.vairapido.api.dto.admin.UserAdminUpdateRoleRequest;
import com.vairapido.api.dto.admin.UserAdminUpdateStatusRequest;
import com.vairapido.api.entity.TransportCompany;
import com.vairapido.api.entity.User;
import com.vairapido.api.entity.enums.UserRole;
import com.vairapido.api.entity.enums.UserStatus;
import com.vairapido.api.repository.TransportCompanyRepository;
import com.vairapido.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final TransportCompanyRepository transportCompanyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(
            UserRepository userRepository,
            TransportCompanyRepository transportCompanyRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.transportCompanyRepository = transportCompanyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(
                        User::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserAdminResponse findById(UUID id) {
        return toResponse(findUserOrThrow(id));
    }

    @Transactional
    public UserAdminResponse create(UserAdminCreateRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um usuário cadastrado com este e-mail."
            );
        }

        TransportCompany transportCompany =
                resolveTransportCompanyForRole(
                        request.getRole(),
                        request.getTransportCompanyId()
                );

        UserStatus status = Boolean.FALSE.equals(request.getActive())
                ? UserStatus.INACTIVE
                : UserStatus.ACTIVE;

        User user = new User()
                .setFullName(request.getFullName().trim())
                .setEmail(email)
                .setPasswordHash(passwordEncoder.encode(request.getPassword()))
                .setRole(request.getRole())
                .setStatus(status)
                .setTransportCompany(transportCompany)
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserAdminResponse updateRole(
            UUID id,
            UserAdminUpdateRoleRequest request
    ) {
        User user = findUserOrThrow(id);

        user.setRole(request.getRole());

        if (UserRole.COMPANY_ADMIN.equals(request.getRole())
                && user.getTransportCompany() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuário COMPANY_ADMIN precisa estar vinculado a uma empresa."
            );
        }

        if (!UserRole.COMPANY_ADMIN.equals(request.getRole())) {
            user.setTransportCompany(null);
        }

        user.setUpdatedAt(LocalDateTime.now());

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserAdminResponse updateStatus(
            UUID id,
            UserAdminUpdateStatusRequest request
    ) {
        User user = findUserOrThrow(id);

        UserStatus status = Boolean.TRUE.equals(request.getActive())
                ? UserStatus.ACTIVE
                : UserStatus.INACTIVE;

        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserAdminResponse deactivate(UUID id) {
        User user = findUserOrThrow(id);

        user.setStatus(UserStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());

        return toResponse(userRepository.save(user));
    }

    private TransportCompany resolveTransportCompanyForRole(
            UserRole role,
            UUID transportCompanyId
    ) {
        if (!UserRole.COMPANY_ADMIN.equals(role)) {
            return null;
        }

        if (transportCompanyId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuário COMPANY_ADMIN precisa de uma empresa vinculada."
            );
        }

        return transportCompanyRepository.findById(transportCompanyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Empresa de transporte não encontrada."
                ));
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."
                ));
    }

    private UserAdminResponse toResponse(User user) {
        UserAdminResponse response = new UserAdminResponse()
                .setId(user.getId())
                .setFullName(user.getFullName())
                .setEmail(user.getEmail())
                .setRole(user.getRole())
                .setActive(UserStatus.ACTIVE.equals(user.getStatus()))
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

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase();
    }
}