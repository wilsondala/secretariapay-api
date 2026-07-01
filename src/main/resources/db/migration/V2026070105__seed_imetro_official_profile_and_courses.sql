-- SecretáriaPay Académico - Perfil oficial IMETRO e catálogo inicial de cursos
-- Dados informados a partir do site oficial do IMETRO e validação do projeto.

UPDATE institutions
SET
    name = 'Instituto Superior Politécnico Metropolitano de Angola',
    legal_name = 'Instituto Superior Politécnico Metropolitano de Angola (IMETRO)',
    address = 'Avenida 21 de Janeiro, Travessa da Talatona S/N, bairro Morro Bento, município de Belas, Luanda, Angola',
    updated_at = NOW()
WHERE id = 'c3726494-46b5-4563-8e84-0a04334fac8c';

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1001-4000-8000-000000000001'::uuid, i.id, 'Electrónica e Telecomunicações', 'ELEC-TELECOM', 'Engenharia e Tecnologia', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'ELEC-TELECOM');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1002-4000-8000-000000000002'::uuid, i.id, 'Ciências da Computação', 'CC', 'Engenharia e Tecnologia', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'CC');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1003-4000-8000-000000000003'::uuid, i.id, 'Planeamento Regional e Urbano', 'PRU', 'Arquitectura, Urbanismo e Território', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'PRU');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1004-4000-8000-000000000004'::uuid, i.id, 'Arquitectura', 'ARQ', 'Arquitectura, Urbanismo e Território', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'ARQ');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1005-4000-8000-000000000005'::uuid, i.id, 'Engenharia Civil', 'ENG-CIVIL', 'Engenharia e Tecnologia', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'ENG-CIVIL');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1006-4000-8000-000000000006'::uuid, i.id, 'Geologia e Minas', 'GEO-MINAS', 'Engenharia e Tecnologia', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'GEO-MINAS');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1007-4000-8000-000000000007'::uuid, i.id, 'Jornalismo', 'JOR', 'Comunicação, Artes e Humanidades', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'JOR');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1008-4000-8000-000000000008'::uuid, i.id, 'Cinema e TV', 'CTV', 'Comunicação, Artes e Humanidades', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'CTV');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1009-4000-8000-000000000009'::uuid, i.id, 'Direito', 'DIR', 'Ciências Jurídicas', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'DIR');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1010-4000-8000-000000000010'::uuid, i.id, 'Economia', 'ECO', 'Economia, Gestão e Administração', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'ECO');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1011-4000-8000-000000000011'::uuid, i.id, 'Gestão de Recursos Humanos', 'GRH', 'Economia, Gestão e Administração', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'GRH');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1012-4000-8000-000000000012'::uuid, i.id, 'Gestão Pública', 'GPUB', 'Economia, Gestão e Administração', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'GPUB');

INSERT INTO courses (id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at)
SELECT '11111111-1013-4000-8000-000000000013'::uuid, i.id, 'Administração de Empresas', 'ADM-EMP', 'Economia, Gestão e Administração', NULL, TRUE, NOW(), NOW()
FROM institutions i
WHERE i.id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.institution_id = i.id AND c.code = 'ADM-EMP');
