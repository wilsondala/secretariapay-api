package com.secretariapay.api.security;

import com.secretariapay.api.entity.User;
import com.secretariapay.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class MandatoryPasswordChangeFilter extends OncePerRequestFilter {

    private static final String CHANGE_PASSWORD_PATH = "/api/v1/auth/change-password";
    private static final String CURRENT_USER_PATH = "/api/v1/auth/me";

    private final UserRepository userRepository;

    public MandatoryPasswordChangeFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return HttpMethod.OPTIONS.matches(request.getMethod())
                || CHANGE_PASSWORD_PATH.equals(path)
                || CURRENT_USER_PATH.equals(path)
                || path.startsWith("/api/v1/public/")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/error")
                || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<User> user = userRepository.findByEmailIgnoreCase(authentication.getName());

        if (user.isPresent() && Boolean.TRUE.equals(user.get().getMustChangePassword())) {
            response.setStatus(HttpStatus.PRECONDITION_REQUIRED.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"code\":\"PASSWORD_CHANGE_REQUIRED\","
                            + "\"message\":\"É obrigatório alterar a palavra-passe temporária antes de continuar.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
