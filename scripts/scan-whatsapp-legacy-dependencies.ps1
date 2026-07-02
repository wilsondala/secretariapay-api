# SecretáriaPay Académico - Fase 7
# Varredura de dependências dos serviços WhatsApp legados.
# Objetivo: descobrir quem ainda injeta ou chama WhatsappCommandService e WhatsappFaqAnswerService
# antes de qualquer remoção ou migração de pacote.

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "== SecretáriaPay | Fase 7 - Dependências do WhatsApp legado ==" -ForegroundColor Cyan
Write-Host "Projeto: $root"
Write-Host ""

$patterns = @(
    "WhatsappCommandService",
    "WhatsappFaqAnswerService",
    "new WhatsappCommandService",
    "new WhatsappFaqAnswerService",
    "processCommand",
    "answer\(",
    "WhatsappWebhookService",
    "WhatsappSessionService",
    "WhatsappCommandResult",
    "WhatsappSessionResponse"
)

$paths = @(
    ".\src\main\java\com\secretariapay\api\**\*.java"
)

foreach ($pattern in $patterns) {
    Write-Host ""
    Write-Host "--- Procurando: $pattern ---" -ForegroundColor Yellow
    Select-String -Path $paths -Pattern $pattern -CaseSensitive:$false | ForEach-Object {
        "{0}:{1}: {2}" -f $_.Path.Replace($root + "\", ""), $_.LineNumber, $_.Line.Trim()
    }
}

Write-Host ""
Write-Host "== Sugestão de classificação ==" -ForegroundColor Cyan
Write-Host "1. Se aparecer em controller público antigo: candidato a desligamento/isolamento."
Write-Host "2. Se aparecer em webhook antigo: manter temporariamente e marcar como legado."
Write-Host "3. Se aparecer no novo módulo secretariapay/message-history ou message-dispatch: revisar, pois não deve depender do legado."
Write-Host ""
Write-Host "== Fim da varredura ==" -ForegroundColor Green
