-- Retention support (DataRetentionJob).

-- The retention delete filters on transaction_date; without an index it would degrade into a
-- full table scan as the table grows.
CREATE INDEX idx_tx_transaction_date ON transactions (transaction_date);

-- message_claims also grows with every distinct message and needs retention. claimed_at records
-- when the claim was made. NOTE: deleting a claim re-opens the duplicate-detection window for
-- that message id — the claim retention period must therefore stay LONGER than any realistic
-- upstream retry window.
ALTER TABLE message_claims ADD COLUMN claimed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
CREATE INDEX idx_claims_claimed_at ON message_claims (claimed_at);
