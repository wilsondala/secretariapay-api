create table if not exists academic_student_import_batches (
    id uuid primary key,
    institution_id uuid not null,
    import_code varchar(80) not null unique,
    source_system varchar(80) not null,
    source_name varchar(180),
    file_name varchar(180),
    academic_year varchar(20),
    semester varchar(20),
    status varchar(40) not null,
    total_rows integer not null default 0,
    valid_rows integer not null default 0,
    invalid_rows integer not null default 0,
    imported_rows integer not null default 0,
    notes text,
    created_by varchar(120),
    created_at timestamp not null,
    updated_at timestamp not null,
    validated_at timestamp,
    completed_at timestamp
);

create table if not exists academic_student_import_rows (
    id uuid primary key,
    batch_id uuid not null references academic_student_import_batches(id) on delete cascade,
    institution_id uuid not null,
    row_number integer,
    academic_year varchar(20),
    semester_number integer,
    student_number varchar(80),
    full_name varchar(220),
    course_name varchar(220),
    class_name varchar(80),
    shift_name varchar(80),
    department_name varchar(160),
    email varchar(160),
    phone varchar(60),
    whatsapp varchar(60),
    responsible_name varchar(180),
    responsible_phone varchar(60),
    responsible_email varchar(160),
    source_action varchar(80),
    status varchar(40) not null,
    validation_message text,
    matched_student_id uuid,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index if not exists idx_academic_student_import_batches_institution on academic_student_import_batches(institution_id);
create index if not exists idx_academic_student_import_batches_status on academic_student_import_batches(status);
create index if not exists idx_academic_student_import_rows_batch on academic_student_import_rows(batch_id);
create index if not exists idx_academic_student_import_rows_student_number on academic_student_import_rows(student_number);
create index if not exists idx_academic_student_import_rows_status on academic_student_import_rows(status);
