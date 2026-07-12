CREATE TABLE IF NOT EXISTS academic_service_catalog (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    category VARCHAR(40) NOT NULL,
    unit_price NUMERIC(14,2),
    historical_total NUMERIC(18,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'AOA',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    generates_guide BOOLEAN NOT NULL DEFAULT TRUE,
    generates_receipt BOOLEAN NOT NULL DEFAULT TRUE,
    allows_discount BOOLEAN NOT NULL DEFAULT FALSE,
    allows_penalty BOOLEAN NOT NULL DEFAULT FALSE,
    available_whatsapp BOOLEAN NOT NULL DEFAULT TRUE,
    available_portal BOOLEAN NOT NULL DEFAULT TRUE,
    available_panel BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    source_reference VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_academic_service_catalog_category
    ON academic_service_catalog(category);
CREATE INDEX IF NOT EXISTS idx_academic_service_catalog_active
    ON academic_service_catalog(active);

-- O ficheiro relatPagamentos.xls contém totais históricos arrecadados por serviço,
-- não preços unitários. Por segurança, unit_price permanece NULL até validação
-- formal da tabela de preços pelo IMETRO.
INSERT INTO academic_service_catalog
(id, code, name, category, unit_price, historical_total, active, generates_guide, generates_receipt,
 allows_discount, allows_penalty, available_whatsapp, available_portal, available_panel, display_order, source_reference)
VALUES
(gen_random_uuid(),'SECOND_STUDENT_CARD','2ª Via do Cartão de Estudante','DOCUMENT',NULL,5334000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,10,'relatPagamentos.xls'),
(gen_random_uuid(),'SECOND_CONFIRMATION_RECEIPT','2ª Via Recibo de Confirmação e Matrícula','DOCUMENT',NULL,84000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,20,'relatPagamentos.xls'),
(gen_random_uuid(),'SECOND_NADA_COSTA','2º Nada Costa','OTHER',NULL,40000.00,FALSE,TRUE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,30,'relatPagamentos.xls'),
(gen_random_uuid(),'ADEIMA','ADEIMA','OTHER',NULL,5396000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,40,'relatPagamentos.xls'),
(gen_random_uuid(),'AMORTIZATION','Amortização','FINANCIAL',NULL,170475880.00,TRUE,TRUE,TRUE,TRUE,FALSE,TRUE,TRUE,TRUE,50,'relatPagamentos.xls'),
(gen_random_uuid(),'SUBJECT_CANCELLATION','Anulação de Cadeiras','ACADEMIC',NULL,430000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,60,'relatPagamentos.xls'),
(gen_random_uuid(),'ENROLLMENT_CANCELLATION','Anulação de Matrícula','ACADEMIC',NULL,835300.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,70,'relatPagamentos.xls'),
(gen_random_uuid(),'SUBJECTS','Cadeiras','ACADEMIC',NULL,51755712.00,TRUE,TRUE,TRUE,FALSE,TRUE,TRUE,TRUE,TRUE,80,'relatPagamentos.xls'),
(gen_random_uuid(),'ENROLLMENT_CONFIRMATION','Confirmação de Matrícula','ENROLLMENT',NULL,119663234.00,TRUE,TRUE,TRUE,FALSE,TRUE,TRUE,TRUE,TRUE,90,'relatPagamentos.xls'),
(gen_random_uuid(),'PROGRAM_CONTENT','Conteúdo Programático','DOCUMENT',NULL,100000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,100,'relatPagamentos.xls'),
(gen_random_uuid(),'DECLARATION_WITH_GRADES','Declaração com Notas','DOCUMENT',NULL,622080.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,110,'relatPagamentos.xls'),
(gen_random_uuid(),'URGENT_DECLARATION_WITH_GRADES','Declaração com Notas (Urgente)','DOCUMENT',NULL,72000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,120,'relatPagamentos.xls'),
(gen_random_uuid(),'ATTENDANCE_DECLARATION','Declaração de Frequência','DOCUMENT',NULL,5477996.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,130,'relatPagamentos.xls'),
(gen_random_uuid(),'DECLARATION_WITHOUT_GRADES','Declaração sem Notas','DOCUMENT',NULL,3997680.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,140,'relatPagamentos.xls'),
(gen_random_uuid(),'DIPLOMA_CERTIFICATE','Diploma + Certificado','DOCUMENT',NULL,338400.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,150,'relatPagamentos.xls'),
(gen_random_uuid(),'WORKER_STUDENT_STATUS','Estatuto Estudante Trabalhador','ACADEMIC',NULL,1850000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,160,'relatPagamentos.xls'),
(gen_random_uuid(),'RESIT_EXAM','Exame de Recurso','EXAM',NULL,48582185.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,170,'relatPagamentos.xls'),
(gen_random_uuid(),'SPECIAL_EXAM','Exame Especial','EXAM',NULL,34200.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,180,'relatPagamentos.xls'),
(gen_random_uuid(),'MONOGRAPH','Monografia','ACADEMIC',NULL,35640000.00,TRUE,TRUE,TRUE,TRUE,FALSE,TRUE,TRUE,TRUE,190,'relatPagamentos.xls'),
(gen_random_uuid(),'MONOGRAPH_INSTALLMENT_1','Monografia 1ª Parcela','ACADEMIC',NULL,47767000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,200,'relatPagamentos.xls'),
(gen_random_uuid(),'MONOGRAPH_INSTALLMENT_2','Monografia 2ª Parcela','ACADEMIC',NULL,2700000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,210,'relatPagamentos.xls'),
(gen_random_uuid(),'MONOGRAPH_INTERNSHIP_INSTALLMENT_1','Monografia Atrelada ao Estágio — 1ª Prestação','ACADEMIC',NULL,450000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,220,'relatPagamentos.xls'),
(gen_random_uuid(),'COURSE_CHANGE','Mudança de Curso','ACADEMIC',NULL,823960.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,230,'relatPagamentos.xls'),
(gen_random_uuid(),'SHIFT_CHANGE','Mudança de Turno','ACADEMIC',NULL,1443312.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,240,'relatPagamentos.xls'),
(gen_random_uuid(),'GENERIC_FINE','Multa','PENALTY',NULL,7619101.00,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,250,'relatPagamentos.xls'),
(gen_random_uuid(),'SUBJECT_FINE_10','Multa Cadeira 10%','PENALTY',NULL,428189.00,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,260,'relatPagamentos.xls'),
(gen_random_uuid(),'SUBJECT_FINE_20','Multa Cadeira 20%','PENALTY',NULL,760698.00,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,270,'relatPagamentos.xls'),
(gen_random_uuid(),'SUBJECT_FINE_30','Multa Cadeira 30%','PENALTY',NULL,3839254.00,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,280,'relatPagamentos.xls'),
(gen_random_uuid(),'CONFIRMATION_FINE','Multa de Confirmação','PENALTY',NULL,14296804.00,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,290,'relatPagamentos.xls'),
(gen_random_uuid(),'ENROLLMENT_CONFIRMATION_FINE','Multa da Confirmação de Matrícula','PENALTY',NULL,93702.00,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,300,'relatPagamentos.xls'),
(gen_random_uuid(),'BORDERAUX_FINE','Multa de Borderô','PENALTY',NULL,266400.00,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,310,'relatPagamentos.xls'),
(gen_random_uuid(),'TUITION_FINE_10','Multa de Propina 10%','PENALTY',NULL,10577291.00,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,320,'relatPagamentos.xls'),
(gen_random_uuid(),'TUITION_FINE_20','Multa de Propina 20%','PENALTY',NULL,19818823.00,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,330,'relatPagamentos.xls'),
(gen_random_uuid(),'TUITION_FINE_30','Multa de Propina 30%','PENALTY',NULL,68624661.00,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,340,'relatPagamentos.xls'),
(gen_random_uuid(),'TUITION','Propina','TUITION',NULL,1553806783.00,TRUE,TRUE,TRUE,TRUE,TRUE,TRUE,TRUE,TRUE,350,'relatPagamentos.xls'),
(gen_random_uuid(),'READMISSION','Readmissão','ENROLLMENT',NULL,20000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,360,'relatPagamentos.xls'),
(gen_random_uuid(),'REENROLLMENT_1','Reematrícula 1','ENROLLMENT',NULL,585000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,370,'relatPagamentos.xls'),
(gen_random_uuid(),'REENROLLMENT_2','Reematrícula 2','ENROLLMENT',NULL,1970000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,380,'relatPagamentos.xls'),
(gen_random_uuid(),'EXAM_REVIEW','Revisão de Prova','EXAM',NULL,98835.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,390,'relatPagamentos.xls'),
(gen_random_uuid(),'TRANSFER','Transferência','ACADEMIC',NULL,216000.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,400,'relatPagamentos.xls'),
(gen_random_uuid(),'CURRENT_BORDERAUX_SECOND_COPY','Utilização de 2ª Via do Borderô Atual','DOCUMENT',NULL,43200.00,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,410,'relatPagamentos.xls')
ON CONFLICT (code) DO NOTHING;
