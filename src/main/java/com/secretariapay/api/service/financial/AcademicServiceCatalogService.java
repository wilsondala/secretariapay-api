package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.AcademicServiceCatalogDto;
import com.secretariapay.api.entity.financial.AcademicServiceCatalog;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.financial.AcademicServiceCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AcademicServiceCatalogService {

    private final AcademicServiceCatalogRepository repository;

    public AcademicServiceCatalogService(AcademicServiceCatalogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AcademicServiceCatalogDto.Response> list(Boolean activeOnly, String category) {
        List<AcademicServiceCatalog> items;
        if (category != null && !category.isBlank()) {
            items = repository.findByCategoryIgnoreCaseOrderByDisplayOrderAscNameAsc(category.trim());
            if (Boolean.TRUE.equals(activeOnly)) items = items.stream().filter(AcademicServiceCatalog::isActive).toList();
        } else if (Boolean.TRUE.equals(activeOnly)) {
            items = repository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        } else {
            items = repository.findAllByOrderByDisplayOrderAscNameAsc();
        }
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AcademicServiceCatalogDto.Response findById(UUID id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço académico não encontrado.")));
    }

    @Transactional
    public AcademicServiceCatalogDto.Response create(AcademicServiceCatalogDto.Request request) {
        validate(request, true);
        if (repository.findByCodeIgnoreCase(request.code().trim()).isPresent()) {
            throw new IllegalArgumentException("Já existe um serviço com este código.");
        }
        AcademicServiceCatalog entity = new AcademicServiceCatalog();
        apply(entity, request, true);
        entity.setSourceReference("PANEL");
        return toResponse(repository.save(entity));
    }

    @Transactional
    public AcademicServiceCatalogDto.Response update(UUID id, AcademicServiceCatalogDto.Request request) {
        validate(request, false);
        AcademicServiceCatalog entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço académico não encontrado."));
        if (request.code() != null && !request.code().isBlank()) {
            repository.findByCodeIgnoreCase(request.code().trim())
                    .filter(found -> !found.getId().equals(id))
                    .ifPresent(found -> { throw new IllegalArgumentException("Já existe um serviço com este código."); });
        }
        apply(entity, request, false);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public AcademicServiceCatalogDto.Response setActive(UUID id, boolean active) {
        AcademicServiceCatalog entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço académico não encontrado."));
        entity.setActive(active);
        return toResponse(repository.save(entity));
    }

    private void validate(AcademicServiceCatalogDto.Request request, boolean creating) {
        if (request == null) throw new IllegalArgumentException("Dados do serviço são obrigatórios.");
        if (creating && (request.code() == null || request.code().isBlank())) throw new IllegalArgumentException("Código é obrigatório.");
        if (creating && (request.name() == null || request.name().isBlank())) throw new IllegalArgumentException("Nome é obrigatório.");
        if (creating && (request.category() == null || request.category().isBlank())) throw new IllegalArgumentException("Categoria é obrigatória.");
        if (request.unitPrice() != null && request.unitPrice().signum() < 0) throw new IllegalArgumentException("Preço unitário não pode ser negativo.");
    }

    private void apply(AcademicServiceCatalog entity, AcademicServiceCatalogDto.Request request, boolean creating) {
        if (request.code() != null && !request.code().isBlank()) entity.setCode(normalizeCode(request.code()));
        if (request.name() != null && !request.name().isBlank()) entity.setName(request.name().trim());
        if (request.category() != null && !request.category().isBlank()) entity.setCategory(request.category().trim().toUpperCase(Locale.ROOT));
        if (request.unitPrice() != null || creating) entity.setUnitPrice(request.unitPrice());
        if (request.currency() != null && !request.currency().isBlank()) entity.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        if (request.active() != null) entity.setActive(request.active());
        if (request.generatesGuide() != null) entity.setGeneratesGuide(request.generatesGuide());
        if (request.generatesReceipt() != null) entity.setGeneratesReceipt(request.generatesReceipt());
        if (request.allowsDiscount() != null) entity.setAllowsDiscount(request.allowsDiscount());
        if (request.allowsPenalty() != null) entity.setAllowsPenalty(request.allowsPenalty());
        if (request.availableWhatsapp() != null) entity.setAvailableWhatsapp(request.availableWhatsapp());
        if (request.availablePortal() != null) entity.setAvailablePortal(request.availablePortal());
        if (request.availablePanel() != null) entity.setAvailablePanel(request.availablePanel());
        if (request.displayOrder() != null) entity.setDisplayOrder(request.displayOrder());
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private AcademicServiceCatalogDto.Response toResponse(AcademicServiceCatalog e) {
        return new AcademicServiceCatalogDto.Response(
                e.getId(), e.getCode(), e.getName(), e.getCategory(), e.getUnitPrice(), e.getHistoricalTotal(),
                e.getCurrency(), e.isActive(), e.isGeneratesGuide(), e.isGeneratesReceipt(), e.isAllowsDiscount(),
                e.isAllowsPenalty(), e.isAvailableWhatsapp(), e.isAvailablePortal(), e.isAvailablePanel(),
                e.getDisplayOrder(), e.getSourceReference(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
