param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Email = "admin@secretariapay.com",
    [string]$Password = $env:SECRETARIAPAY_LOCAL_ADMIN_PASSWORD,
    [string]$OutputDirectory = "artifacts/local-branding-demo"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Invoke-JsonApi {
    param(
        [Parameter(Mandatory = $true)][ValidateSet("GET", "POST", "PUT", "PATCH", "DELETE")][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [object]$Body,
        [switch]$AllowNotFound
    )

    $uri = "$BaseUrl$Path"
    $parameters = @{
        Method      = $Method
        Uri         = $uri
        Headers     = $script:Headers
        ContentType = "application/json; charset=utf-8"
    }

    if ($null -ne $Body) {
        $parameters.Body = ($Body | ConvertTo-Json -Depth 12)
    }

    try {
        return Invoke-RestMethod @parameters
    }
    catch {
        $statusCode = $null
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        if ($AllowNotFound -and $statusCode -eq 404) {
            return $null
        }
        throw "Falha em $Method $uri. Status: $statusCode. $($_.Exception.Message)"
    }
}

function Download-Pdf {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Destination,
        [switch]$Public
    )

    $uri = "$BaseUrl$Path"
    $parameters = @{
        Uri     = $uri
        OutFile = $Destination
    }
    if (-not $Public) {
        $parameters.Headers = $script:Headers
    }

    Invoke-WebRequest @parameters | Out-Null

    $file = Get-Item $Destination
    if ($file.Length -lt 1000) {
        throw "O arquivo $Destination foi criado, mas parece inválido: $($file.Length) bytes."
    }
    Write-Host "PDF criado: $($file.FullName) ($([math]::Round($file.Length / 1KB, 1)) KB)" -ForegroundColor Green
}

if ([string]::IsNullOrWhiteSpace($Password)) {
    $securePassword = Read-Host "Senha do utilizador administrativo local" -AsSecureString
    $credential = [System.Net.NetworkCredential]::new("", $securePassword)
    $Password = $credential.Password
}

$BaseUrl = $BaseUrl.TrimEnd('/')
$OutputDirectory = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputDirectory))
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

Write-Step "Autenticar na API local"
$loginBody = @{
    email    = $Email
    password = $Password
}
$login = Invoke-RestMethod \
    -Method POST \
    -Uri "$BaseUrl/api/v1/auth/login" \
    -ContentType "application/json; charset=utf-8" \
    -Body ($loginBody | ConvertTo-Json)

if ([string]::IsNullOrWhiteSpace($login.token)) {
    throw "A autenticação não devolveu um token JWT."
}

$script:Headers = @{
    Authorization = "Bearer $($login.token)"
    Accept        = "application/json"
}
Write-Host "Autenticado como $($login.user.email)." -ForegroundColor Green

Write-Step "Criar ou reutilizar a instituição local"
$institutions = @(Invoke-JsonApi -Method GET -Path "/api/v1/institutions")
$institution = $institutions | Where-Object {
    $_.name -eq "Instituto Superior Politécnico Metropolitano de Angola"
} | Select-Object -First 1

if ($null -eq $institution) {
    $institution = Invoke-JsonApi -Method POST -Path "/api/v1/institutions" -Body @{
        name      = "Instituto Superior Politécnico Metropolitano de Angola"
        legalName = "Instituto Superior Politécnico Metropolitano de Angola"
        nif       = "TESTE-IMETRO-LOCAL"
        email     = "secretaria.financeira@imetroangola.com"
        phone     = "+244 923 168 085"
        whatsapp  = "+244 923 168 085"
        address   = "Luanda, Angola"
        active    = $true
    }
    Write-Host "Instituição criada: $($institution.id)" -ForegroundColor Green
}
else {
    Write-Host "Instituição reutilizada: $($institution.id)" -ForegroundColor DarkGreen
}

Write-Step "Criar ou reutilizar o curso"
$courses = @(Invoke-JsonApi -Method GET -Path "/api/v1/courses?institutionId=$($institution.id)")
$course = $courses | Where-Object { $_.code -eq "GFB-TESTE" } | Select-Object -First 1

