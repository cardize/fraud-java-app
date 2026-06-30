-- Outbox temizlik sorgusu (status + processed_at filtreler) için eksik index.
-- Mevcut idx_outbox_status_created (status, created_at) bu sorguyu karşılamıyor;
-- tablo büyüdükçe deleteProcessedBefore() full-table-scan'e döner.
CREATE INDEX idx_outbox_status_processed ON outbox_messages (status, processed_at);
