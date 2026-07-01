# Correção do DTO WhatsAppCloudSendResult

Este ajuste mantém compatibilidade com os serviços antigos e novos de WhatsApp.

Campos e métodos garantidos:

- success / getSuccess() / setSuccess(Boolean)
- providerMessageId / getProviderMessageId() / setProviderMessageId(String)
- errorMessage / getErrorMessage() / setErrorMessage(String)
- rawResponse / getRawResponse() / setRawResponse(String)

Motivo:

Alguns serviços existentes ainda utilizam getSuccess() e setRawResponse(), enquanto o novo cliente de WhatsApp Cloud API também depende destes campos para registrar sucesso, erro e resposta bruta do provedor.