if ($null -eq $course) {
    $course = Invoke-JsonApi -Method POST -Path "/api/v1/courses" -Body @{
        institutionId = $institution.id
        name          = "Gestão Financeira e Bancária"
        code          = "GFB-TESTE"
        faculty       = "Faculdade de Ciências Económicas e Empresariais"
        durationYears = 4
        active        = $true
    }
    Write-Host "Curso criado: $($course.id)" -ForegroundColor Green
}
else {
    Write-Host "Curso reutilizado: $($course.id)" -ForegroundColor DarkGreen
}

Write-Step "Criar ou reutilizar a turma"
$classes = @(Invoke-JsonApi -Method GET -Path "/api/v1/academic-classes?courseId=$($course.id)")
$academicClass = $classes | Where-Object {
    $_.name -eq "GFB-1A-2026" -and $_.academicYear -eq "2026"
} | Select-Object -First 1

if ($null -eq $academicClass) {
    $academicClass = Invoke-JsonApi -Method POST -Path "/api/v1/academic-classes" -Body @{
        courseId    = $course.id
        name        = "GFB-1A-2026"
        academicYear = "2026"
        yearLevel   = 1
        active      = $true
    }
    Write-Host "Turma criada: $($academicClass.id)" -ForegroundColor Green
}
else {
    Write-Host "Turma reutilizada: $($academicClass.id)" -ForegroundColor DarkGreen
}

Write-Step "Criar ou reutilizar o estudante de teste"
$studentNumber = "202301404"
$student = Invoke-JsonApi -Method GET -Path "/api/v1/students/number/$studentNumber" -AllowNotFound

if ($null -eq $student) {
    $student = Invoke-JsonApi -Method POST -Path "/api/v1/students" -Body @{
        academicClassId   = $academicClass.id
        studentNumber     = $studentNumber
        fullName          = "Wilson dos Santos Kahango Dala"
        documentType      = "BI"
        documentNumber    = "006123456LA042"
        email             = "wilson.dala.teste@imetroangola.com"
        phone             = "+244 923 168 085"
        whatsapp          = "+244 923 168 085"
        birthDate         = "1990-01-01"
        status            = "ACTIVE"
        financiallyBlocked = $false
    }
    Write-Host "Estudante criado: $($student.id)" -ForegroundColor Green
}
else {
    Write-Host "Estudante reutilizado: $($student.id)" -ForegroundColor DarkGreen
}

Write-Step "Criar ou reutilizar a propina usada na validação visual"
$referenceMonth = "Julho/2026 - Branding"
$description = "Propina referente ao mês de Julho de 2026 - validação visual"
$studentCharges = @(Invoke-JsonApi -Method GET -Path "/api/v1/charges/student/$($student.id)")
$charge = $studentCharges | Where-Object {
    $_.referenceMonth -eq $referenceMonth -and $_.chargeCategory -eq "TUITION"
} | Select-Object -First 1

if ($null -eq $charge) {
    $charge = Invoke-JsonApi -Method POST -Path "/api/v1/charges" -Body @{
        studentId      = $student.id
        description    = $description
        referenceMonth = $referenceMonth
        chargeCategory = "TUITION"
        dueDate         = "2026-07-31"
        amount          = 45000.00
        fineAmount      = 9000.00
        interestAmount  = 90.00
        discountAmount  = 0.00
        currency        = "AOA"
    }
    Write-Host "Cobrança criada: $($charge.chargeCode)" -ForegroundColor Green
}
else {
    Write-Host "Cobrança reutilizada: $($charge.chargeCode)" -ForegroundColor DarkGreen
}

Write-Step "Gerar a Guia de Pagamento Académico com as novas marcas"
$guideFilename = "Guia_Pagamento_Academico_${studentNumber}_Julho_2026_$($charge.chargeCode).pdf"
$guidePath = Join-Path $OutputDirectory $guideFilename
Download-Pdf \
    -Path "/api/v1/public/payment-guides/$($charge.chargeCode)/pdf" \
    -Destination $guidePath \
    -Public

