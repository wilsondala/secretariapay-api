package com.secretariapay.api.controller.academic;
import com.secretariapay.api.entity.academic.AcademicRequest;
import com.secretariapay.api.service.academic.AcademicRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/academic-requests")
public class AcademicRequestController {
    private final AcademicRequestService service; public AcademicRequestController(AcademicRequestService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','SECRETARIA','ROLE_SECRETARIA','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public AcademicRequest create(@RequestBody AcademicRequest request){return service.create(request);}
    @GetMapping @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','DIRECAO','ROLE_DIRECAO','SECRETARIA','ROLE_SECRETARIA','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public List<AcademicRequest> findAll(){return service.findAll();}
    @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','DIRECAO','ROLE_DIRECAO','SECRETARIA','ROLE_SECRETARIA','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public AcademicRequest findById(@PathVariable UUID id){return service.findById(id);}
    @GetMapping("/student/{studentId}") @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','DIRECAO','ROLE_DIRECAO','SECRETARIA','ROLE_SECRETARIA','FINANCEIRO','ROLE_FINANCEIRO','TESOURARIA','ROLE_TESOURARIA')") public List<AcademicRequest> findByStudent(@PathVariable UUID studentId){return service.findByStudent(studentId);}
    @PatchMapping("/{id}/approve") @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','SECRETARIA','ROLE_SECRETARIA')") public AcademicRequest approve(@PathVariable UUID id,@RequestBody(required=false) Map<String,String> body){return service.approve(id,body);}
    @PatchMapping("/{id}/reject") @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','SECRETARIA','ROLE_SECRETARIA')") public AcademicRequest reject(@PathVariable UUID id,@RequestBody(required=false) Map<String,String> body){return service.reject(id,body);}
    @PatchMapping("/{id}/complete") @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN','SECRETARIA','ROLE_SECRETARIA')") public AcademicRequest complete(@PathVariable UUID id,@RequestBody(required=false) Map<String,String> body){return service.complete(id,body);}
}
