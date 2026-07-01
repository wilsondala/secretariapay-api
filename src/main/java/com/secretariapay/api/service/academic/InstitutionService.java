package com.secretariapay.api.service.academic;

import com.secretariapay.api.dto.academic.InstitutionRequest;
import com.secretariapay.api.dto.academic.InstitutionResponse;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InstitutionService {

    private final InstitutionRepository repository;

    public InstitutionService(InstitutionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public InstitutionResponse create(InstitutionRequest request) {
        if (request.getNif() != null && !request.getNif().isBlank() && repository.existsByNif(request.getNif())) {
            throw new IllegalArgumentException("Já existe instituição cadastrada com este NIF.");
        }
        Institution institution = applyRequest(new Institution(), request);
        return toResponse(repository.save(institution));
    }

    @Transactional(readOnly = true)
    public List<InstitutionResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InstitutionResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public InstitutionResponse update(UUID id, InstitutionRequest request) {
        Institution institution = findEntityById(id);
        return toResponse(repository.save(applyRequest(institution, request)));
    }

    public Institution findEntityById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Instituição não encontrada."));
    }

    private Institution applyRequest(Institution institution, InstitutionRequest request) {
        institution
                .setName(request.getName())
                .setLegalName(request.getLegalName())
                .setNif(request.getNif())
                .setEmail(request.getEmail())
                .setPhone(request.getPhone())
                .setWhatsapp(request.getWhatsapp())
                .setAddress(request.getAddress());
        if (request.getActive() != null) {
            institution.setActive(request.getActive());
        }
        return institution;
    }

    private InstitutionResponse toResponse(Institution institution) {
        return new InstitutionResponse()
                .setId(institution.getId())
                .setName(institution.getName())
                .setLegalName(institution.getLegalName())
                .setNif(institution.getNif())
                .setEmail(institution.getEmail())
                .setPhone(institution.getPhone())
                .setWhatsapp(institution.getWhatsapp())
                .setAddress(institution.getAddress())
                .setActive(institution.getActive())
                .setCreatedAt(institution.getCreatedAt())
                .setUpdatedAt(institution.getUpdatedAt());
    }
}
