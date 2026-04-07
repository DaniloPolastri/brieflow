CREATE TABLE client_members (
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (client_id, member_id)
);

CREATE INDEX idx_client_members_member_id ON client_members(member_id);
