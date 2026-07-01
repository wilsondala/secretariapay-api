$path = "src\main\java\com\SecretariaPay\api\service\WhatsappCommandService.java"

if (-not (Test-Path $path)) {
    throw "Arquivo não encontrado: $path"
}

$content = Get-Content $path -Raw

if ($content -notmatch "WhatsappFaqAnswerService whatsappFaqAnswerService") {
    $content = $content -replace `
        "private final TicketRepository ticketRepository;", `
        "private final TicketRepository ticketRepository;`r`n    private final WhatsappFaqAnswerService whatsappFaqAnswerService;"
}

if ($content -notmatch "TicketRepository ticketRepository,\s*WhatsappFaqAnswerService whatsappFaqAnswerService") {
    $content = $content -replace `
        "TicketRepository ticketRepository\r?\n    \)", `
        "TicketRepository ticketRepository,`r`n            WhatsappFaqAnswerService whatsappFaqAnswerService`r`n    )"
}

if ($content -notmatch "this.whatsappFaqAnswerService = whatsappFaqAnswerService;") {
    $content = $content -replace `
        "this.ticketRepository = ticketRepository;", `
        "this.ticketRepository = ticketRepository;`r`n        this.whatsappFaqAnswerService = whatsappFaqAnswerService;"
}

$content = $content -replace `
    "if \(isTicketValidationCommand\(normalizedMessage\)\) \{", `
    "if (WhatsappSessionType.USER.equals(session.getSessionType())`r`n                && isTicketValidationCommand(normalizedMessage)) {"

$paymentPattern = 'private boolean isPaymentCommand\(String normalizedMessage\) \{\s*return normalizedMessage\.contains\("pagar"\)\s*\|\| normalizedMessage\.contains\("pagamento"\)\s*\|\| normalizedMessage\.contains\("paguei"\)\s*\|\| normalizedMessage\.contains\("confirmar pagamento"\);\s*\}'

$paymentReplacement = @'
private boolean isPaymentCommand(String normalizedMessage) {
        return normalizedMessage.contains("pagar reserva")
                || normalizedMessage.contains("paguei")
                || normalizedMessage.contains("confirmar pagamento")
                || normalizedMessage.matches(".*\\bpagar\\s+vr\\d{6,}.*")
                || normalizedMessage.matches(".*\\bpagamento\\s+vr\\d{6,}.*");
    }
'@

$content = [regex]::Replace($content, $paymentPattern, $paymentReplacement)

$content = $content -replace `
    "return fallback\(session\);", `
    "return fallback(session, messageText);"

if ($content -match "private WhatsappCommandResult fallback\(WhatsappSessionResponse session\) \{") {
    $content = $content -replace `
        "private WhatsappCommandResult fallback\(WhatsappSessionResponse session\) \{", `
        "private WhatsappCommandResult fallback(WhatsappSessionResponse session, String messageText) {`r`n        Optional<String> faqAnswer = whatsappFaqAnswerService.answer(`r`n                messageText,`r`n                session != null ? session.getSessionType() : null`r`n        );`r`n`r`n        if (faqAnswer.isPresent()) {`r`n            return allowed(`"FAQ`", faqAnswer.get());`r`n        }"
}

Set-Content $path $content -Encoding UTF8

Write-Host "Patch aplicado com sucesso em $path"

Select-String -Path $path -Pattern "WhatsappFaqAnswerService"
Select-String -Path $path -Pattern "return fallback\(session, messageText\)"
Select-String -Path $path -Pattern 'return allowed\("FAQ"'

