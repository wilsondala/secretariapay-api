package com.secretariapay.api.repository.financial;

import com.secretariapay.api.entity.financial.AcademicServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicServiceCatalogRepository extends JpaRepository<AcademicServiceCatalog, UUID> {
    Optional<AcademicServiceCatalog> findByCodeIgnoreCase(String code);
    List<AcademicServiceCatalog> findAllByOrderByDisplayOrderAscNameAsc();
    List<AcademicServiceCatalog> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    List<AcademicServiceCatalog> findByCategoryIgnoreCaseOrderByDisplayOrderAscNameAsc(String category);
}
