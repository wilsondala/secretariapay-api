package com.secretariapay.api.entity.enums;

public enum UserRole {
    ADMIN_GLOBAL,
    ADMIN_INSTITUTION,
    DIRECAO,
    FINANCEIRO,
    TESOURARIA,
    SECRETARIA,
    OPERADOR_ATENDIMENTO,

    // Papéis legados mantidos temporariamente para não quebrar a base herdada do VaiRápido.
    ADMIN,
    COMPANY_ADMIN,
    OPERATOR
}
