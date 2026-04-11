-- V8__create_jobs_and_files.sql

ALTER TABLE workspaces ADD COLUMN job_counter BIGINT NOT NULL DEFAULT 0;

CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id),
    client_id BIGINT NOT NULL REFERENCES clients(id),
    assigned_to_id BIGINT REFERENCES members(id),
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    job_number BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(32) NOT NULL DEFAULT 'NOVO',
    due_date DATE,
    briefing_data JSONB,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_jobs_workspace_number UNIQUE (workspace_id, job_number)
);

CREATE INDEX idx_jobs_workspace_id ON jobs(workspace_id);
CREATE INDEX idx_jobs_workspace_archived ON jobs(workspace_id, archived);
CREATE INDEX idx_jobs_client_id ON jobs(client_id);
CREATE INDEX idx_jobs_assigned_to ON jobs(assigned_to_id);
CREATE INDEX idx_jobs_status ON jobs(workspace_id, status);
CREATE INDEX idx_jobs_due_date ON jobs(workspace_id, due_date);

CREATE TABLE job_files (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_by_id BIGINT NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_files_job_id ON job_files(job_id);
