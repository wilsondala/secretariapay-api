package com.secretariapay.api.repository;

import com.secretariapay.api.entity.TransportCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransportCompanyRepository extends JpaRepository<TransportCompany, UUID> {

    Optional<TransportCompany> findByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumber(String documentNumber);
}
