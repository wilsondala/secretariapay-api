package com.secretariapay.api.repository.core;
import com.secretariapay.api.entity.core.BankAccountConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BankAccountConfigRepository extends JpaRepository<BankAccountConfig, UUID> { List<BankAccountConfig> findByActiveOrderByCreatedAtDesc(Boolean active); }
