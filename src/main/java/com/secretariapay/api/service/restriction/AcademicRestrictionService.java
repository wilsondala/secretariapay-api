package com.secretariapay.api.service.restriction;
import com.secretariapay.api.entity.restriction.AcademicRestriction;
import com.secretariapay.api.entity.enums.restriction.AcademicRestrictionStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.restriction.AcademicRestrictionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class AcademicRestrictionService {
    private final AcademicRestrictionRepository repository;
    public AcademicRestrictionService(AcademicRestrictionRepository repository){ this.repository=repository; }
    @Transactional public AcademicRestriction apply(AcademicRestriction r){ if(r.restrictionCode==null||r.restrictionCode.isBlank()) r.restrictionCode=code(); if(r.status==null) r.status=AcademicRestrictionStatus.ACTIVE; if(r.appliedAt==null) r.appliedAt=LocalDateTime.now(); return repository.save(r); }
    @Transactional(readOnly=true) public List<AcademicRestriction> findAll(){ return repository.findAll().stream().sorted((a,b)->b.createdAt.compareTo(a.createdAt)).toList(); }
    @Transactional(readOnly=true) public List<AcademicRestriction> findByStudent(UUID studentId){ return repository.findByStudentIdOrderByCreatedAtDesc(studentId); }
    @Transactional(readOnly=true) public List<AcademicRestriction> findActiveByStudent(UUID studentId){ return repository.findByStudentIdAndStatusOrderByCreatedAtDesc(studentId, AcademicRestrictionStatus.ACTIVE); }
    @Transactional(readOnly=true) public AcademicRestriction findById(UUID id){ return repository.findById(id).orElseThrow(()->new NotFoundException("Restrição académica não encontrada.")); }
    @Transactional public AcademicRestriction release(UUID id, Map<String,String> body){ AcademicRestriction r=findById(id); r.status=AcademicRestrictionStatus.RELEASED; r.releasedBy=body!=null?body.get("releasedBy"):null; r.releaseNote=body!=null?body.get("releaseNote"):null; r.releasedAt=LocalDateTime.now(); return repository.save(r); }
    private String code(){ String c; do{ c="RST-"+System.currentTimeMillis(); }while(repository.existsByRestrictionCode(c)); return c; }
}
