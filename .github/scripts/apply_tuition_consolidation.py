from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file_path = Path(path)
    text = file_path.read_text(encoding="utf-8")
    occurrences = text.count(old)
    if occurrences != 1:
        raise RuntimeError(f"{path}: expected exactly one occurrence, found {occurrences}")
    file_path.write_text(text.replace(old, new, 1), encoding="utf-8")


infinite_pay = "src/main/java/com/secretariapay/api/service/payment/InfinitePayTestPaymentService.java"
replace_once(
    infinite_pay,
    "import com.secretariapay.api.service.financial.ReceiptService;\nimport com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;",
    "import com.secretariapay.api.service.financial.ReceiptService;\nimport com.secretariapay.api.service.financial.TuitionChargeSettlementService;\nimport com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;",
)
replace_once(
    infinite_pay,
    "    private final ChargeRepository chargeRepository;\n    private final ReceiptService receiptService;\n    private final FinancialPenaltyCalculatorService penaltyCalculatorService;",
    "    private final ChargeRepository chargeRepository;\n    private final ReceiptService receiptService;\n    private final TuitionChargeSettlementService tuitionChargeSettlementService;\n    private final FinancialPenaltyCalculatorService penaltyCalculatorService;",
)
replace_once(
    infinite_pay,
    "            ChargeRepository chargeRepository,\n            ReceiptService receiptService,\n            FinancialPenaltyCalculatorService penaltyCalculatorService\n    ) {",
    "            ChargeRepository chargeRepository,\n            ReceiptService receiptService,\n            FinancialPenaltyCalculatorService penaltyCalculatorService,\n            TuitionChargeSettlementService tuitionChargeSettlementService\n    ) {",
)
replace_once(
    infinite_pay,
    "        this.receiptService = receiptService;\n        this.penaltyCalculatorService = penaltyCalculatorService;",
    "        this.receiptService = receiptService;\n        this.penaltyCalculatorService = penaltyCalculatorService;\n        this.tuitionChargeSettlementService = tuitionChargeSettlementService;",
)
replace_once(
    infinite_pay,
    '''        for (AcademicLine line : pending.lines()) {
            Charge charge = new Charge()
                    .setStudent(student)
                    .setChargeCode(generateChargeCode())
                    .setDescription("Teste real InfinitePay - " + line.description())
                    .setReferenceMonth(line.referenceMonth())
                    .setDueDate(line.dueDate())
                    .setAmount(line.baseAmount())
                    .setFineAmount(line.fineAmount())
                    .setInterestAmount(line.interestAmount())
                    .setDiscountAmount(BigDecimal.ZERO)
                    .setCurrency("AOA")
                    .setStatus(ChargeStatus.PAID)
                    .setPaidAt(LocalDateTime.now());

            Charge saved = chargeRepository.save(charge);
            ReceiptResponse receipt = receiptService.issueOrFindForCharge(saved.getId());
            result.add(new PersistedPayment(line, saved, receipt));
        }''',
    '''        for (AcademicLine line : pending.lines()) {
            Charge saved = tuitionChargeSettlementService.settleTuitionPayment(
                    student,
                    line.referenceMonth(),
                    line.description(),
                    line.dueDate(),
                    line.baseAmount(),
                    line.fineAmount(),
                    line.interestAmount(),
                    "AOA",
                    LocalDateTime.now()
            );
            ReceiptResponse receipt = receiptService.issueOrFindForCharge(saved.getId());
            result.add(new PersistedPayment(line, saved, receipt));
        }''',
)

monthly_generation = "src/main/java/com/secretariapay/api/service/operations/MonthlyChargeGenerationService.java"
replace_once(
    monthly_generation,
    '''                String referenceMonth = monthLabel(month) + "/" + year;
                String chargeCode = buildChargeCode(student, year, month);
                boolean exists = chargeRepository.existsByStudentIdAndReferenceMonthIgnoreCase(student.getId(), referenceMonth)
                        || chargeRepository.existsByChargeCode(chargeCode);''',
    '''                String referenceMonth = monthLabel(month) + "/" + year;
                String chargeCode = buildChargeCode(student, year, month);
                LocalDate periodStart = LocalDate.of(year, month, 1);
                LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
                boolean exists = chargeRepository.existsActiveTuitionByStudentAndPeriod(
                                student.getId(),
                                periodStart,
                                periodEnd
                        ) || chargeRepository.existsByStudentIdAndReferenceMonthIgnoreCase(student.getId(), referenceMonth)
                        || chargeRepository.existsByChargeCode(chargeCode);''',
)

tuition_generation = "src/main/java/com/secretariapay/api/service/financial/TuitionChargeGenerationService.java"
replace_once(
    tuition_generation,
    '''            String chargeCode = buildChargeCode(serviceCode, referenceMonth, student.getStudentNumber());
            Optional<Charge> existingCharge = chargeRepository.findByChargeCode(chargeCode);''',
    '''            String chargeCode = buildChargeCode(serviceCode, referenceMonth, student.getStudentNumber());
            LocalDate periodStart = request.getDueDate().withDayOfMonth(1);
            LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
            Optional<Charge> existingCharge = chargeRepository
                    .findActiveTuitionByStudentAndPeriodForUpdate(student.getId(), periodStart, periodEnd)
                    .stream()
                    .findFirst()
                    .or(() -> chargeRepository.findByChargeCode(chargeCode));''',
)

print("Tuition consolidation changes applied successfully.")
