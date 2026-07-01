# Correção do link de PDF nas mensagens de recibo

## Contexto

O endpoint de preview de mensagem de recibo emitido estava usando diretamente `receipt.getPdfUrl()`.
Para recibos antigos ou emitidos antes da atualização do campo `pdfUrl`, esse valor pode estar `null`, mesmo que o endpoint de PDF funcione corretamente.

## Ajuste

A mensagem `RECEIPT_ISSUED` agora usa uma função de fallback:

```text
https://secretariapay-api.paixaoangola.com/api/v1/receipts/{receiptId}/pdf
```

Assim, a mensagem enviada ao estudante nunca mostra `null` no campo “Baixar recibo”.

## Melhoria adicional

As mensagens passam a usar `legalName` da instituição quando disponível, permitindo exibir:

```text
Instituto Superior Politécnico Metropolitano de Angola (IMETRO)
```

em vez do nome simplificado.
