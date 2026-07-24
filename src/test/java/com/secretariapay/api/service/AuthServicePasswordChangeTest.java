package com.secretariapay.api.service;

import com.secretariapay.api.dto.auth.ChangePasswordRequest;
import com.secretariapay.api.dto.user.UserResponse;
import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.UserStatus;
import com.secretariapay.api.repository.UserRepository;
import com.secretariapay.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServicePasswordChangeTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, jwtService);
        user = new User()
                .setFullName("Responsável IMETRO")
                .setEmail("responsavel@imetroangola.com")
                .setPasswordHash("temporary-hash")
                .setRole(UserRole.DIRECAO)
                .setStatus(UserStatus.ACTIVE)
                .setMustChangePassword(true);
    }

    @Test
    void changesTemporaryPasswordAndReleasesAccess() {
        ChangePasswordRequest request = request(
                "SenhaTemporaria#7",
                "NovaSenhaSegura#2026",
                "NovaSenhaSegura#2026"
        );

        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaTemporaria#7", "temporary-hash")).thenReturn(true);
        when(passwordEncoder.matches("NovaSenhaSegura#2026", "temporary-hash")).thenReturn(false);
        when(passwordEncoder.encode("NovaSenhaSegura#2026")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = service.changePassword(user.getEmail(), request);

        assertFalse(response.getMustChangePassword());
        assertFalse(user.getMustChangePassword());
        assertEquals("new-hash", user.getPasswordHash());
        assertNotNull(user.getPasswordChangedAt());
        verify(userRepository).save(user);
    }

    @Test
    void rejectsIncorrectCurrentPassword() {
        ChangePasswordRequest request = request(
                "SenhaErrada#7",
                "NovaSenhaSegura#2026",
                "NovaSenhaSegura#2026"
        );

        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaErrada#7", "temporary-hash")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.changePassword(user.getEmail(), request)
        );

        assertEquals("A palavra-passe atual está incorreta.", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsWeakPassword() {
        ChangePasswordRequest request = request(
                "SenhaTemporaria#7",
                "fraca12345",
                "fraca12345"
        );

        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.changePassword(user.getEmail(), request)
        );

        assertTrue(exception.getMessage().contains("maiúscula"));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    private ChangePasswordRequest request(String current, String next, String confirmation) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(current);
        request.setNewPassword(next);
        request.setConfirmPassword(confirmation);
        return request;
    }
}
