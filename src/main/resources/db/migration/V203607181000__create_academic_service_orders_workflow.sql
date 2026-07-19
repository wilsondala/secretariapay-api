CREATE TABLE IF NOT EXISTS academic_service_orders (
    id UUID PRIMARY KEY,
    order_code VARCHAR(80) NOT NULL UNIQUE,
    student_id UUID NOT NULL REFERENCES students(id),
    service_id UUID NOT NULL REFERENCES academic_service_catalog(id),
    charge_id UUID UNIQUE REFERENCES charges(id),
    document_request_id UUID UNIQUE REFERENCES academic_document_requests(id),
    status VARCHAR(40) NOT NULL DEFAULT 'SOLICITADO',
    purpose VARCHAR(240),
    notes TEXT,
    physical_location VARCHAR(180),
    requested_by VARCHAR(180),
    printed_by VARCHAR(180),
    signed_by VARCHAR(180),
    whatsapp_sent_by VARCHAR(180),
    delivered_by VARCHAR(180),
    recipient_name VARCHAR(180),
    recipient_document_number VARCHAR(80),
    delivery_notes VARCHAR(500),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_requested_at TIMESTAMP,
    payment_confirmed_at TIMESTAMP,
    document_generated_at TIMESTAMP,
    ready_for_print_at TIMESTAMP,
    printed_at TIMESTAMP,
    waiting_signature_at TIMESTAMP,
    signed_at TIMESTAMP,
    ready_for_pickup_at TIMESTAMP,
    whatsapp_sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_academic_service_orders_status CHECK (
        status IN (
            'SOLICITADO',
            'AGUARDANDO_PAGAMENTO',
            'PAGO',
            'DOCUMENTO_GERADO',
            'PRONTO_PARA_IMPRESSAO',
            'IMPRESSO',
            'AGUARDANDO_ASSINATURA',
            'ASSINADO',
            'PRONTO_PARA_LEVANTAMENTO',
            'WHATSAPP_ENVIADO',
            'ENTREGUE'
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_academic_service_orders_status
    ON academic_service_orders(status, created_at);

CREATE INDEX IF NOT EXISTS idx_academic_service_orders_student
    ON academic_service_orders(student_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_academic_service_orders_service
    ON academic_service_orders(service_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_academic_service_orders_ready_secretaria
    ON academic_service_orders(status, payment_confirmed_at)
    WHERE status IN (
        'PAGO',
        'DOCUMENTO_GERADO',
        'PRONTO_PARA_IMPRESSAO',
        'IMPRESSO',
        'AGUARDANDO_ASSINATURA',
        'ASSINADO',
        'PRONTO_PARA_LEVANTAMENTO',
        'WHATSAPP_ENVIADO'
    );