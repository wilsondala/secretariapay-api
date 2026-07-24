package com.secretariapay.api.controller.campaign;
import com.secretariapay.api.entity.campaign.*;
import com.secretariapay.api.service.campaign.BillingCampaignService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/billing-campaigns")
public class BillingCampaignController {
    private final BillingCampaignService service; public BillingCampaignController(BillingCampaignService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','SECRETARIA','ROLE_SECRETARIA')") public BillingCampaign create(@RequestBody BillingCampaign request){return service.create(request);}
    @GetMapping @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','SECRETARIA','ROLE_SECRETARIA')") public List<BillingCampaign> findAll(){return service.findAll();}
    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','SECRETARIA','ROLE_SECRETARIA')") public BillingCampaign findById(@PathVariable UUID id){return service.findById(id);}
    @PatchMapping("/{id}/activate") @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public BillingCampaign activate(@PathVariable UUID id){return service.activate(id);}
    @PatchMapping("/{id}/complete") @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public BillingCampaign complete(@PathVariable UUID id){return service.complete(id);}
    @PostMapping("/{id}/messages") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','SECRETARIA','ROLE_SECRETARIA')") public BillingCampaignMessage addMessage(@PathVariable UUID id,@RequestBody BillingCampaignMessage request){return service.addMessage(id,request);}
    @GetMapping("/{id}/messages") @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA','SECRETARIA','ROLE_SECRETARIA')") public List<BillingCampaignMessage> findMessages(@PathVariable UUID id){return service.findMessages(id);}
}
