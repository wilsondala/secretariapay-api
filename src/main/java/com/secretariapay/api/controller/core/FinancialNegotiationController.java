package com.secretariapay.api.controller.core;
import com.secretariapay.api.service.core.CoreFinanceOperationsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/financial-negotiations")
public class FinancialNegotiationController { private final CoreFinanceOperationsService service; public FinancialNegotiationController(CoreFinanceOperationsService service){this.service=service;} @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','SECRETARIA','ROLE_SECRETARIA')") public Map<String,Object> create(@RequestBody Map<String,Object> r){return service.createNegotiation(r);} @GetMapping @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','SECRETARIA','ROLE_SECRETARIA')") public List<Map<String,Object>> all(){return service.negotiations();} @PatchMapping("/{id}/approve") @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public Map<String,Object> approve(@PathVariable UUID id,@RequestBody(required=false) Map<String,Object> r){return service.reviewNegotiation(id,"APPROVED",r==null?Map.of():r);} @PatchMapping("/{id}/reject") @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public Map<String,Object> reject(@PathVariable UUID id,@RequestBody(required=false) Map<String,Object> r){return service.reviewNegotiation(id,"REJECTED",r==null?Map.of():r);} }