Write-Step "Simular confirmação DCR local para emitir o borderô"
if ($charge.status -ne "PAID") {
    $charge = Invoke-JsonApi -Method PATCH -Path "/api/v1/charges/$($charge.id)/confirm-payment"
    Write-Host "Pagamento confirmado localmente para validação visual." -ForegroundColor Yellow
}
else {
    Write-Host "A cobrança já estava paga no banco local." -ForegroundColor DarkGreen
}

$receipts = @(Invoke-JsonApi -Method GET -Path "/api/v1/receipts")
$receipt = $receipts | Where-Object { $_.chargeId -eq $charge.id } | Select-Object -First 1
if ($null -eq $receipt) {
    $receipt = Invoke-JsonApi -Method POST -Path "/api/v1/receipts/charge/$($charge.id)/issue"
    Write-Host "Recibo/borderô emitido: $($receipt.receiptCode)" -ForegroundColor Green
}
else {
    Write-Host "Recibo/borderô reutilizado: $($receipt.receiptCode)" -ForegroundColor DarkGreen
}

Write-Step "Gerar o borderô/comprovativo com as novas marcas"
$receiptFilename = "Comprovativo_Pagamentos_${studentNumber}_$($receipt.receiptCode).pdf"
$receiptPath = Join-Path $OutputDirectory $receiptFilename
Download-Pdf \
    -Path "/api/v1/receipts/$($receipt.id)/pdf" \
    -Destination $receiptPath

Write-Step "Criar ou reutilizar a declaração académica de demonstração"
$purpose = "Validação visual das novas marcas institucionais"
$documents = @(Invoke-JsonApi -Method GET -Path "/api/v1/academic-documents")
$academicDocument = $documents | Where-Object {
    $_.studentNumber -eq $studentNumber -and $_.purpose -eq $purpose
} | Select-Object -First 1

if ($null -eq $academicDocument) {
    $academicDocument = Invoke-JsonApi -Method POST -Path "/api/v1/academic-documents/demo/simple-declaration" -Body @{
        studentNumber  = $studentNumber
        purpose        = $purpose
        declarationText = "Para os devidos efeitos, declara-se que Wilson dos Santos Kahango Dala, titular da matrícula 202301404, encontra-se regularmente matriculado no curso de Gestão Financeira e Bancária no ano académico de 2026."
    }
    Write-Host "Declaração criada: $($academicDocument.documentCode)" -ForegroundColor Green
}
else {
    Write-Host "Declaração reutilizada: $($academicDocument.documentCode)" -ForegroundColor DarkGreen
}

if ($academicDocument.status -eq "DRAFT") {
    $academicDocument = Invoke-JsonApi -Method POST -Path "/api/v1/academic-documents/$($academicDocument.id)/ready-for-signature"
}
if ($academicDocument.status -eq "READY_FOR_SIGNATURE") {
    $academicDocument = Invoke-JsonApi -Method POST -Path "/api/v1/academic-documents/$($academicDocument.id)/sign-demo"
}

Write-Step "Gerar a declaração académica com as novas marcas"
$declarationFilename = "Declaracao_IMETRO_${studentNumber}_$($academicDocument.documentCode).pdf"
$declarationPath = Join-Path $OutputDirectory $declarationFilename
Download-Pdf \
    -Path "/api/v1/academic-documents/$($academicDocument.id)/pdf" \
    -Destination $declarationPath

Write-Host "`nValidação local preparada com sucesso." -ForegroundColor Green
Write-Host "Estudante: Wilson dos Santos Kahango Dala ($studentNumber)"
Write-Host "Propina: 45.000,00 Kz + 9.000,00 Kz de multa + 90,00 Kz de juros = 54.090,00 Kz"
Write-Host "Pasta dos PDFs: $OutputDirectory"
Write-Host "`nAbra os três arquivos e confirme alinhamento, proporção, nitidez e leitura das marcas." -ForegroundColor Cyan
