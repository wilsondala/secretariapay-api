package com.secretariapay.api.controller.restriction;
import com.secretariapay.api.entity.restriction.AcademicRestriction;
import com.secretariapay.api.service.restriction.AcademicRestrictionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/academic-restrictions")
public class AcademicRestrictionController {
    private final AcademicRestrictionService service; public AcademicRestrictionController(AcademicRestrictionService service){this.service=service;}
    @PostMapping("/apply") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','SECRETARIA','ROLE_SECRETARIA','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public AcademicRestriction apply(@RequestBody AcademicRestriction request){return service.apply(request);}
    @GetMapping @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','SECRETARIA','ROLE_SECRETARIA','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public List<AcademicRestriction> findAll(){return service.findAll();}
    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','SECRETARIA','ROLE_SECRETARIA','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public AcademicRestriction findById(@PathVariable UUID id){return service.findById(id);}
    @GetMapping("/student/{studentId}") @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','SECRETARIA','ROLE_SECRETARIA','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public List<AcademicRestriction> findByStudent(@PathVariable UUID studentId){return service.findByStudent(studentId);}
    @GetMapping("/student/{studentId}/active") @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','SECRETARIA','ROLE_SECRETARIA','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public List<AcademicRestriction> findActiveByStudent(@PathVariable UUID studentId){return service.findActiveByStudent(studentId);}
    @PatchMapping("/{id}/release") @PreAuthorize("hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','SECRETARIA','ROLE_SECRETARIA','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public AcademicRestriction release(@PathVariable UUID id,@RequestBody(required=false) Map<String,String> body){return service.release(id,body);}
}
