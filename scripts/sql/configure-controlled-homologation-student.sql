\set ON_ERROR_STOP on

\echo '=== SecretáriaPay: configuração do estudante controlado de homologação ==='

BEGIN;

DO $$
DECLARE
    target_count INTEGER;
    whatsapp_value TEXT;
BEGIN
    SELECT COUNT(*), MAX(whatsapp)
      INTO target_count, whatsapp_value
      FROM students
     WHERE student_number = '202301404'
       AND full_name = 'Wilson dos Santos Kahango Dala';

    IF target_count <> 1 THEN
        RAISE EXCEPTION 'Esperado exatamente um estudante 202301404 / Wilson dos Santos Kahango Dala; encontrado(s): %', target_count;
    END IF;

    IF whatsapp_value IS NULL OR BTRIM(whatsapp_value) = '' THEN
        RAISE EXCEPTION 'O estudante controlado de homologação não possui WhatsApp cadastrado.';
    END IF;
END $$;

UPDATE students
   SET email = 'dalakahango@hotmail.com',
       updated_at = CURRENT_TIMESTAMP
 WHERE student_number = '202301404'
   AND full_name = 'Wilson dos Santos Kahango Dala';

COMMIT;

SELECT
    student_number,
    full_name,
    email,
    CASE
        WHEN whatsapp IS NULL OR BTRIM(whatsapp) = '' THEN 'NAO_CONFIGURADO'
        ELSE 'CONFIGURADO'
    END AS whatsapp_status,
    status
FROM students
WHERE student_number = '202301404';

\echo 'Estudante controlado de homologação configurado com sucesso.'
