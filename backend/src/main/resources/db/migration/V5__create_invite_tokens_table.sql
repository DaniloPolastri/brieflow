CREATE TABLE invite_tokens (
    id              BIGSERIAL PRIMARY KEY,
    workspace_id    BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    position        VARCHAR(30) NOT NULL,
    token           VARCHAR(255) NOT NULL UNIQUE,
    invited_by      BIGINT NOT NULL REFERENCES users(id),
    expires_at      TIMESTAMP NOT NULL,
    used            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invite_tokens_token ON invite_tokens(token);
CREATE INDEX idx_invite_tokens_workspace_id ON invite_tokens(workspace_id);
