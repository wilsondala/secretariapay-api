create table if not exists academic_requests (
    id uuid primary key,
    request_code varchar(60) not null unique,
    institution_id uuid,
    student_id uuid not null,
    student_name varchar(180) not null,
    student_number varchar(80),
    request_type varchar(60) not null,
    subject varchar(180) not null,
    description text,
    status varchar(40) not null,
    priority varchar(30) not null,
    requester_phone varchar(40),
    assigned_to varchar(120),
    review_note text,
    completed_note text,
    requested_at timestamp not null,
    reviewed_at timestamp,
    completed_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists academic_restrictions (
    id uuid primary key,
    restriction_code varchar(60) not null unique,
    institution_id uuid,
    student_id uuid not null,
    student_name varchar(180) not null,
    student_number varchar(80),
    restriction_type varchar(60) not null,
    reason varchar(180) not null,
    description text,
    status varchar(40) not null,
    source varchar(60) not null,
    related_charge_id uuid,
    related_charge_code varchar(80),
    applied_by varchar(120),
    released_by varchar(120),
    release_note text,
    applied_at timestamp not null,
    released_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists billing_campaigns (
    id uuid primary key,
    campaign_code varchar(60) not null unique,
    institution_id uuid,
    name varchar(180) not null,
    campaign_type varchar(60) not null,
    audience varchar(80) not null,
    channel varchar(40) not null,
    status varchar(40) not null,
    title varchar(180),
    message_template text not null,
    scheduled_for timestamp,
    created_by varchar(120),
    total_recipients integer not null default 0,
    total_generated integer not null default 0,
    total_sent integer not null default 0,
    total_failed integer not null default 0,
    created_at timestamp not null,
    updated_at timestamp not null,
    activated_at timestamp,
    completed_at timestamp
);

create table if not exists billing_campaign_messages (
    id uuid primary key,
    campaign_id uuid not null references billing_campaigns(id) on delete cascade,
    student_id uuid,
    student_name varchar(180),
    student_number varchar(80),
    recipient_phone varchar(40) not null,
    charge_id uuid,
    charge_code varchar(80),
    message text not null,
    status varchar(40) not null,
    provider_message_id varchar(160),
    failure_reason text,
    created_at timestamp not null,
    sent_at timestamp
);

create index if not exists idx_academic_requests_student_id on academic_requests(student_id);
create index if not exists idx_academic_requests_status on academic_requests(status);
create index if not exists idx_academic_restrictions_student_id on academic_restrictions(student_id);
create index if not exists idx_academic_restrictions_status on academic_restrictions(status);
create index if not exists idx_billing_campaigns_status on billing_campaigns(status);
create index if not exists idx_billing_campaign_messages_campaign_id on billing_campaign_messages(campaign_id);
