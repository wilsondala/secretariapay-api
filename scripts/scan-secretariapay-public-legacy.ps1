param(
    [string]$ProjectRoot = "C:\Users\dalaw\secretariapay-api"
)

Set-Location $ProjectRoot

Write-Host "== SecretáriaPay: varredura de legado público e fluxo antigo ==" -ForegroundColor Cyan
Write-Host "Projeto: $ProjectRoot"
Write-Host ""

$patterns = @(
    "VaiRápido",
    "VaiRapido",
    "vairapido",
    "api-vairapido",
    "Comprar passagem",
    "passagem",
    "passagens",
    "bilhete",
    "ticket",
    "trip",
    "travel",
    "route",
    "passenger",
    "transport",
    "boarding",
    "embarque",
    "poltrona",
    "Pix",
    "pix"
)

$paths = @(
    ".\src\main\java\com\secretariapay\api\**\*.java",
    ".\src\main\resources\**\*",
    ".\docs\**\*",
    ".\.env.example",
    ".\.env.production.example",
    ".\README.md"
)

foreach ($pattern in $patterns) {
    Write-Host "\n--- Procurando: $pattern ---" -ForegroundColor Yellow
    Select-String -Path $paths -Pattern $pattern -CaseSensitive:$false -ErrorAction SilentlyContinue |
        ForEach-Object {
            "{0}:{1}: {2}" -f $_.Path.Replace($ProjectRoot + "\", ""), $_.LineNumber, $_.Line.Trim()
        }
}

Write-Host "\n== Fim da varredura ==" -ForegroundColor Cyan
