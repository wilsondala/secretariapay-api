package com.secretariapay.api.entity.enums.academic;

public enum AcademicServiceOrderStatus {
    SOLICITADO,
    AGUARDANDO_PAGAMENTO,
    PAGO,
    DOCUMENTO_GERADO,
    PRONTO_PARA_IMPRESSAO,
    IMPRESSO,
    AGUARDANDO_ASSINATURA,
    ASSINADO,
    PRONTO_PARA_LEVANTAMENTO,
    WHATSAPP_ENVIADO,
    ENTREGUE;

    public boolean isArchived() {
        return this == ASSINADO
                || this == PRONTO_PARA_LEVANTAMENTO
                || this == WHATSAPP_ENVIADO
                || this == ENTREGUE;
    }
}