\set ON_ERROR_STOP on

\echo '=== SecretáriaPay: configuração do estudante controlado de homologação ==='

BEGIN;

DO $$
DECLARE
    target_count INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO target_count
      FROM students
     WHERE student_number = '202301404'
       AND full_name = 'Wilson dos Santos Kahango Dala';

    IF target_count <> 1 THEN
        RAISE EXCEPTION 'Esperado exatamente um estudante 202301404 / Wilson dos Santos Kahango Dala; encontrado(s): %', target_count;
    END IF;
END $$;

UPDATE students
   SET email = 'dalakahango@hotmail.com',
       phone = '+5511915102566',
       whatsapp = '+5511915102566',
       updated_at = CURRENT_TIMESTAMP
 WHERE student_number = '202301404'
   AND full_name = 'Wilson dos Santos Kahango Dala';

COMMIT;

SELECT
    student_number,
    full_name,
    email,
    phone,
    whatsapp,
    status
FROM students
WHERE student_number = '202301404';

\echo 'Estudante controlado de homologação configurado com sucesso.'
