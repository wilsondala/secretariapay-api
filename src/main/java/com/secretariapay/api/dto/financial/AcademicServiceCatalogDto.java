package com.secretariapay.api.dto.financial;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class AcademicServiceCatalogDto {
    private AcademicServiceCatalogDto() {}

    public record Request(
            String code,
            String name,
            String category,
            BigDecimal unitPrice,
            String currency,
            Boolean active,
            Boolean generatesGuide,
            Boolean generatesReceipt,
            Boolean allowsDiscount,
            Boolean allowsPenalty,
            Boolean availableWhatsapp,
            Boolean availablePortal,
            Boolean availablePanel,
            Integer displayOrder
    ) {}

    public record Response(
            UUID id,
            String code,
            String name,
            String category,
            BigDecimal unitPrice,
            BigDecimal historicalTotal,
            String currency,
            boolean active,
            boolean generatesGuide,
            boolean generatesReceipt,
            boolean allowsDiscount,
            boolean allowsPenalty,
            boolean availableWhatsapp,
            boolean availablePortal,
            boolean availablePanel,
            int displayOrder,
            String sourceReference,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
