cd C:\Users\dalaw\secretariapay-api

Write-Host "=== SecretáriaPay Legacy Scan ===" -ForegroundColor Cyan

Select-String -Path .\src\main\java\com\secretariapay\api\**\*.java,.\src\main\resources\**\* `
  -Pattern "vairapido|VaiRápido|VaiRapido|api-vairapido|Comprar passagem|bilhete-vairapido|passagem|ticket|trip|passenger|transport|boarding|pix" `
  -CaseSensitive:$false |
  Sort-Object Path, LineNumber |
  ForEach-Object {
    "{0}:{1}: {2}" -f $_.Path, $_.LineNumber, $_.Line.Trim()
  }
