# Fase 6 - Marcar serviços WhatsApp antigos como legado
# Uso: executar na raiz do projeto secretariapay-api
# Este script NÃO apaga arquivos. Ele apenas adiciona marcação @Deprecated em serviços herdados do VaiRápido.

$ErrorActionPreference = "Stop"

$files = @(
  @{
    Path = "src/main/java/com/secretariapay/api/service/WhatsappCommandService.java"
    ClassName = "WhatsappCommandService"
    Reason = "Serviço legado do fluxo antigo de passagens/VaiRápido. Não usar para o SecretáriaPay Académico. Manter temporariamente até remoção controlada."
  },
  @{
    Path = "src/main/java/com/secretariapay/api/service/WhatsappFaqAnswerService.java"
    ClassName = "WhatsappFaqAnswerService"
    Reason = "FAQ legado do fluxo antigo de passagens/VaiRápido. Não usar para o SecretáriaPay Académico. Manter temporariamente até remoção controlada."
  }
)

foreach ($item in $files) {
  $path = $item.Path
  $className = $item.ClassName
  $reason = $item.Reason

  if (!(Test-Path $path)) {
    Write-Host "Arquivo não encontrado: $path" -ForegroundColor Yellow
    continue
  }

  $content = Get-Content -Path $path -Raw -Encoding UTF8

  $alreadyMarkedPattern = '@Deprecated\(since = "2026-07-02", forRemoval = false\)\s*public class ' + [regex]::Escape($className)
  if ($content -match $alreadyMarkedPattern) {
    Write-Host "Já marcado como legado: $path" -ForegroundColor Cyan
    continue
  }

  $legacyBlock = @"
/**
 * LEGADO TEMPORÁRIO.
 * $reason
 */
@Service
@Deprecated(since = "2026-07-02", forRemoval = false)
public class $className
"@

  $pattern = '@Service\s+public class ' + [regex]::Escape($className)

  if ($content -notmatch $pattern) {
    Write-Host "Padrão não encontrado para alteração segura: $path" -ForegroundColor Red
    continue
  }

  $content = [regex]::Replace($content, $pattern, $legacyBlock, 1)
  Set-Content -Path $path -Value $content -Encoding UTF8
  Write-Host "Marcado como legado: $path" -ForegroundColor Green
}

Write-Host "`nFase 6 concluída. Rode o build agora:" -ForegroundColor Green
Write-Host '& "C:\tools\apache-maven-3.9.16\bin\mvn.cmd" clean package -DskipTests'
