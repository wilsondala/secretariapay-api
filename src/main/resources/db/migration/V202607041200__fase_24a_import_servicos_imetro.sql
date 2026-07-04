create table if not exists institution_charge_service_imports (
    id uuid primary key,
    institution_id uuid not null,
    import_code varchar(80) not null unique,
    source_name varchar(180) not null,
    description text,
    currency varchar(10) not null default 'AOA',
    reported_grand_total numeric(18,2),
    source_report_period varchar(80),
    source_file_name varchar(180),
    created_at timestamp not null default now()
);

create table if not exists institution_charge_service_configs (
    id uuid primary key,
    institution_id uuid not null,
    import_id uuid references institution_charge_service_imports(id),
    service_code varchar(120) not null,
    service_name varchar(240) not null,
    service_category varchar(80) not null,
    charge_type varchar(120) not null,
    currency varchar(10) not null default 'AOA',
    total_reported_amount numeric(18,2) not null default 0,
    amount_semantics varchar(80) not null default 'AGGREGATED_REVENUE_TOTAL',
    can_generate_charge boolean not null default true,
    requires_dcr_validation boolean not null default true,
    active boolean not null default true,
    notes text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uq_institution_charge_service_configs unique (institution_id, service_code)
);

create index if not exists idx_charge_service_configs_institution on institution_charge_service_configs(institution_id);
create index if not exists idx_charge_service_configs_category on institution_charge_service_configs(service_category);
create index if not exists idx_charge_service_configs_charge_type on institution_charge_service_configs(charge_type);
create index if not exists idx_charge_service_configs_active on institution_charge_service_configs(active);

insert into institution_charge_service_imports (
    id,
    institution_id,
    import_code,
    source_name,
    description,
    currency,
    reported_grand_total,
    source_report_period,
    source_file_name,
    created_at
) values (
    '24aa0000-0000-4000-8000-000000000001',
    'c3726494-46b5-4563-8e84-0a04334fac8c',
    'IMETRO_SERVICOS_RECEITA_2026_07',
    'Relatório de serviços e totais IMETRO',
    'Importação preliminar dos serviços e totais agregados partilhados pelo cliente. Os valores representam totais em AKZ e não preços unitários definitivos.',
    'AOA',
    2187000000.00,
    'Relatório preliminar cliente',
    '93899.jpg',
    now()
)
on conflict (import_code) do update set
    reported_grand_total = excluded.reported_grand_total,
    description = excluded.description,
    source_file_name = excluded.source_file_name;

