# Receipt PDF URL

## Objetivo

Garantir que todo recibo digital do SecretáriaPay retorne um link direto para o PDF institucional.

## Endpoint do PDF

```text
GET /api/v1/receipts/{receiptId}/pdf
```

## Comportamento

Ao emitir um novo recibo, o serviço agora:

1. Cria o recibo.
2. Salva o recibo para gerar o UUID.
3. Monta o link do PDF.
4. Atualiza o campo `pdfUrl`.
5. Retorna o recibo com `pdfUrl` preenchido.

Exemplo:

```json
{
  "receiptCode": "RCT1782913501413",
  "status": "VALID",
  "pdfUrl": "https://secretariapay-api.paixaoangola.com/api/v1/receipts/7b1dd67d-797c-4884-b062-c32393cb0300/pdf"
}
```

## Compatibilidade com recibos antigos

Para recibos emitidos antes desta melhoria, se o campo `pdfUrl` estiver nulo no banco,
a resposta da API calcula o link automaticamente usando o `receiptId`.

Assim, o painel administrativo e o WhatsApp já podem exibir o link do PDF mesmo para recibos antigos.
