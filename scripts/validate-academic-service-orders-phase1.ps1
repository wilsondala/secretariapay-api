param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$DcrEmail = $env:SECRETARIAPAY_VALIDATION_DCR_EMAIL,
    [string]$SecretariaEmail = $env:SECRETARIAPAY_VALIDATION_SECRETARIA_EMAIL,
    [string]$DirecaoEmail = $env:SECRETARIAPAY_VALIDATION_DIRECAO_EMAIL,
    [string]$Password = $env:SECRETARIAPAY_VALIDATION_PASSWORD,
    [string]$StudentNumber = "202301404",
    [string]$ServiceCode = "DECLARATION_WITHOUT_GRADES",
    [string]$PhysicalLocation = "Secretaria Académica do IMETRO",
    [string]$RecipientName = "Wilson Dala",
    [string]$RecipientDocumentNumber = "006123456LA042",
    [string]$OutputDirectory = "artifacts/academic-service-orders-validation"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Require-Value([string]$Name, [string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "A variável/parâmetro $Name é obrigatório para a validação controlada."
    }
}

function Connect-Profile([string]$Email) {
    $response = Invoke-RestMethod -Method POST -Uri "$BaseUrl/api/v1/auth/login" `
        -ContentType "application/json; charset=utf-8" `
        -Body (@{ email = $Email; password = $Password } | ConvertTo-Json)

    if ([string]::IsNullOrWhiteSpace([string]$response.token)) {
        throw "A autenticação de $Email não devolveu token JWT."
    }

    return @{
        Email = $Email
        Headers = @{ Authorization = "Bearer $($response.token)"; Accept = "application/json" }
    }
}

function Invoke-JsonApi {
    param(
        [Parameter(Mandatory = $true)][ValidateSet("GET", "POST", "PUT", "PATCH", "DELETE")][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][hashtable]$Profile,
        [AllowNull()][object]$Body
    )

    $parameters = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $Profile.Headers
        ContentType = "application/json; charset=utf-8"
    }
    if ($null -ne $Body) {
        $parameters.Body = $Body | ConvertTo-Json -Depth 12
    }
    return Invoke-RestMethod @parameters
}

function Assert-Forbidden {
    param(
        [Parameter(Mandatory = $true)][ValidateSet("GET", "POST", "PUT", "PATCH", "DELETE")][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][hashtable]$Profile,
        [AllowNull()][object]$Body,
        [Parameter(Mandatory = $true)][string]$Description
    )

    try {
        $null = Invoke-JsonApi -Method $Method -Path $Path -Profile $Profile -Body $Body
        throw "Falha de segurança: $Description foi permitido para $($Profile.Email)."
    }
    catch {
        $statusCode = $null
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        if ($statusCode -ne 403) {
            throw "Era esperado HTTP 403 em '$Description', mas foi recebido $statusCode. Erro: $($_.Exception.Message)"
        }
        Write-Host "403 confirmado: $Description" -ForegroundColor DarkGreen
    }
}

function Assert-Status([object]$Order, [string]$Expected) {
    if ([string]$Order.status -ne $Expected) {
        throw "Estado inválido para o pedido $($Order.orderCode). Esperado: $Expected; recebido: $($Order.status)."
    }
    Write-Host "Estado confirmado: $Expected" -ForegroundColor Green
}

Require-Value "DcrEmail / SECRETARIAPAY_VALIDATION_DCR_EMAIL" $DcrEmail
Require-Value "SecretariaEmail / SECRETARIAPAY_VALIDATION_SECRETARIA_EMAIL" $SecretariaEmail
Require-Value "DirecaoEmail / SECRETARIAPAY_VALIDATION_DIRECAO_EMAIL" $DirecaoEmail
Require-Value "Password / SECRETARIAPAY_VALIDATION_PASSWORD" $Password

$BaseUrl = $BaseUrl.TrimEnd('/')
if ($BaseUrl -match '^https://secretariapay-api\.paixaoangola\.com') {
    throw "Validação interrompida: este roteiro não pode ser executado na API de produção. Use ambiente local ou homologação."
}

$OutputDirectory = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputDirectory))
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$transcript = [System.Collections.Generic.List[object]]::new()

Write-Step "Autenticar DCR, Secretaria e Direção"
$dcr = Connect-Profile $DcrEmail
$secretaria = Connect-Profile $SecretariaEmail
$direcao = Connect-Profile $DirecaoEmail
Write-Host "Perfis autenticados sem expor tokens." -ForegroundColor Green

Write-Step "Localizar estudante e serviço académico"
$student = Invoke-JsonApi GET "/api/v1/students/number/$StudentNumber" $dcr $null
$servicesResponse = Invoke-JsonApi GET "/api/v1/academic-services?activeOnly=true" $dcr $null

if ($null -ne $servicesResponse -and $servicesResponse.PSObject.Properties.Name -contains 'content') {
    $services = @($servicesResponse.content | ForEach-Object { $_ })
}
else {
    $services = @($servicesResponse | ForEach-Object { $_ })
}

$serviceMatches = @($services | Where-Object { [string]$_.code -eq $ServiceCode })
if ($serviceMatches.Count -eq 0) {
    throw "Serviço ativo não encontrado: $ServiceCode"
}
if ($serviceMatches.Count -gt 1) {
    throw "Foram encontrados $($serviceMatches.Count) serviços ativos com o código $ServiceCode. A validação exige exatamente um serviço."
}

$service = $serviceMatches[0]
if ([string]::IsNullOrWhiteSpace([string]$service.id)) {
    throw "O serviço $ServiceCode foi localizado, mas não possui um ID válido."
}

