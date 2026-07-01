# Validação pública de recibos

Este módulo expõe a rota pública de validação de recibo digital:

GET /api/v1/public/receipts/validate/{receiptCode}

A rota não exige token JWT porque é usada pelo QR Code do recibo. Ela retorna os dados públicos do recibo, cobrança e estudante necessários para confirmar validade institucional.

Exemplo:
https://secretariapay-api.paixaoangola.com/api/v1/public/receipts/validate/RCT1782913501413
