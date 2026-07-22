package com.secretariapay.api.entity.enums;

public enum UserRole {
    ADMIN_GLOBAL,
    ADMIN_INSTITUTION,
    ADMIN_IMETRO,
    DIRECAO,
    FINANCEIRO,
    TESOURARIA,
    SECRETARIA,
    ADMISSOES,
    MARKETING,
    OPERADOR_ATENDIMENTO,
    DCR_COORDENACAO,
    DCR_OPERADOR,
    TIC,
    AUDITORIA,

    // Papéis legados mantidos temporariamente para não quebrar a base herdada do VaiRápido.
    ADMIN,
    COMPANY_ADMIN,
    OPERATOR
}
