package com.secretariapay.api.repository.academic;

import com.secretariapay.api.entity.academic.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {

    List<Institution> findByActiveTrueOrderByNameAsc();

    Optional<Institution> findByNif(String nif);

    boolean existsByNif(String nif);
}