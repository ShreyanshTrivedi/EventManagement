-- =============================================================================
-- V12: Unified Resource Model + Waitlist System
-- =============================================================================
-- Changes:
--   1. Create `resources` table — single bookable resource (rooms, open spaces, labs…)
--   2. Migrate all existing `rooms` rows into `resources` (type = 'ROOM')
--   3. Add `resource_id` column to `bookings`, backfill from `room_id`
--   4. Drop room_id EXCLUDE constraint (added in V11), add resource_id EXCLUDE
--   5. Create `waitlist_entries` table for the queue-based waitlist system
-- =============================================================================

-- ─── 1. resources table ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.resources (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    resource_type VARCHAR(32)  NOT NULL DEFAULT 'ROOM',
    capacity      INT,
    building_id   BIGINT       REFERENCES public.buildings(id),
    floor_id      BIGINT       REFERENCES public.floors(id),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    amenities     TEXT,
    description   TEXT,
    -- Back-link to the original rooms row so we can join/backfill safely.
    -- Nullable: new OPEN_SPACE / LAB resources will not have a room counterpart.
    room_ref_id   BIGINT       REFERENCES public.rooms(id)
);

-- ─── 2. Migrate existing rooms → resources (type = 'ROOM') ───────────────────
INSERT INTO public.resources (name, resource_type, capacity, building_id, floor_id,
                               is_active, amenities, room_ref_id)
SELECT
    COALESCE(r.name, 'Room ' || r.room_number),
    'ROOM',
    r.capacity,
    f.building_id,
    r.floor_id,
    r.is_active,
    r.amenities,
    r.id
FROM   public.rooms r
JOIN   public.floors f ON f.id = r.floor_id
WHERE  NOT EXISTS (
    SELECT 1 FROM public.resources res WHERE res.room_ref_id = r.id
);

-- ─── 3. Add resource_id to bookings ──────────────────────────────────────────
ALTER TABLE public.bookings
    ADD COLUMN IF NOT EXISTS resource_id BIGINT REFERENCES public.resources(id);

-- ─── 4. Backfill resource_id from room_id (via room_ref_id link) ─────────────
UPDATE public.bookings b
SET    resource_id = res.id
FROM   public.resources res
WHERE  res.room_ref_id = b.room_id
  AND  b.resource_id IS NULL;

-- ─── 5. Drop old room_id EXCLUDE constraint (added in V11) ───────────────────
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE  conrelid = 'public.bookings'::regclass
          AND  contype  = 'x'
          AND  conname  = 'excl_bookings_room_no_overlap'
    ) THEN
        ALTER TABLE public.bookings
            DROP CONSTRAINT excl_bookings_room_no_overlap;
    END IF;
END $$;

-- ─── 6. New EXCLUDE constraint on resource_id ─────────────────────────────────
-- Guards all future bookings. WHERE clause excludes legacy rows with NULL.
CREATE EXTENSION IF NOT EXISTS btree_gist;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE  conrelid = 'public.bookings'::regclass
          AND  contype  = 'x'
          AND  conname  = 'excl_bookings_resource_no_overlap'
    ) THEN
        ALTER TABLE public.bookings
            ADD CONSTRAINT excl_bookings_resource_no_overlap
            EXCLUDE USING gist (
                resource_id                           WITH =,
                tsrange(start_time, end_time, '[)')   WITH &&
            )
            WHERE (resource_id IS NOT NULL);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_bookings_resource_time
    ON public.bookings (resource_id, start_time, end_time)
    WHERE resource_id IS NOT NULL;

-- ─── 7. waitlist_entries table ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.waitlist_entries (
    id           BIGSERIAL PRIMARY KEY,
    event_id     BIGINT    NOT NULL REFERENCES public.events(id) ON DELETE CASCADE,
    user_id      BIGINT    NOT NULL REFERENCES public.users(id)  ON DELETE CASCADE,
    queued_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    position     INT       NOT NULL,
    promoted_at  TIMESTAMP,
    CONSTRAINT uq_waitlist_event_user UNIQUE (event_id, user_id)
);

-- Ordered index so "select top-1 by position" is fast
CREATE INDEX IF NOT EXISTS idx_waitlist_event_position
    ON public.waitlist_entries (event_id, position ASC)
    WHERE promoted_at IS NULL;
