package com.vairapido.api.service;

import com.vairapido.api.dto.transportcompany.TransportCompanyRequest;
import com.vairapido.api.dto.transportcompany.TransportCompanyResponse;
import com.vairapido.api.entity.TransportCompany;
import com.vairapido.api.entity.enums.CompanyStatus;
import com.vairapido.api.exception.NotFoundException;
import com.vairapido.api.repository.TransportCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TransportCompanyService {

    private final TransportCompanyRepository repository;

    public TransportCompanyService(TransportCompanyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TransportCompanyResponse create(TransportCompanyRequest request) {
        if (repository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new IllegalArgumentException("Já existe uma empresa cadastrada com este documento.");
        }

        TransportCompany company = new TransportCompany()
                .setName(request.getName())
                .setTradeName(request.getTradeName())
                .setDocumentNumber(request.getDocumentNumber())
                .setEmail(request.getEmail())
                .setPhone(request.getPhone())
                .setWhatsapp(request.getWhatsapp())
                .setLogoUrl(request.getLogoUrl())
                .setStatus(CompanyStatus.ACTIVE);

        TransportCompany savedCompany = repository.save(company);
        return toResponse(savedCompany);
    }

    @Transactional(readOnly = true)
    public List<TransportCompanyResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransportCompanyResponse findById(UUID id) {
        TransportCompany company = findEntityById(id);
        return toResponse(company);
    }

    @Transactional
    public TransportCompanyResponse update(UUID id, TransportCompanyRequest request) {
        TransportCompany company = findEntityById(id);

        repository.findByDocumentNumber(request.getDocumentNumber())
                .filter(existingCompany -> !existingCompany.getId().equals(id))
                .ifPresent(existingCompany -> {
                    throw new IllegalArgumentException("Já existe outra empresa cadastrada com este documento.");
                });

        company
                .setName(request.getName())
                .setTradeName(request.getTradeName())
                .setDocumentNumber(request.getDocumentNumber())
                .setEmail(request.getEmail())
                .setPhone(request.getPhone())
                .setWhatsapp(request.getWhatsapp())
                .setLogoUrl(request.getLogoUrl());

        TransportCompany updatedCompany = repository.save(company);
        return toResponse(updatedCompany);
    }

    @Transactional
    public TransportCompanyResponse activate(UUID id) {
        TransportCompany company = findEntityById(id);
        company.setStatus(CompanyStatus.ACTIVE);

        return toResponse(repository.save(company));
    }

    @Transactional
    public TransportCompanyResponse deactivate(UUID id) {
        TransportCompany company = findEntityById(id);
        company.setStatus(CompanyStatus.INACTIVE);

        return toResponse(repository.save(company));
    }

    @Transactional
    public void delete(UUID id) {
        TransportCompany company = findEntityById(id);
        repository.delete(company);
    }

    private TransportCompany findEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Empresa de transporte não encontrada."));
    }

    private TransportCompanyResponse toResponse(TransportCompany company) {
        return new TransportCompanyResponse()
                .setId(company.getId())
                .setName(company.getName())
                .setTradeName(company.getTradeName())
                .setDocumentNumber(company.getDocumentNumber())
                .setEmail(company.getEmail())
                .setPhone(company.getPhone())
                .setWhatsapp(company.getWhatsapp())
                .setLogoUrl(company.getLogoUrl())
                .setStatus(company.getStatus())
                .setCreatedAt(company.getCreatedAt())
                .setUpdatedAt(company.getUpdatedAt());
    }
}