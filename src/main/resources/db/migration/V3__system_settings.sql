-- ─────────────────────────────────────────────────────────────────────────────
-- V3__system_settings.sql
-- Add system settings table used by admin configuration endpoints
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS system_settings (
    setting_key   VARCHAR(255) PRIMARY KEY,
    setting_value TEXT,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
