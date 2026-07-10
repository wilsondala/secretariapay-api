package com.secretariapay.api.service.admin;

import com.secretariapay.api.dto.admin.AdminUserRequest;
import com.secretariapay.api.dto.admin.AdminUserResponse;
import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.UserStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.UserRepository;
import com.secretariapay.api.repository.academic.InstitutionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, InstitutionRepository institutionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> findAll() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse create(AdminUserRequest request) {
        validateRequired(request, true);
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Já existe um utilizador com este e-mail.");
        }
        User user = new User()
                .setFullName(request.getFullName().trim())
                .setEmail(email)
                .setPasswordHash(passwordEncoder.encode(request.getPassword()))
                .setRole(request.getRole() == null ? UserRole.DCR_OPERADOR : request.getRole())
                .setStatus(request.getStatus() == null ? UserStatus.ACTIVE : request.getStatus())
                .setWhatsapp(trimToNull(request.getWhatsapp()))
                .setInstitution(resolveInstitution(request.getInstitutionId()));
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse update(UUID id, AdminUserRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilizador não encontrado."));
        validateRequired(request, false);
        if (request.getFullName() != null && !request.getFullName().isBlank()) user.setFullName(request.getFullName().trim());
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = normalizeEmail(request.getEmail());
            userRepository.findByEmailIgnoreCase(email).filter(found -> !found.getId().equals(id))
                    .ifPresent(found -> { throw new IllegalArgumentException("Já existe um utilizador com este e-mail."); });
            user.setEmail(email);
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 8) throw new IllegalArgumentException("A palavra-passe deve ter pelo menos 8 caracteres.");
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        if (request.getWhatsapp() != null) user.setWhatsapp(trimToNull(request.getWhatsapp()));
        if (request.getInstitutionId() != null) user.setInstitution(resolveInstitution(request.getInstitutionId()));
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse changeStatus(UUID id, UserStatus status) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilizador não encontrado."));
        user.setStatus(status);
        return AdminUserResponse.from(userRepository.save(user));
    }

    private void validateRequired(AdminUserRequest request, boolean creating) {
        if (creating && (request.getFullName() == null || request.getFullName().isBlank())) throw new IllegalArgumentException("Nome completo é obrigatório.");
        if (creating && (request.getEmail() == null || request.getEmail().isBlank())) throw new IllegalArgumentException("E-mail é obrigatório.");
        if (creating && (request.getPassword() == null || request.getPassword().length() < 8)) throw new IllegalArgumentException("A palavra-passe deve ter pelo menos 8 caracteres.");
    }

    private Institution resolveInstitution(UUID id) {
        if (id == null) return null;
        return institutionRepository.findById(id).orElseThrow(() -> new NotFoundException("Instituição não encontrada."));
    }

    private String normalizeEmail(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
