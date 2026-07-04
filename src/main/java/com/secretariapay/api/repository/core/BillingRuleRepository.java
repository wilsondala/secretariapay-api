package com.secretariapay.api.repository.core;
import com.secretariapay.api.entity.core.BillingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BillingRuleRepository extends JpaRepository<BillingRule, UUID> { List<BillingRule> findByActiveOrderByRuleNameAsc(Boolean active); }
