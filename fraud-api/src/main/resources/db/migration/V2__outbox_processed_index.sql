-- Missing index for the outbox cleanup query (which filters on status + processed_at).
-- The existing idx_outbox_status_created (status, created_at) does not cover that query;
-- as the table grows, deleteProcessedBefore() would degrade into a full table scan.
CREATE INDEX idx_outbox_status_processed ON outbox_messages (status, processed_at);
