package com.secretariapay.api.controller.core;
import com.secretariapay.api.service.core.CoreFinanceOperationsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/billing-rules")
public class BillingRuleController { private final CoreFinanceOperationsService service; public BillingRuleController(CoreFinanceOperationsService service){this.service=service;} @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public Map<String,Object> create(@RequestBody Map<String,Object> r){return service.createRule(r);} @GetMapping @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','SECRETARIA','ROLE_SECRETARIA')") public List<Map<String,Object>> all(){return service.rules();} }
