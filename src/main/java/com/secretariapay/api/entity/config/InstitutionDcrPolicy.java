package com.secretariapay.api.entity.config;

import com.secretariapay.api.entity.academic.Institution;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "institution_dcr_policies")
public class InstitutionDcrPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(name = "policy_code", nullable = false, unique = true, length = 100)
    private String policyCode;

    @Column(name = "policy_name", nullable = false, length = 180)
    private String policyName;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "AOA";

    @Column(name = "payment_window_start_day", nullable = false)
    private Integer paymentWindowStartDay = 3;

    @Column(name = "no_penalty_until_day", nullable = false)
    private Integer noPenaltyUntilDay = 10;

    @Column(name = "first_penalty_start_day", nullable = false)
    private Integer firstPenaltyStartDay = 11;

    @Column(name = "first_penalty_percent", nullable = false, precision = 8, scale = 2)
    private BigDecimal firstPenaltyPercent = new BigDecimal("20.00");

    @Column(name = "second_penalty_start_day", nullable = false)
    private Integer secondPenaltyStartDay = 15;

    @Column(name = "second_penalty_percent", nullable = false, precision = 8, scale = 2)
    private BigDecimal secondPenaltyPercent = new BigDecimal("30.00");

    @Column(name = "daily_interest_enabled", nullable = false)
    private Boolean dailyInterestEnabled = false;

    @Column(name = "daily_interest_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal dailyInterestPercent = BigDecimal.ZERO;

    @Column(name = "debt_after_days", nullable = false)
    private Integer debtAfterDays = 30;

    @Column(name = "delinquency_after_days", nullable = false)
    private Integer delinquencyAfterDays = 90;

    @Column(name = "pre_due_reminder_days", nullable = false)
    private Integer preDueReminderDays = 5;

    @Column(name = "compulsory_reminder_start_day", nullable = false)
    private Integer compulsoryReminderStartDay = 22;

    @Column(name = "whatsapp_allowed_start", nullable = false)
    private LocalTime whatsappAllowedStart = LocalTime.of(7, 0);

    @Column(name = "whatsapp_allowed_end", nullable = false)
    private LocalTime whatsappAllowedEnd = LocalTime.of(19, 0);

    @Column(name = "provisional_automatic_confirmation", nullable = false)
    private Boolean provisionalAutomaticConfirmation = true;

    @Column(name = "manual_dcr_confirmation_required", nullable = false)
    private Boolean manualDcrConfirmationRequired = true;

    @Column(name = "dcr_approval_role", nullable = false, length = 80)
    private String dcrApprovalRole = "DCR_COORDENACAO";

    @Column(name = "official_email", length = 180)
    private String officialEmail;

    @Column(name = "cc_email", length = 180)
    private String ccEmail;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (currency == null || currency.isBlank()) currency = "AOA";
        if (paymentWindowStartDay == null) paymentWindowStartDay = 3;
        if (noPenaltyUntilDay == null) noPenaltyUntilDay = 10;
        if (firstPenaltyStartDay == null) firstPenaltyStartDay = 11;
        if (firstPenaltyPercent == null) firstPenaltyPercent = new BigDecimal("20.00");
        if (secondPenaltyStartDay == null) secondPenaltyStartDay = 15;
        if (secondPenaltyPercent == null) secondPenaltyPercent = new BigDecimal("30.00");
        if (dailyInterestEnabled == null) dailyInterestEnabled = false;
        if (dailyInterestPercent == null) dailyInterestPercent = BigDecimal.ZERO;
        if (debtAfterDays == null) debtAfterDays = 30;
        if (delinquencyAfterDays == null) delinquencyAfterDays = 90;
        if (preDueReminderDays == null) preDueReminderDays = 5;
        if (compulsoryReminderStartDay == null) compulsoryReminderStartDay = 22;
        if (whatsappAllowedStart == null) whatsappAllowedStart = LocalTime.of(7, 0);
        if (whatsappAllowedEnd == null) whatsappAllowedEnd = LocalTime.of(19, 0);
        if (provisionalAutomaticConfirmation == null) provisionalAutomaticConfirmation = true;
        if (manualDcrConfirmationRequired == null) manualDcrConfirmationRequired = true;
        if (dcrApprovalRole == null || dcrApprovalRole.isBlank()) dcrApprovalRole = "DCR_COORDENACAO";
        if (active == null) active = true;

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (currency == null || currency.isBlank()) currency = "AOA";
        if (dailyInterestEnabled == null) dailyInterestEnabled = false;
        if (dailyInterestPercent == null) dailyInterestPercent = BigDecimal.ZERO;
        if (provisionalAutomaticConfirmation == null) provisionalAutomaticConfirmation = true;
        if (manualDcrConfirmationRequired == null) manualDcrConfirmationRequired = true;
        if (active == null) active = true;
    }

    public UUID getId() { return id; }
    public Institution getInstitution() { return institution; }
    public InstitutionDcrPolicy setInstitution(Institution institution) { this.institution = institution; return this; }
    public String getPolicyCode() { return policyCode; }
    public InstitutionDcrPolicy setPolicyCode(String policyCode) { this.policyCode = policyCode; return this; }
    public String getPolicyName() { return policyName; }
    public InstitutionDcrPolicy setPolicyName(String policyName) { this.policyName = policyName; return this; }
    public String getCurrency() { return currency; }
    public InstitutionDcrPolicy setCurrency(String currency) { this.currency = currency; return this; }
    public Integer getPaymentWindowStartDay() { return paymentWindowStartDay; }
    public InstitutionDcrPolicy setPaymentWindowStartDay(Integer paymentWindowStartDay) { this.paymentWindowStartDay = paymentWindowStartDay; return this; }
    public Integer getNoPenaltyUntilDay() { return noPenaltyUntilDay; }
    public InstitutionDcrPolicy setNoPenaltyUntilDay(Integer noPenaltyUntilDay) { this.noPenaltyUntilDay = noPenaltyUntilDay; return this; }
    public Integer getFirstPenaltyStartDay() { return firstPenaltyStartDay; }
    public InstitutionDcrPolicy setFirstPenaltyStartDay(Integer firstPenaltyStartDay) { this.firstPenaltyStartDay = firstPenaltyStartDay; return this; }
    public BigDecimal getFirstPenaltyPercent() { return firstPenaltyPercent; }
    public InstitutionDcrPolicy setFirstPenaltyPercent(BigDecimal firstPenaltyPercent) { this.firstPenaltyPercent = firstPenaltyPercent; return this; }
    public Integer getSecondPenaltyStartDay() { return secondPenaltyStartDay; }
    public InstitutionDcrPolicy setSecondPenaltyStartDay(Integer secondPenaltyStartDay) { this.secondPenaltyStartDay = secondPenaltyStartDay; return this; }
    public BigDecimal getSecondPenaltyPercent() { return secondPenaltyPercent; }
    public InstitutionDcrPolicy setSecondPenaltyPercent(BigDecimal secondPenaltyPercent) { this.secondPenaltyPercent = secondPenaltyPercent; return this; }
    public Boolean getDailyInterestEnabled() { return dailyInterestEnabled; }
    public InstitutionDcrPolicy setDailyInterestEnabled(Boolean dailyInterestEnabled) { this.dailyInterestEnabled = dailyInterestEnabled; return this; }
    public BigDecimal getDailyInterestPercent() { return dailyInterestPercent; }
    public InstitutionDcrPolicy setDailyInterestPercent(BigDecimal dailyInterestPercent) { this.dailyInterestPercent = dailyInterestPercent; return this; }
    public Integer getDebtAfterDays() { return debtAfterDays; }
    public InstitutionDcrPolicy setDebtAfterDays(Integer debtAfterDays) { this.debtAfterDays = debtAfterDays; return this; }
    public Integer getDelinquencyAfterDays() { return delinquencyAfterDays; }
    public InstitutionDcrPolicy setDelinquencyAfterDays(Integer delinquencyAfterDays) { this.delinquencyAfterDays = delinquencyAfterDays; return this; }
    public Integer getPreDueReminderDays() { return preDueReminderDays; }
    public InstitutionDcrPolicy setPreDueReminderDays(Integer preDueReminderDays) { this.preDueReminderDays = preDueReminderDays; return this; }
    public Integer getCompulsoryReminderStartDay() { return compulsoryReminderStartDay; }
    public InstitutionDcrPolicy setCompulsoryReminderStartDay(Integer compulsoryReminderStartDay) { this.compulsoryReminderStartDay = compulsoryReminderStartDay; return this; }
    public LocalTime getWhatsappAllowedStart() { return whatsappAllowedStart; }
    public InstitutionDcrPolicy setWhatsappAllowedStart(LocalTime whatsappAllowedStart) { this.whatsappAllowedStart = whatsappAllowedStart; return this; }
    public LocalTime getWhatsappAllowedEnd() { return whatsappAllowedEnd; }
    public InstitutionDcrPolicy setWhatsappAllowedEnd(LocalTime whatsappAllowedEnd) { this.whatsappAllowedEnd = whatsappAllowedEnd; return this; }
    public Boolean getProvisionalAutomaticConfirmation() { return provisionalAutomaticConfirmation; }
    public InstitutionDcrPolicy setProvisionalAutomaticConfirmation(Boolean provisionalAutomaticConfirmation) { this.provisionalAutomaticConfirmation = provisionalAutomaticConfirmation; return this; }
    public Boolean getManualDcrConfirmationRequired() { return manualDcrConfirmationRequired; }
    public InstitutionDcrPolicy setManualDcrConfirmationRequired(Boolean manualDcrConfirmationRequired) { this.manualDcrConfirmationRequired = manualDcrConfirmationRequired; return this; }
    public String getDcrApprovalRole() { return dcrApprovalRole; }
    public InstitutionDcrPolicy setDcrApprovalRole(String dcrApprovalRole) { this.dcrApprovalRole = dcrApprovalRole; return this; }
    public String getOfficialEmail() { return officialEmail; }
    public InstitutionDcrPolicy setOfficialEmail(String officialEmail) { this.officialEmail = officialEmail; return this; }
    public String getCcEmail() { return ccEmail; }
    public InstitutionDcrPolicy setCcEmail(String ccEmail) { this.ccEmail = ccEmail; return this; }
    public Boolean getActive() { return active; }
    public InstitutionDcrPolicy setActive(Boolean active) { this.active = active; return this; }
    public String getNotes() { return notes; }
    public InstitutionDcrPolicy setNotes(String notes) { this.notes = notes; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
