package com.secretariapay.api.repository.core;
import com.secretariapay.api.entity.core.PaymentMethodConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PaymentMethodConfigRepository extends JpaRepository<PaymentMethodConfig, UUID> { List<PaymentMethodConfig> findByActiveOrderByMethodNameAsc(Boolean active); }
