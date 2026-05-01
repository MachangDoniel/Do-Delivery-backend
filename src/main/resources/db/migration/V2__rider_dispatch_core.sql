-- ─────────────────────────────────────────────────────────────────────────────
-- V2__rider_dispatch_core.sql
-- Add rider location + dispatch timeline columns for assignment engine
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE rider_profiles
    ADD COLUMN IF NOT EXISTS current_lat DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS current_lng DOUBLE PRECISION;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS picked_up_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_rider_profiles_online ON rider_profiles (is_online);
CREATE INDEX IF NOT EXISTS idx_orders_assigned_at ON orders (assigned_at DESC);
