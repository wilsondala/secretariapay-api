package com.secretariapay.api.service.academic;
import com.secretariapay.api.entity.academic.AcademicRequest;
import com.secretariapay.api.entity.enums.academic.AcademicRequestStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.AcademicRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class AcademicRequestService {
    private final AcademicRequestRepository repository;
    public AcademicRequestService(AcademicRequestRepository repository){ this.repository=repository; }
    @Transactional public AcademicRequest create(AcademicRequest r){ if(r.requestCode==null||r.requestCode.isBlank()) r.requestCode=code(); if(r.status==null) r.status=AcademicRequestStatus.REQUESTED; return repository.save(r); }
    @Transactional(readOnly=true) public List<AcademicRequest> findAll(){ return repository.findAll().stream().sorted((a,b)->b.createdAt.compareTo(a.createdAt)).toList(); }
    @Transactional(readOnly=true) public List<AcademicRequest> findByStudent(UUID studentId){ return repository.findByStudentIdOrderByCreatedAtDesc(studentId); }
    @Transactional(readOnly=true) public AcademicRequest findById(UUID id){ return repository.findById(id).orElseThrow(()->new NotFoundException("Solicitação académica não encontrada.")); }
    @Transactional public AcademicRequest approve(UUID id, Map<String,String> body){ AcademicRequest r=findById(id); r.status=AcademicRequestStatus.APPROVED; r.assignedTo=body!=null?body.get("assignedTo"):null; r.reviewNote=body!=null?body.get("reviewNote"):null; r.reviewedAt=LocalDateTime.now(); return repository.save(r); }
    @Transactional public AcademicRequest reject(UUID id, Map<String,String> body){ AcademicRequest r=findById(id); r.status=AcademicRequestStatus.REJECTED; r.assignedTo=body!=null?body.get("assignedTo"):null; r.reviewNote=body!=null?body.get("reviewNote"):null; r.reviewedAt=LocalDateTime.now(); return repository.save(r); }
    @Transactional public AcademicRequest complete(UUID id, Map<String,String> body){ AcademicRequest r=findById(id); r.status=AcademicRequestStatus.COMPLETED; r.completedNote=body!=null?body.get("completedNote"):null; r.completedAt=LocalDateTime.now(); return repository.save(r); }
    private String code(){ String c; do{ c="ACD-"+System.currentTimeMillis(); }while(repository.existsByRequestCode(c)); return c; }
}
