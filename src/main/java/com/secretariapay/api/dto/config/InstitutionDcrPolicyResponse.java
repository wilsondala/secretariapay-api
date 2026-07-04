package com.secretariapay.api.dto.config;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public class InstitutionDcrPolicyResponse {

    private UUID id;
    private UUID institutionId;
    private String policyCode;
    private String policyName;
    private String currency;
    private Integer paymentWindowStartDay;
    private Integer noPenaltyUntilDay;
    private Integer firstPenaltyStartDay;
    private BigDecimal firstPenaltyPercent;
    private Integer secondPenaltyStartDay;
    private BigDecimal secondPenaltyPercent;
    private Boolean dailyInterestEnabled;
    private BigDecimal dailyInterestPercent;
    private Integer debtAfterDays;
    private Integer delinquencyAfterDays;
    private Integer preDueReminderDays;
    private Integer compulsoryReminderStartDay;
    private LocalTime whatsappAllowedStart;
    private LocalTime whatsappAllowedEnd;
    private Boolean provisionalAutomaticConfirmation;
    private Boolean manualDcrConfirmationRequired;
    private String dcrApprovalRole;
    private String officialEmail;
    private String ccEmail;
    private Boolean active;
    private String notes;

    public UUID getId() { return id; }
    public InstitutionDcrPolicyResponse setId(UUID id) { this.id = id; return this; }
    public UUID getInstitutionId() { return institutionId; }
    public InstitutionDcrPolicyResponse setInstitutionId(UUID institutionId) { this.institutionId = institutionId; return this; }
    public String getPolicyCode() { return policyCode; }
    public InstitutionDcrPolicyResponse setPolicyCode(String policyCode) { this.policyCode = policyCode; return this; }
    public String getPolicyName() { return policyName; }
    public InstitutionDcrPolicyResponse setPolicyName(String policyName) { this.policyName = policyName; return this; }
    public String getCurrency() { return currency; }
    public InstitutionDcrPolicyResponse setCurrency(String currency) { this.currency = currency; return this; }
    public Integer getPaymentWindowStartDay() { return paymentWindowStartDay; }
    public InstitutionDcrPolicyResponse setPaymentWindowStartDay(Integer paymentWindowStartDay) { this.paymentWindowStartDay = paymentWindowStartDay; return this; }
    public Integer getNoPenaltyUntilDay() { return noPenaltyUntilDay; }
    public InstitutionDcrPolicyResponse setNoPenaltyUntilDay(Integer noPenaltyUntilDay) { this.noPenaltyUntilDay = noPenaltyUntilDay; return this; }
    public Integer getFirstPenaltyStartDay() { return firstPenaltyStartDay; }
    public InstitutionDcrPolicyResponse setFirstPenaltyStartDay(Integer firstPenaltyStartDay) { this.firstPenaltyStartDay = firstPenaltyStartDay; return this; }
    public BigDecimal getFirstPenaltyPercent() { return firstPenaltyPercent; }
    public InstitutionDcrPolicyResponse setFirstPenaltyPercent(BigDecimal firstPenaltyPercent) { this.firstPenaltyPercent = firstPenaltyPercent; return this; }
    public Integer getSecondPenaltyStartDay() { return secondPenaltyStartDay; }
    public InstitutionDcrPolicyResponse setSecondPenaltyStartDay(Integer secondPenaltyStartDay) { this.secondPenaltyStartDay = secondPenaltyStartDay; return this; }
    public BigDecimal getSecondPenaltyPercent() { return secondPenaltyPercent; }
    public InstitutionDcrPolicyResponse setSecondPenaltyPercent(BigDecimal secondPenaltyPercent) { this.secondPenaltyPercent = secondPenaltyPercent; return this; }
    public Boolean getDailyInterestEnabled() { return dailyInterestEnabled; }
    public InstitutionDcrPolicyResponse setDailyInterestEnabled(Boolean dailyInterestEnabled) { this.dailyInterestEnabled = dailyInterestEnabled; return this; }
    public BigDecimal getDailyInterestPercent() { return dailyInterestPercent; }
    public InstitutionDcrPolicyResponse setDailyInterestPercent(BigDecimal dailyInterestPercent) { this.dailyInterestPercent = dailyInterestPercent; return this; }
    public Integer getDebtAfterDays() { return debtAfterDays; }
    public InstitutionDcrPolicyResponse setDebtAfterDays(Integer debtAfterDays) { this.debtAfterDays = debtAfterDays; return this; }
    public Integer getDelinquencyAfterDays() { return delinquencyAfterDays; }
    public InstitutionDcrPolicyResponse setDelinquencyAfterDays(Integer delinquencyAfterDays) { this.delinquencyAfterDays = delinquencyAfterDays; return this; }
    public Integer getPreDueReminderDays() { return preDueReminderDays; }
    public InstitutionDcrPolicyResponse setPreDueReminderDays(Integer preDueReminderDays) { this.preDueReminderDays = preDueReminderDays; return this; }
    public Integer getCompulsoryReminderStartDay() { return compulsoryReminderStartDay; }
    public InstitutionDcrPolicyResponse setCompulsoryReminderStartDay(Integer compulsoryReminderStartDay) { this.compulsoryReminderStartDay = compulsoryReminderStartDay; return this; }
    public LocalTime getWhatsappAllowedStart() { return whatsappAllowedStart; }
    public InstitutionDcrPolicyResponse setWhatsappAllowedStart(LocalTime whatsappAllowedStart) { this.whatsappAllowedStart = whatsappAllowedStart; return this; }
    public LocalTime getWhatsappAllowedEnd() { return whatsappAllowedEnd; }
    public InstitutionDcrPolicyResponse setWhatsappAllowedEnd(LocalTime whatsappAllowedEnd) { this.whatsappAllowedEnd = whatsappAllowedEnd; return this; }
    public Boolean getProvisionalAutomaticConfirmation() { return provisionalAutomaticConfirmation; }
    public InstitutionDcrPolicyResponse setProvisionalAutomaticConfirmation(Boolean provisionalAutomaticConfirmation) { this.provisionalAutomaticConfirmation = provisionalAutomaticConfirmation; return this; }
    public Boolean getManualDcrConfirmationRequired() { return manualDcrConfirmationRequired; }
    public InstitutionDcrPolicyResponse setManualDcrConfirmationRequired(Boolean manualDcrConfirmationRequired) { this.manualDcrConfirmationRequired = manualDcrConfirmationRequired; return this; }
    public String getDcrApprovalRole() { return dcrApprovalRole; }
    public InstitutionDcrPolicyResponse setDcrApprovalRole(String dcrApprovalRole) { this.dcrApprovalRole = dcrApprovalRole; return this; }
    public String getOfficialEmail() { return officialEmail; }
    public InstitutionDcrPolicyResponse setOfficialEmail(String officialEmail) { this.officialEmail = officialEmail; return this; }
    public String getCcEmail() { return ccEmail; }
    public InstitutionDcrPolicyResponse setCcEmail(String ccEmail) { this.ccEmail = ccEmail; return this; }
    public Boolean getActive() { return active; }
    public InstitutionDcrPolicyResponse setActive(Boolean active) { this.active = active; return this; }
    public String getNotes() { return notes; }
    public InstitutionDcrPolicyResponse setNotes(String notes) { this.notes = notes; return this; }
}
