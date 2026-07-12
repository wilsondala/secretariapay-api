package com.secretariapay.api.entity.financial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_service_catalog")
public class AcademicServiceCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(name = "unit_price", precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "historical_total", precision = 18, scale = 2)
    private BigDecimal historicalTotal;

    @Column(nullable = false, length = 10)
    private String currency = "AOA";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "generates_guide", nullable = false)
    private boolean generatesGuide = true;

    @Column(name = "generates_receipt", nullable = false)
    private boolean generatesReceipt = true;

    @Column(name = "allows_discount", nullable = false)
    private boolean allowsDiscount;

    @Column(name = "allows_penalty", nullable = false)
    private boolean allowsPenalty;

    @Column(name = "available_whatsapp", nullable = false)
    private boolean availableWhatsapp = true;

    @Column(name = "available_portal", nullable = false)
    private boolean availablePortal = true;

    @Column(name = "available_panel", nullable = false)
    private boolean availablePanel = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "source_reference", length = 120)
    private String sourceReference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (currency == null || currency.isBlank()) currency = "AOA";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (currency == null || currency.isBlank()) currency = "AOA";
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public AcademicServiceCatalog setCode(String code) { this.code = code; return this; }
    public String getName() { return name; }
    public AcademicServiceCatalog setName(String name) { this.name = name; return this; }
    public String getCategory() { return category; }
    public AcademicServiceCatalog setCategory(String category) { this.category = category; return this; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public AcademicServiceCatalog setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
    public BigDecimal getHistoricalTotal() { return historicalTotal; }
    public AcademicServiceCatalog setHistoricalTotal(BigDecimal historicalTotal) { this.historicalTotal = historicalTotal; return this; }
    public String getCurrency() { return currency; }
    public AcademicServiceCatalog setCurrency(String currency) { this.currency = currency; return this; }
    public boolean isActive() { return active; }
    public AcademicServiceCatalog setActive(boolean active) { this.active = active; return this; }
    public boolean isGeneratesGuide() { return generatesGuide; }
    public AcademicServiceCatalog setGeneratesGuide(boolean generatesGuide) { this.generatesGuide = generatesGuide; return this; }
    public boolean isGeneratesReceipt() { return generatesReceipt; }
    public AcademicServiceCatalog setGeneratesReceipt(boolean generatesReceipt) { this.generatesReceipt = generatesReceipt; return this; }
    public boolean isAllowsDiscount() { return allowsDiscount; }
    public AcademicServiceCatalog setAllowsDiscount(boolean allowsDiscount) { this.allowsDiscount = allowsDiscount; return this; }
    public boolean isAllowsPenalty() { return allowsPenalty; }
    public AcademicServiceCatalog setAllowsPenalty(boolean allowsPenalty) { this.allowsPenalty = allowsPenalty; return this; }
    public boolean isAvailableWhatsapp() { return availableWhatsapp; }
    public AcademicServiceCatalog setAvailableWhatsapp(boolean availableWhatsapp) { this.availableWhatsapp = availableWhatsapp; return this; }
    public boolean isAvailablePortal() { return availablePortal; }
    public AcademicServiceCatalog setAvailablePortal(boolean availablePortal) { this.availablePortal = availablePortal; return this; }
    public boolean isAvailablePanel() { return availablePanel; }
    public AcademicServiceCatalog setAvailablePanel(boolean availablePanel) { this.availablePanel = availablePanel; return this; }
    public int getDisplayOrder() { return displayOrder; }
    public AcademicServiceCatalog setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; return this; }
    public String getSourceReference() { return sourceReference; }
    public AcademicServiceCatalog setSourceReference(String sourceReference) { this.sourceReference = sourceReference; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
