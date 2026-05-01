-- ─────────────────────────────────────────────────────────────────────────────
-- V1__init_schema.sql
-- Do Delivery – initial database schema
-- ─────────────────────────────────────────────────────────────────────────────

-- Enable pgcrypto for gen_random_uuid() on PostgreSQL < 13
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── Users ───────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    role       VARCHAR(20)  NOT NULL,                 -- CUSTOMER | RIDER
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_phone UNIQUE (phone)
);

-- ─── Rider Profiles ──────────────────────────────────────────────────────────
-- One-to-one with users; only rows for RIDER role are created.
CREATE TABLE rider_profiles (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    is_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    is_online   BOOLEAN      NOT NULL DEFAULT FALSE,
    rating      NUMERIC(3,2) NOT NULL DEFAULT 0.00,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_rider_profiles_user  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_rider_profiles_user  UNIQUE (user_id)
);

-- ─── Orders ──────────────────────────────────────────────────────────────────
CREATE TABLE orders (
    id          UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID             NOT NULL,
    rider_id    UUID,                                 -- NULL until accepted
    pickup_lat  DOUBLE PRECISION NOT NULL,
    pickup_lng  DOUBLE PRECISION NOT NULL,
    drop_lat    DOUBLE PRECISION NOT NULL,
    drop_lng    DOUBLE PRECISION NOT NULL,
    type        VARCHAR(20)      NOT NULL,            -- DOCUMENT | SMALL | PARCEL
    status      VARCHAR(20)      NOT NULL DEFAULT 'CREATED',
    price       NUMERIC(10,2)    NOT NULL,
    note        TEXT,
    created_at  TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_orders_rider    FOREIGN KEY (rider_id)    REFERENCES users(id)
);

-- ─── Indexes ─────────────────────────────────────────────────────────────────
CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_rider_id    ON orders (rider_id);
CREATE INDEX idx_orders_status      ON orders (status);
CREATE INDEX idx_orders_created_at  ON orders (created_at DESC);
