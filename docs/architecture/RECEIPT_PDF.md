# PDF institucional de recibos

Este pacote adiciona a emissão de PDF institucional para recibos do SecretáriaPay Académico.

## Endpoint

`GET /api/v1/receipts/{id}/pdf`

## Conteúdo do PDF

- Logotipo institucional do IMETRO.
- Nome da universidade.
- Código do recibo.
- Código da cobrança.
- Dados do estudante.
- Curso, turma e ano académico.
- Descrição da cobrança.
- Valor pago em AOA.
- Data de pagamento e emissão.
- QR Code com validação pública.
- Rodapé institucional da SecretáriaPay Académico.

## Validação pública

O QR Code aponta para:

`/api/v1/public/receipts/validate/{receiptCode}`

