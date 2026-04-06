CREATE TABLE members (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workspace_id    BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    position        VARCHAR(30) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_members_user_workspace UNIQUE (user_id, workspace_id)
);

CREATE INDEX idx_members_workspace_id ON members(workspace_id);
CREATE INDEX idx_members_user_id ON members(user_id);
