param(
    [string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " VaiRápido API - Inicialização local" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

if (!(Test-Path $EnvFile)) {
    Write-Host "Arquivo $EnvFile não encontrado." -ForegroundColor Yellow
    Write-Host "Crie o arquivo .env com base no .env.example." -ForegroundColor Yellow
    exit 1
}

Write-Host "Carregando variáveis de ambiente de: $EnvFile" -ForegroundColor Green

Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()

    if ([string]::IsNullOrWhiteSpace($line)) {
        return
    }

    if ($line.StartsWith("#")) {
        return
    }

    $separatorIndex = $line.IndexOf("=")

    if ($separatorIndex -le 0) {
        return
    }

    $key = $line.Substring(0, $separatorIndex).Trim()
    $value = $line.Substring($separatorIndex + 1).Trim()

    if (
        ($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))
    ) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    [Environment]::SetEnvironmentVariable($key, $value, "Process")
}

Write-Host ""
Write-Host "Configuração WhatsApp:" -ForegroundColor Cyan
Write-Host "VAIRAPIDO_WHATSAPP_ENABLED=$env:VAIRAPIDO_WHATSAPP_ENABLED"
Write-Host "VAIRAPIDO_WHATSAPP_GRAPH_API_VERSION=$env:VAIRAPIDO_WHATSAPP_GRAPH_API_VERSION"
Write-Host "VAIRAPIDO_WHATSAPP_PHONE_NUMBER_ID=$env:VAIRAPIDO_WHATSAPP_PHONE_NUMBER_ID"

if ($env:VAIRAPIDO_WHATSAPP_ACCESS_TOKEN) {
    Write-Host "VAIRAPIDO_WHATSAPP_ACCESS_TOKEN=********"
} else {
    Write-Host "VAIRAPIDO_WHATSAPP_ACCESS_TOKEN="
}

Write-Host ""
Write-Host "Iniciando Spring Boot..." -ForegroundColor Green

& "C:\tools\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run