insert into institution_charge_service_configs (
    id,
    institution_id,
    import_id,
    service_code,
    service_name,
    service_category,
    charge_type,
    currency,
    total_reported_amount,
    amount_semantics,
    can_generate_charge,
    requires_dcr_validation,
    active,
    notes,
    created_at,
    updated_at
) values
('2c82234b-55e0-52e5-a847-ce809103e9d0', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'SEGUNDA_VIA_CARTAO_ESTUDANTE', '2ª Via do Cartão de Estudante', 'DOCUMENTOS', 'SEGUNDA_VIA_CARTAO_ESTUDANTE', 'AOA', 5334000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('9a45294f-52c2-5797-87ce-022dd2fd6d89', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'SEGUNDA_VIA_RECIBO_CONFIRMACAO_MATRICULA', '2ª Via Recibo de Confirmação e Matrícula', 'DOCUMENTOS', 'SEGUNDA_VIA_RECIBO_CONFIRMACAO_MATRICULA', 'AOA', 84000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('bda37576-5d1a-5870-8a0e-5387d75e2d0c', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'SEGUNDA_VIA_NADA_COSTA', '2º Nada Costa', 'DOCUMENTOS', 'NADA_CONSTA', 'AOA', 40000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('f4a66edf-a741-5f05-bd1b-c30e8bfdd5b6', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'ADEIMA', 'ADEIMA', 'OUTROS', 'ADEIMA', 'AOA', 5396000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('211b310b-dcdb-5aaa-9781-ac133509c757', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'AMORTIZACAO', 'AMORTIZACAO', 'FINANCEIRO', 'AMORTIZACAO', 'AOA', 170475880.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('ced43332-9d10-5c74-980f-ebbf826ae9a8', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'ANULACAO_CADEIRAS', 'Anulação de Cadeiras', 'ACADEMICO', 'ANULACAO_CADEIRAS', 'AOA', 430000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('0453d16c-b72e-5a12-acc2-66bdcdd4b5be', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'ANULACAO_MATRICULA', 'Anulação de Matricula', 'MATRICULA', 'ANULACAO_MATRICULA', 'AOA', 835300.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('ba7549d2-65df-5472-b463-9e4b3b93affb', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'CADEIRAS', 'CADEIRAS', 'ACADEMICO', 'CADEIRAS', 'AOA', 51755712.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('d5770845-0234-5ac4-ad3c-205f84106753', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'CONFIRMACAO_MATRICULA', 'CONFIRMAÇÃO DE MATRICULA', 'MATRICULA', 'CONFIRMACAO_MATRICULA', 'AOA', 119663234.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('fecc363f-afc2-5e14-ba7a-288bfad96c39', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'CONTEUDO_PROGRAMATICO', 'CONTEUDO PROGRAMATICO', 'DOCUMENTOS', 'CONTEUDO_PROGRAMATICO', 'AOA', 100000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('a2e03727-f750-5f24-a426-257d62548254', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'DECLARACAO_COM_NOTAS', 'DECLARAÇÃO COM NOTAS', 'DECLARACOES', 'DECLARACAO_COM_NOTAS', 'AOA', 622080.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('f6d1e5bf-e433-5490-8b6e-c942821606b5', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'DECLARACAO_COM_NOTAS_URGENTE', 'DECLARAÇÃO COM NOTAS(URGENTE)', 'DECLARACOES', 'DECLARACAO_COM_NOTAS_URGENTE', 'AOA', 72000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('fa867c0b-c4c0-5b3b-90d4-d65e3ff17f87', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'DECLARACAO_FREQUENCIA', 'DECLARAÇÃO DE FREQUÊNCIA', 'DECLARACOES', 'DECLARACAO_FREQUENCIA', 'AOA', 5477996.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('b3b56be2-b761-5ced-8ec6-32d3936e89ca', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'DECLARACAO_SEM_NOTAS', 'DECLARAÇÃO SEM NOTAS', 'DECLARACOES', 'DECLARACAO_SEM_NOTAS', 'AOA', 3997680.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('638dc8b7-2dce-58b4-8ef4-0d49c4c5a384', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'DIPLOMA_CERTIFICADO', 'DIPLOMA + CERTIFICADO', 'CERTIFICADOS', 'DIPLOMA_CERTIFICADO', 'AOA', 338400.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('cea31d70-66fc-5a5a-a394-a3d4ff4cff5d', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'ESTATUTO_ESTUDANTE_TRABALHADOR', 'ESTATUTO ESTUDANTE TRABALHADOR', 'DOCUMENTOS', 'ESTATUTO_ESTUDANTE_TRABALHADOR', 'AOA', 1850000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('06ced056-f09b-5b8c-84ad-192907e3cc41', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'EXAME_RECURSO', 'EXAME DE RECURSO', 'EXAMES', 'EXAME_RECURSO', 'AOA', 48582185.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('4e70e247-9e05-543b-84c5-ce446c60fa3d', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'EXAME_ESPECIAL', 'EXAME ESPECIAL', 'EXAMES', 'EXAME_ESPECIAL', 'AOA', 34200.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('6cf7e996-6cbc-566d-bfdc-a6cd8529bf2a', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MONOGRAFIA', 'MONOGRAFIA', 'TFC_MONOGRAFIA', 'MONOGRAFIA', 'AOA', 35640000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('f6dd3423-be75-50af-9a8f-27f01b3de56a', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MONOGRAFIA_1_PARCELA', 'MONOGRAFIA 1ª PARCELA', 'TFC_MONOGRAFIA', 'MONOGRAFIA_1_PARCELA', 'AOA', 47767000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('8cc4a1db-8e94-553f-a252-b10fd6cfd316', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MONOGRAFIA_2_PARCELA', 'MONOGRAFIA 2ª PARCELA', 'TFC_MONOGRAFIA', 'MONOGRAFIA_2_PARCELA', 'AOA', 2700000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('84ea7a2c-8889-5ba4-8486-82cebee76d56', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MONOGRAFIA_ATRELADO_ESTAGIO_1_PRESTACAO', 'Monografia Atrelado ao Estágio 1º Prestação', 'TFC_MONOGRAFIA', 'MONOGRAFIA_ATRELADO_ESTAGIO_1_PRESTACAO', 'AOA', 450000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('a1281afd-371e-5636-bd3f-b3e19c0eb3f9', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MUDANCA_CURSO', 'MUDANÇA DE CURSO', 'ACADEMICO', 'MUDANCA_CURSO', 'AOA', 823960.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('4a89d4cb-533d-5116-8426-7dbf719233ac', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MUDANCA_TURNO', 'MUDANÇA DE TURNO', 'ACADEMICO', 'MUDANCA_TURNO', 'AOA', 1443312.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('0a4beeeb-c65c-5e7d-98f3-2c310c76d884', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MULTA', 'MULTA', 'MULTAS', 'MULTA', 'AOA', 7619101.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('e2b14344-ea8e-55ea-841e-9d791451d7b0', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MULTA_CADEIRA_10', 'Multa Cadeira 10%', 'MULTAS', 'MULTA_CADEIRA_10', 'AOA', 428189.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('093b5688-67fe-57e0-89f6-e68748ff0958', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MULTA_CADEIRA_20', 'Multa Cadeira 20%', 'MULTAS', 'MULTA_CADEIRA_20', 'AOA', 760698.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('73ef54ca-dc40-51f7-8d23-9dc5de796f3e', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MULTA_CADEIRA_30', 'Multa Cadeira 30%', 'MULTAS', 'MULTA_CADEIRA_30', 'AOA', 3839254.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('6c2f0ed2-ef9a-5f71-8440-e9b7d41120af', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MULTA_CONFIRMACAO', 'MULTA CONFIRMACAO', 'MULTAS', 'MULTA_CONFIRMACAO', 'AOA', 14296804.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('9147f76f-b742-58d8-ba2a-611c030f0031', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MULTA_CONFIRMACAO_MATRICULA', 'Multa da Confirmacao de Matricula', 'MULTAS', 'MULTA_CONFIRMACAO_MATRICULA', 'AOA', 93702.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('8d2b9ee8-cde1-5bd5-aa9c-3818283a8a99', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MULTA_BORDEREAUX', 'MULTA DE BORDEREAUX', 'MULTAS', 'MULTA_BORDEREAUX', 'AOA', 266400.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('0ca853f9-efca-5af0-bb94-f55c80a10d03', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MULTA_PROPINA_10', 'Multa de Propina 10%', 'MULTAS', 'MULTA_PROPINA_10', 'AOA', 10577291.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('0e554917-0e23-5659-8809-4187c96fa88a', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MULTA_PROPINA_20', 'Multa de Propina 20%', 'MULTAS', 'MULTA_PROPINA_20', 'AOA', 19818823.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('4869e818-3a66-5443-a2bf-dab8e382021f', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'MULTA_PROPINA_30', 'Multa de Propina 30%', 'MULTAS', 'MULTA_PROPINA_30', 'AOA', 68624661.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('1bd24caf-a044-5405-ad79-634f321c9647', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'PROPINA', 'PROPINA', 'PROPINA', 'PROPINA', 'AOA', 1554000000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('714f37dc-9a96-587f-894e-6e4262f87f21', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'READMISSAO', 'READMISSAO', 'ACADEMICO', 'READMISSAO', 'AOA', 20000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('85590e03-368a-5272-89e8-d117d2862bca', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'REEMATRICULA_1', 'Reematricula 1', 'MATRICULA', 'REEMATRICULA_1', 'AOA', 585000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('d9e216d8-3ebb-5be4-b39c-5a462952de9c', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'REEMATRICULA_2', 'Reematricula 2', 'MATRICULA', 'REEMATRICULA_2', 'AOA', 1970000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('bed1a8a0-ff57-56fa-bcc4-5f608b6f8a2c', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'REVISAO_PROVA', 'Revisao de Prova', 'EXAMES', 'REVISAO_PROVA', 'AOA', 98835.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('5fbdb8a8-338e-54b1-92b9-46f728079054', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'TRANSFERENCIA', 'TRANSFERÊNCIA', 'ACADEMICO', 'TRANSFERENCIA', 'AOA', 216000.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now()),
('94f61edb-d8fd-55c3-aa79-f19806a2351b', 'c3726494-46b5-4563-8e84-0a04334fac8c', '24aa0000-0000-4000-8000-000000000001', 'UTILIZACAO_2_VIA_BORDEREOUX_ACTUAL', 'UTILIZAÇÃO DE 2° VIA DO BORDEREOUX ACTUAL', 'DOCUMENTOS', 'UTILIZACAO_2_VIA_BORDEREOUX_ACTUAL', 'AOA', 43200.00, 'AGGREGATED_REVENUE_TOTAL', true, true, true, 'Valor importado do relatório do cliente. Campo total_reported_amount representa total agregado em AKZ, não preço unitário definitivo.', now(), now())
on conflict (institution_id, service_code) do update set
    import_id = excluded.import_id,
    service_name = excluded.service_name,
    service_category = excluded.service_category,
    charge_type = excluded.charge_type,
    currency = excluded.currency,
    total_reported_amount = excluded.total_reported_amount,
    amount_semantics = excluded.amount_semantics,
    can_generate_charge = excluded.can_generate_charge,
    requires_dcr_validation = excluded.requires_dcr_validation,
    active = excluded.active,
    notes = excluded.notes,
    updated_at = now();
