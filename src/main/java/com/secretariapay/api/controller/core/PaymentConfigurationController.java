package com.secretariapay.api.controller.core;
import com.secretariapay.api.service.core.CoreFinanceOperationsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/payment-configurations")
public class PaymentConfigurationController { private final CoreFinanceOperationsService service; public PaymentConfigurationController(CoreFinanceOperationsService service){this.service=service;} @PostMapping("/methods") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public Map<String,Object> createMethod(@RequestBody Map<String,Object> r){return service.createMethod(r);} @GetMapping("/methods") @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','SECRETARIA','ROLE_SECRETARIA')") public List<Map<String,Object>> methods(){return service.methods();} @PostMapping("/bank-accounts") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public Map<String,Object> createBank(@RequestBody Map<String,Object> r){return service.createBank(r);} @GetMapping("/bank-accounts") @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','SECRETARIA','ROLE_SECRETARIA')") public List<Map<String,Object>> banks(){return service.banks();} }
