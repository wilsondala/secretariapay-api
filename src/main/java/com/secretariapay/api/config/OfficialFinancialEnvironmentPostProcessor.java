package com.secretariapay.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

public class OfficialFinancialEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "secretariapayOfficialFinancialDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("secretariapay.demo.email", "dcr_pay@imetroangola.com");
        defaults.put("secretariapay.payments.infinitepay-test-enabled", false);
        defaults.put("secretariapay.notifications.email-from", "dcr_pay@imetroangola.com");
        defaults.put("secretariapay.notifications.email-cc", "df.oi_pay@imetroangola.com");
        defaults.put("secretariapay.notifications.email-sender-name", "SecretáriaPay Académico — IMETRO");

        if (!environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
        }
    }
}
