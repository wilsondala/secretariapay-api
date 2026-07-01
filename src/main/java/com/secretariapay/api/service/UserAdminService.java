package com.secretariapay.api.service;

import com.secretariapay.api.dto.admin.UserAdminCreateRequest;
import com.secretariapay.api.dto.admin.UserAdminResponse;
import com.secretariapay.api.dto.admin.UserAdminUpdateRoleRequest;
import com.secretariapay.api.dto.admin.UserAdminUpdateStatusRequest;
import com.secretariapay.api.dto.admin.UserAdminUpdateWhatsappRequest;
import com.secretariapay.api.entity.TransportCompany;
import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.UserStatus;
import com.secretariapay.api.repository.TransportCompanyRepository;
import com.secretariapay.api.repository.UserRepository;
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
        String whatsapp = normalizeOptionalWhatsapp(request.getWhatsapp());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um usuário cadastrado com este e-mail."
            );
        }

        validateWhatsappIsAvailable(whatsapp, null);

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
                .setWhatsapp(whatsapp)
                .setWhatsappVerified(whatsapp != null && Boolean.TRUE.equals(request.getWhatsappVerified()))
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
    public UserAdminResponse updateWhatsapp(
            UUID id,
            UserAdminUpdateWhatsappRequest request
    ) {
        User user = findUserOrThrow(id);

        String whatsapp = normalizeRequiredWhatsapp(request.getWhatsapp());

        validateWhatsappIsAvailable(whatsapp, user.getId());

        user
                .setWhatsapp(whatsapp)
                .setWhatsappVerified(Boolean.TRUE.equals(request.getWhatsappVerified()))
                .setUpdatedAt(LocalDateTime.now());

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserAdminResponse verifyWhatsapp(UUID id) {
        User user = findUserOrThrow(id);

        if (user.getWhatsapp() == null || user.getWhatsapp().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuário não possui WhatsApp cadastrado."
            );
        }

        user
                .setWhatsappVerified(true)
                .setUpdatedAt(LocalDateTime.now());

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserAdminResponse clearWhatsapp(UUID id) {
        User user = findUserOrThrow(id);

        user
                .setWhatsapp(null)
                .setWhatsappVerified(false)
                .setLastWhatsappLoginAt(null)
                .setUpdatedAt(LocalDateTime.now());

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

    private void validateWhatsappIsAvailable(
            String whatsapp,
            UUID currentUserId
    ) {
        if (whatsapp == null || whatsapp.isBlank()) {
            return;
        }

        userRepository.findByWhatsapp(whatsapp)
                .filter(existingUser -> currentUserId == null
                        || !existingUser.getId().equals(currentUserId))
                .ifPresent(existingUser -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Já existe um usuário cadastrado com este WhatsApp."
                    );
                });
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
                .setWhatsapp(user.getWhatsapp())
                .setWhatsappVerified(Boolean.TRUE.equals(user.getWhatsappVerified()))
                .setLastWhatsappLoginAt(user.getLastWhatsappLoginAt())
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

    private String normalizeOptionalWhatsapp(String whatsapp) {
        if (whatsapp == null || whatsapp.isBlank()) {
            return null;
        }

        return normalizeRequiredWhatsapp(whatsapp);
    }

    private String normalizeRequiredWhatsapp(String whatsapp) {
        if (whatsapp == null || whatsapp.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Número do WhatsApp é obrigatório."
            );
        }

        String cleaned = whatsapp.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }

        return cleaned;
    }
}
