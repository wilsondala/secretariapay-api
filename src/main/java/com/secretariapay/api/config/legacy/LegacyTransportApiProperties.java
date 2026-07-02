package com.secretariapay.api.config.legacy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "secretariapay.legacy.transport-api")
public class LegacyTransportApiProperties {

    /**
     * Mantém a API legada de transporte/passagens desligada por padrão.
     * Para reativar temporariamente em ambiente controlado, configurar:
     * secretariapay.legacy.transport-api.enabled=true
     */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public LegacyTransportApiProperties setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
