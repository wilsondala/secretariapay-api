-- ==========================================================
-- Conciliação automática interna
-- Garante que toda cobrança marcada como PAID gere uma linha de
-- payment_transactions, mesmo quando o provedor externo ainda não
-- envia webhook detalhado.
-- ==========================================================

CREATE OR REPLACE FUNCTION spay_record_internal_payment_transaction()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'PAID' AND (TG_OP = 'INSERT' OR OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO payment_transactions (
            id,
            charge_id,
            student_id,
            provider,
            provider_transaction_id,
            merchant_transaction_id,
            payment_method,
            amount,
            currency,
            status,
            raw_payload,
            paid_at,
            created_at,
            updated_at
        ) VALUES (
            md5(random()::text || clock_timestamp()::text || NEW.id::text)::uuid,
            NEW.id,
            NEW.student_id,
            'SECRETARIAPAY_INTERNAL',
            NEW.charge_code,
            NEW.charge_code,
            'AUTO_RECONCILIATION',
            COALESCE(NEW.total_amount, NEW.amount, 0),
            COALESCE(NEW.currency, 'AOA'),
            'PAID',
            'Registro automático criado ao detectar cobrança PAID.',
            COALESCE(NEW.paid_at, now()),
            now(),
            now()
        )
        ON CONFLICT (provider, provider_transaction_id) DO UPDATE SET
            amount = EXCLUDED.amount,
            currency = EXCLUDED.currency,
            status = EXCLUDED.status,
            paid_at = EXCLUDED.paid_at,
            updated_at = now();
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_spay_record_internal_payment_transaction ON charges;

CREATE TRIGGER trg_spay_record_internal_payment_transaction
AFTER INSERT OR UPDATE OF status ON charges
FOR EACH ROW
EXECUTE FUNCTION spay_record_internal_payment_transaction();
