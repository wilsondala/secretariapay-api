
create table if not exists support_tickets (
    id uuid primary key,
    protocol_code varchar(60) not null unique,
    student_id uuid,
    student_name varchar(180),
    student_number varchar(80),
    requester_phone varchar(40),
    subject varchar(160) not null,
    reason varchar(120),
    description text,
    priority varchar(30) not null default 'NORMAL',
    status varchar(30) not null default 'OPEN',
    assigned_to varchar(120),
    created_at timestamp not null,
    updated_at timestamp not null,
    closed_at timestamp
);
create index if not exists idx_support_tickets_student_id on support_tickets(student_id);
create index if not exists idx_support_tickets_status on support_tickets(status);

create table if not exists support_ticket_messages (
    id uuid primary key,
    ticket_id uuid not null references support_tickets(id) on delete cascade,
    sender_type varchar(30) not null,
    sender_name varchar(140),
    message text not null,
    created_at timestamp not null
);
create index if not exists idx_support_ticket_messages_ticket on support_ticket_messages(ticket_id);

create table if not exists financial_negotiations (
    id uuid primary key,
    negotiation_code varchar(60) not null unique,
    student_id uuid,
    student_name varchar(180),
    student_number varchar(80),
    charge_id uuid,
    charge_code varchar(80),
    total_amount numeric(14,2) not null default 0,
    currency varchar(10) not null default 'AOA',
    proposal_type varchar(40) not null default 'INSTALLMENT',
    installments integer not null default 1,
    first_due_date date,
    status varchar(30) not null default 'REQUESTED',
    request_note text,
    review_note text,
    created_at timestamp not null,
    updated_at timestamp not null,
    reviewed_at timestamp
);
create index if not exists idx_financial_negotiations_student_id on financial_negotiations(student_id);
create index if not exists idx_financial_negotiations_status on financial_negotiations(status);

create table if not exists financial_negotiation_installments (
    id uuid primary key,
    negotiation_id uuid not null references financial_negotiations(id) on delete cascade,
    installment_number integer not null,
    amount numeric(14,2) not null,
    currency varchar(10) not null default 'AOA',
    due_date date,
    status varchar(30) not null default 'PENDING',
    charge_id uuid,
    created_at timestamp not null,
    updated_at timestamp not null
);
create index if not exists idx_financial_negotiation_installments_negotiation on financial_negotiation_installments(negotiation_id);

create table if not exists payment_method_configs (
    id uuid primary key,
    institution_id uuid,
    method_code varchar(80) not null,
    method_name varchar(140) not null,
    description text,
    active boolean not null default true,
    instructions text,
    created_at timestamp not null,
    updated_at timestamp not null
);
create index if not exists idx_payment_method_configs_active on payment_method_configs(active);

create table if not exists bank_account_configs (
    id uuid primary key,
    institution_id uuid,
    holder_name varchar(180) not null,
    bank_name varchar(180) not null,
    account_number varchar(80),
    iban varchar(120),
    currency varchar(10) not null default 'AOA',
    active boolean not null default true,
    notes text,
    created_at timestamp not null,
    updated_at timestamp not null
);
create index if not exists idx_bank_account_configs_active on bank_account_configs(active);

create table if not exists billing_rules (
    id uuid primary key,
    institution_id uuid,
    rule_name varchar(140) not null,
    charge_type varchar(80) not null default 'PROPINA',
    due_day integer not null default 10,
    grace_days integer not null default 0,
    fine_type varchar(30) not null default 'NONE',
    fine_value numeric(14,2) not null default 0,
    interest_type varchar(30) not null default 'NONE',
    daily_interest_value numeric(14,2) not null default 0,
    discount_type varchar(30) not null default 'NONE',
    discount_value numeric(14,2) not null default 0,
    active boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);
create index if not exists idx_billing_rules_active on billing_rules(active);
