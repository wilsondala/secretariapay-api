package com.secretariapay.api.security;

import com.secretariapay.api.entity.User;
import com.secretariapay.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MandatoryPasswordChangeFilterTest {

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksBusinessEndpointsWhilePasswordChangeIsPending() throws Exception {
        User user = new User()
                .setEmail("responsavel@imetroangola.com")
                .setMustChangePassword(true);

        authenticate(user);
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        MandatoryPasswordChangeFilter filter = new MandatoryPasswordChangeFilter(userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainCalled.set(true));

        assertEquals(428, response.getStatus());
        assertFalse(chainCalled.get());
        assertTrue(response.getContentAsString().contains("PASSWORD_CHANGE_REQUIRED"));
    }

    @Test
    void allowsThePasswordChangeEndpoint() throws Exception {
        User user = new User()
                .setEmail("responsavel@imetroangola.com")
                .setMustChangePassword(true);

        authenticate(user);

        MandatoryPasswordChangeFilter filter = new MandatoryPasswordChangeFilter(userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/auth/change-password"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainCalled.set(true));

        assertTrue(chainCalled.get());
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        List.of()
                )
        );
    }
}
