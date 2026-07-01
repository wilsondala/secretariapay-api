# Correção de compatibilidade - WhatsAppCloudSendResult

Este ajuste torna o DTO compatível com os serviços antigos e novos do SecretáriaPay.

Inclui:

- getSuccess()
- isSuccess()
- setRawResponse(...)
- getRawResponse()
- sent(providerMessageId, httpStatus)
- failed(errorMessage, httpStatus)
- httpStatus

Objetivo: permitir que WhatsAppService, WhatsAppCloudClient e WhatsAppCloudApiClient compilem sem conflito durante a migração para o envio real via WhatsApp Cloud API.
