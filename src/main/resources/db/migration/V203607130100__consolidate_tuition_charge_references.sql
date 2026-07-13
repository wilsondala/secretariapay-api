-- ==========================================================
-- SecretáriaPay Académico / IMETRO
-- Consolidação de propinas por estudante e período financeiro.
--
-- Objetivos:
-- 1. preservar integralmente cobranças já pagas e respetivos comprovativos;
-- 2. cancelar cobranças abertas quando o mesmo estudante/mês já está pago;
-- 3. cancelar cobranças abertas repetidas, mantendo apenas a mais antiga;
-- 4. impedir novas duplicidades abertas por estudante/mês.
--
-- Nenhum lançamento é eliminado. A limpeza mantém histórico e auditoria.
-- ==========================================================

-- Quando existe propina paga no período, cobranças ainda abertas do mesmo
-- estudante e mês deixam de ser exigíveis.
WITH paid_periods AS (
    SELECT DISTINCT
           student_id,
           date_trunc('month', due_date)::date AS period_start
    FROM charges
    WHERE status = 'PAID'
      AND (
            lower(coalesce(charge_code, '')) LIKE '%propina%'
            OR lower(coalesce(description, '')) LIKE '%propina%'
          )
)
UPDATE charges AS open_charge
SET status = 'CANCELLED',
    cancelled_at = coalesce(open_charge.cancelled_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
FROM paid_periods AS paid
WHERE open_charge.student_id = paid.student_id
  AND date_trunc('month', open_charge.due_date)::date = paid.period_start
  AND open_charge.status IN ('PENDING', 'OVERDUE', 'PARTIALLY_PAID')
  AND (
        lower(coalesce(open_charge.charge_code, '')) LIKE '%propina%'
        OR lower(coalesce(open_charge.description, '')) LIKE '%propina%'
      );

-- Nos períodos que ainda não foram pagos, mantém-se somente uma cobrança
-- aberta canónica. As restantes permanecem no histórico como canceladas.
WITH ranked_open_tuition AS (
    SELECT
        id,
        row_number() OVER (
            PARTITION BY student_id, date_trunc('month', due_date)
            ORDER BY created_at ASC, id ASC
        ) AS duplicate_rank
    FROM charges
    WHERE status IN ('PENDING', 'OVERDUE', 'PARTIALLY_PAID')
      AND (
            lower(coalesce(charge_code, '')) LIKE '%propina%'
            OR lower(coalesce(description, '')) LIKE '%propina%'
          )
)
UPDATE charges AS duplicate_charge
SET status = 'CANCELLED',
    cancelled_at = coalesce(duplicate_charge.cancelled_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
FROM ranked_open_tuition AS ranked
WHERE duplicate_charge.id = ranked.id
  AND ranked.duplicate_rank > 1;

-- Índice para as consultas financeiras por estudante, situação e vencimento.
CREATE INDEX IF NOT EXISTS idx_charges_student_status_due_date
    ON charges (student_id, status, due_date);

-- Proteção estrutural: pode existir apenas uma propina aberta por estudante
-- e período. Cobranças pagas permanecem preservadas para histórico e recibos.
CREATE UNIQUE INDEX IF NOT EXISTS uk_charges_open_tuition_student_period
    ON charges (
        student_id,
        (EXTRACT(YEAR FROM due_date)),
        (EXTRACT(MONTH FROM due_date))
    )
    WHERE status IN ('PENDING', 'OVERDUE', 'PARTIALLY_PAID')
      AND (
            lower(coalesce(charge_code, '')) LIKE '%propina%'
            OR lower(coalesce(description, '')) LIKE '%propina%'
          );