Write-Host "Estudante: $($student.studentNumber) · $($student.fullName)" -ForegroundColor Green
Write-Host "Serviço: $($service.code) · $($service.name) · ID $($service.id)" -ForegroundColor Green

Write-Step "DCR regista o pedido"
$order = Invoke-JsonApi POST "/api/v1/academic-service-orders" $dcr @{
    studentId = [string]$student.id
    serviceId = [string]$service.id
    purpose = "Validação controlada do fluxo institucional"
    notes = "Pedido criado automaticamente pelo roteiro de homologação."
}
Assert-Status $order "SOLICITADO"
$transcript.Add($order) | Out-Null

Write-Step "Validar que a Secretaria não pode emitir cobrança"
Assert-Forbidden POST "/api/v1/academic-service-orders/$($order.id)/request-payment" $secretaria @{ dueDate = (Get-Date).AddDays(3).ToString('yyyy-MM-dd') } "Secretaria emitir cobrança"

Write-Step "DCR emite a cobrança institucional"
$order = Invoke-JsonApi POST "/api/v1/academic-service-orders/$($order.id)/request-payment" $dcr @{
    dueDate = (Get-Date).AddDays(3).ToString('yyyy-MM-dd')
}
Assert-Status $order "AGUARDANDO_PAGAMENTO"
if ([string]::IsNullOrWhiteSpace([string]$order.chargeId)) {
    throw "O pedido não recebeu chargeId após a emissão da cobrança."
}
$transcript.Add($order) | Out-Null

Write-Step "Validar que o pedido ainda não entrou na operação da Secretaria"
Assert-Forbidden POST "/api/v1/academic-service-orders/$($order.id)/generate-document" $dcr $null "DCR gerar documento"

Write-Step "DCR confirma o pagamento"
$null = Invoke-JsonApi PATCH "/api/v1/charges/$($order.chargeId)/confirm-payment" $dcr $null
$order = Invoke-JsonApi GET "/api/v1/academic-service-orders/$($order.id)" $dcr $null
Assert-Status $order "PAGO"
if ([string]$order.chargeStatus -ne "PAID") {
    throw "A cobrança não ficou PAID após a confirmação."
}
$transcript.Add($order) | Out-Null

Write-Step "Secretaria gera e prepara o documento"
$order = Invoke-JsonApi POST "/api/v1/academic-service-orders/$($order.id)/generate-document" $secretaria $null
Assert-Status $order "DOCUMENTO_GERADO"
$order = Invoke-JsonApi POST "/api/v1/academic-service-orders/$($order.id)/ready-for-print" $secretaria $null
Assert-Status $order "PRONTO_PARA_IMPRESSAO"
$order = Invoke-JsonApi POST "/api/v1/academic-service-orders/$($order.id)/print" $secretaria $null
Assert-Status $order "IMPRESSO"
$order = Invoke-JsonApi POST "/api/v1/academic-service-orders/$($order.id)/submit-signature" $secretaria $null
Assert-Status $order "AGUARDANDO_ASSINATURA"
$transcript.Add($order) | Out-Null

Write-Step "Validar segregação da assinatura"
Assert-Forbidden POST "/api/v1/academic-service-orders/$($order.id)/sign" $secretaria $null "Secretaria assinar documento"

Write-Step "Direção confirma a assinatura"
$order = Invoke-JsonApi POST "/api/v1/academic-service-orders/$($order.id)/sign" $direcao $null
Assert-Status $order "ASSINADO"
$transcript.Add($order) | Out-Null

Write-Step "Validar que a Direção não executa a disponibilização física"
Assert-Forbidden POST "/api/v1/academic-service-orders/$($order.id)/ready-for-pickup" $direcao @{ physicalLocation = $PhysicalLocation } "Direção disponibilizar documento para levantamento"

Write-Step "Secretaria confirma disponibilidade física e envia WhatsApp"
$order = Invoke-JsonApi POST "/api/v1/academic-service-orders/$($order.id)/ready-for-pickup" $secretaria @{
    physicalLocation = $PhysicalLocation
}
Assert-Status $order "PRONTO_PARA_LEVANTAMENTO"
$order = Invoke-JsonApi POST "/api/v1/academic-service-orders/$($order.id)/send-pickup-whatsapp" $secretaria $null
Assert-Status $order "WHATSAPP_ENVIADO"
$transcript.Add($order) | Out-Null

Write-Step "Secretaria regista o levantamento e a entrega"
$order = Invoke-JsonApi POST "/api/v1/academic-service-orders/$($order.id)/deliver" $secretaria @{
    recipientName = $RecipientName
    recipientDocumentNumber = $RecipientDocumentNumber
    notes = "Entrega validada no roteiro controlado."
}
Assert-Status $order "ENTREGUE"
if ([string]::IsNullOrWhiteSpace([string]$order.deliveredAt)) {
    throw "A entrega não registou deliveredAt."
}
$transcript.Add($order) | Out-Null

$resultPath = Join-Path $OutputDirectory "academic-service-order-$($order.orderCode).json"
@{
    validatedAt = (Get-Date).ToString('o')
    baseUrl = $BaseUrl
    studentNumber = $StudentNumber
    serviceCode = $ServiceCode
    finalOrder = $order
    transitions = $transcript
} | ConvertTo-Json -Depth 20 | Set-Content -Path $resultPath -Encoding UTF8

Write-Host "`nVALIDAÇÃO CONTROLADA APROVADA" -ForegroundColor Green
Write-Host "Pedido: $($order.orderCode)"
Write-Host "Estado final: $($order.status)"
Write-Host "Evidência: $resultPath"
