ALTER TABLE journal_entries
    ADD COLUMN deleted_at TIMESTAMPTZ NULL,
    ADD COLUMN deleted_by UUID NULL;

ALTER TABLE journal_entries
    ADD CONSTRAINT fk_journal_entries_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES users(id);

CREATE INDEX idx_journal_entries_user_deleted_created
    ON journal_entries (user_id, deleted_at, created_at DESC);
