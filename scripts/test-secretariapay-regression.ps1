param(
    [string]$BaseUrl = "https://secretariapay-api.paixaoangola.com",
    [string]$Email = "admin@secretariapay.com",
    [string]$Password = "Admin@123456"
)

Write-Host "== SecretáriaPay | Fase 11 - Teste final de regressão ==" -ForegroundColor Cyan
Write-Host "BaseUrl: $BaseUrl"
Write-Host ""

function Invoke-Check {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    Write-Host "-- $Name" -ForegroundColor Yellow
    try {
        & $Action
        Write-Host "OK: $Name" -ForegroundColor Green
    } catch {
        Write-Host "FALHOU: $Name" -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
    Write-Host ""
}

$token = $null

Invoke-Check "Health público" {
    $health = Invoke-RestMethod -Method GET -Uri "$BaseUrl/actuator/health"
    if ($health.status -ne "UP") { throw "Health não está UP" }
    $health | ConvertTo-Json -Depth 6
}

Invoke-Check "Login institucional" {
    $loginBody = @{
        email = $Email
        password = $Password
    } | ConvertTo-Json

    $login = Invoke-RestMethod -Method POST -Uri "$BaseUrl/api/v1/auth/login" -ContentType "application/json" -Body $loginBody
    if (-not $login.token) { throw "Token não retornado" }
    $script:token = $login.token
    Write-Host "Token recebido: $($script:token.Substring(0, 20))..."
}

$headers = @{}
if ($token) {
    $headers.Authorization = "Bearer $token"
}

Invoke-Check "Branding público SecretáriaPay" {
    $branding = Invoke-RestMethod -Method GET -Uri "$BaseUrl/api/v1/public/branding/secretariapay"
    if ($branding.name -ne "SecretáriaPay") { throw "Nome de branding inesperado" }
    $branding | ConvertTo-Json -Depth 6
}

Invoke-Check "Branding público IMETRO" {
    $imetro = Invoke-RestMethod -Method GET -Uri "$BaseUrl/api/v1/public/branding/institutions/imetro"
    if (-not $imetro.institutionName) { throw "Branding institucional sem institutionName" }
    $imetro | ConvertTo-Json -Depth 6
}

Invoke-Check "Logo pública" {
    $response = Invoke-WebRequest -Method HEAD -Uri "$BaseUrl/branding/secretariapay-logo.png"
    if ($response.StatusCode -ne 200) { throw "Logo não retornou HTTP 200" }
    if ($response.Headers["Content-Type"] -notmatch "image/png") { throw "Logo não retornou image/png" }
    Write-Host "HTTP $($response.StatusCode) - $($response.Headers['Content-Type'])"
}

Invoke-Check "Instituições com token" {
    $institutions = Invoke-RestMethod -Method GET -Uri "$BaseUrl/api/v1/institutions" -Headers $headers
    if ($null -eq $institutions) { throw "Instituições não retornadas" }
    $institutions | ConvertTo-Json -Depth 6
}

Invoke-Check "Cursos com token" {
    $courses = Invoke-RestMethod -Method GET -Uri "$BaseUrl/api/v1/courses" -Headers $headers
    if ($null -eq $courses) { throw "Cursos não retornados" }
    $courses | ConvertTo-Json -Depth 6
}

Invoke-Check "Dashboard financeiro com token" {
    $dashboard = Invoke-RestMethod -Method GET -Uri "$BaseUrl/api/v1/dashboard/financial" -Headers $headers
    if ($null -eq $dashboard.totalStudents) { throw "Dashboard sem totalStudents" }
    $dashboard | ConvertTo-Json -Depth 6
}

Invoke-Check "Comprovativos com token" {
    $proofs = Invoke-RestMethod -Method GET -Uri "$BaseUrl/api/v1/payment-proofs" -Headers $headers
    if ($null -eq $proofs) { throw "Comprovativos não retornados" }
    $proofs | ConvertTo-Json -Depth 8
}

Invoke-Check "Recibos com token" {
    $receipts = Invoke-RestMethod -Method GET -Uri "$BaseUrl/api/v1/receipts" -Headers $headers
    if ($null -eq $receipts) { throw "Recibos não retornados" }
    $receipts | ConvertTo-Json -Depth 8
}

Write-Host "== Fim do teste de regressão ==" -ForegroundColor Cyan
