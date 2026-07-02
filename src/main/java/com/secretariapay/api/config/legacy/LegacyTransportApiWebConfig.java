package com.secretariapay.api.config.legacy;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LegacyTransportApiWebConfig implements WebMvcConfigurer {

    private final LegacyTransportApiInterceptor interceptor;

    public LegacyTransportApiWebConfig(LegacyTransportApiInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns(
                        "/api/v1/bookings/**",
                        "/api/v1/passengers/**",
                        "/api/v1/payments/**",
                        "/api/v1/public/tickets/**",
                        "/api/v1/tickets/**",
                        "/api/v1/ticket-audit-logs/**",
                        "/api/v1/transport-companies/**",
                        "/api/v1/routes/**",
                        "/api/v1/trips/**",
                        "/api/v1/reports/trips/**"
                )
                .excludePathPatterns(
                        "/api/v1/payment-proofs/**",
                        "/api/v1/secretariapay/**",
                        "/api/v1/public/receipts/**",
                        "/api/v1/receipts/**",
                        "/api/v1/charges/**",
                        "/api/v1/dashboard/**",
                        "/api/v1/public/branding/**",
                        "/api/v1/public/legal/**",
                        "/api/v1/public/whatsapp/webhook/status"
                );
    }
}
