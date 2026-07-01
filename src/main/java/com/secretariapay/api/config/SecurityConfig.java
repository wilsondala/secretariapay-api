package com.secretariapay.api.config;

import com.secretariapay.api.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final UserDetailsService userDetailsService;

        public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        UserDetailsService userDetailsService) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.userDetailsService = userDetailsService;
        }

        @Bean
        public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
                        JwtAuthenticationFilter filter) {
                FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);

                registration.setEnabled(false);

                return registration;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

                provider.setUserDetailsService(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder());

                return provider;
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOriginPatterns(List.of(
                                "http://localhost:*",
                                "http://127.0.0.1:*",
                                "https://*.vercel.app",
                                "https://vairapido.triacompany.com",
                                "https://painel-vairapido.triacompany.com",
                                "https://www.vairapido.triacompany.com"));

                configuration.setAllowedMethods(List.of(
                                "GET",
                                "POST",
                                "PUT",
                                "PATCH",
                                "DELETE",
                                "OPTIONS"));

                configuration.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "Origin",
                                "X-Requested-With"));

                configuration.setExposedHeaders(List.of(
                                "Authorization"));

                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }

        @Bean
        @Order(1)
        public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/v1/public/**")
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .anyRequest().permitAll());

                return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                .requestMatchers(HttpMethod.GET, "/").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                                                .requestMatchers("/error").permitAll()
                                                .requestMatchers("/actuator/**").permitAll()
                                                
                                                .requestMatchers("/api/v1/public/**").permitAll()
                                                .requestMatchers("/api/v1/multi-country/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                                                .requestMatchers("/api/v1/admin/**")
                                                .hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                                                .requestMatchers(HttpMethod.GET, "/api/v1/tickets/*/boarding-preview")
                                                .hasAnyAuthority(
                                                                "ADMIN",
                                                                "ROLE_ADMIN",
                                                                "COMPANY_ADMIN",
                                                                "ROLE_COMPANY_ADMIN",
                                                                "OPERATOR",
                                                                "ROLE_OPERATOR")

                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/tickets/*/board")
                                                .hasAnyAuthority(
                                                                "ADMIN",
                                                                "ROLE_ADMIN",
                                                                "COMPANY_ADMIN",
                                                                "ROLE_COMPANY_ADMIN",
                                                                "OPERATOR",
                                                                "ROLE_OPERATOR")

                                                .requestMatchers("/api/v1/reports/**")
                                                .hasAnyAuthority(
                                                                "ADMIN",
                                                                "ROLE_ADMIN",
                                                                "COMPANY_ADMIN",
                                                                "ROLE_COMPANY_ADMIN")

                                                .anyRequest().authenticated())
                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);
                                                

                return http.build();
        }
}
