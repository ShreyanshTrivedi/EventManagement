-- =============================================================================
-- V11: Safety Constraints & Event State Machine
-- =============================================================================
-- Changes:
--   1. Add event status column (DRAFT|PENDING|APPROVED|COMPLETED)
--   2. Add UNIQUE(event_id, user_id) to event_registrations (idempotent)
--   3. Add UNIQUE(event_id, email) to registrations (idempotent)
--   4. Add PostgreSQL EXCLUSION constraint on bookings to prevent
--      overlapping intervals at DB level — replaces weaker Java-only check.
-- =============================================================================

-- ─── 1. Event status column ───────────────────────────────────────────────────
ALTER TABLE public.events
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'PENDING';

-- Backfill: any event that already has an APPROVED room booking goes APPROVED
UPDATE public.events e
SET status = 'APPROVED'
WHERE EXISTS (
    SELECT 1 FROM public.room_booking_requests rbr
    WHERE rbr.event_id = e.id
      AND rbr.status IN ('APPROVED', 'CONFIRMED')
);

-- Backfill: any event whose end_time is in the past and is APPROVED → COMPLETED
UPDATE public.events
SET status = 'COMPLETED'
WHERE status = 'APPROVED'
  AND end_time < NOW();

-- ─── 2. event_registrations unique constraint (event_id, user_id) ────────────
-- The entity already declares this uniqueConstraint, but V1 migration never
-- created it at DB level.  Add it safely using IF NOT EXISTS via DO block.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.event_registrations'::regclass
          AND contype = 'u'
          AND conname = 'uq_event_registrations_event_user'
    ) THEN
        ALTER TABLE public.event_registrations
            ADD CONSTRAINT uq_event_registrations_event_user
            UNIQUE (event_id, user_id);
    END IF;
END $$;

-- ─── 3. registrations unique constraint (event_id, email) ────────────────────
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.registrations'::regclass
          AND contype = 'u'
          AND conname = 'uq_registrations_event_email'
    ) THEN
        ALTER TABLE public.registrations
            ADD CONSTRAINT uq_registrations_event_email
            UNIQUE (event_id, email);
    END IF;
END $$;

-- ─── 4. bookings overlap exclusion constraint ─────────────────────────────────
-- Requires btree_gist extension (ships with standard PostgreSQL ≥ 9.4).
-- This prevents two bookings for the same room whose time ranges overlap.
CREATE EXTENSION IF NOT EXISTS btree_gist;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.bookings'::regclass
          AND contype = 'x'
          AND conname = 'excl_bookings_room_no_overlap'
    ) THEN
        ALTER TABLE public.bookings
            ADD CONSTRAINT excl_bookings_room_no_overlap
            EXCLUDE USING gist (
                room_id   WITH =,
                tsrange(start_time, end_time, '[)') WITH &&
            );
    END IF;
END $$;

-- Supplemental index to speed up availability queries by room
CREATE INDEX IF NOT EXISTS idx_bookings_room_time
    ON public.bookings (room_id, start_time, end_time);

-- Index to speed "find APPROVED events" queries
CREATE INDEX IF NOT EXISTS idx_events_status
    ON public.events (status);
