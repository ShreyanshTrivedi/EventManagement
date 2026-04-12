-- =============================================================================
-- V13: Complete Resource Model Migration & Claim-based Approvals
-- =============================================================================

-- ─── 1a. Migrate FixedTimetable Room ──────────────────────────────────────────
ALTER TABLE public.fixed_timetable RENAME COLUMN room_id TO resource_id;
-- =============================================================================

-- ─── 1. Complete Resource Migration on Bookings ──────────────────────────────
ALTER TABLE public.bookings DROP COLUMN IF EXISTS room_id;
ALTER TABLE public.bookings ALTER COLUMN resource_id SET NOT NULL;

-- ─── 2. Rename RoomBookingRequest to ResourceBookingRequest ────────────────
ALTER TABLE public.room_booking_requests RENAME TO resource_booking_requests;

-- ─── 3. Complete Resource Mapping on ResourceBookingRequest ──────────────────
ALTER TABLE public.resource_booking_requests RENAME COLUMN pref1_room_id TO pref1_resource_id;
ALTER TABLE public.resource_booking_requests RENAME COLUMN pref2_room_id TO pref2_resource_id;
ALTER TABLE public.resource_booking_requests RENAME COLUMN pref3_room_id TO pref3_resource_id;
ALTER TABLE public.resource_booking_requests RENAME COLUMN allocated_room_id TO allocated_resource_id;

-- ─── 4. Re-wire Foreign Keys gracefully (optional strict assurance) ──────────
-- Though renamed, previously these pointed to `rooms`. We redirect to `resources`.
-- In most DBMS simply renaming the column leaves the FK intact to original table.
-- Postgres allows us to drop the old FKs and recreate them safely.

ALTER TABLE public.resource_booking_requests DROP CONSTRAINT IF EXISTS fk2vakrm2ete6sa7mrossj4rndo;
ALTER TABLE public.resource_booking_requests DROP CONSTRAINT IF EXISTS fk8tp8renfgx9arghv7j5xwiv76;
ALTER TABLE public.resource_booking_requests DROP CONSTRAINT IF EXISTS fkc6l0ms8pyx8s75dmva2wjtoyp;
ALTER TABLE public.resource_booking_requests DROP CONSTRAINT IF EXISTS fkctc050evjtk2qdahmu0207x5m;

ALTER TABLE public.resource_booking_requests ADD CONSTRAINT fk_pref1_resource FOREIGN KEY (pref1_resource_id) REFERENCES public.resources(id);
ALTER TABLE public.resource_booking_requests ADD CONSTRAINT fk_pref2_resource FOREIGN KEY (pref2_resource_id) REFERENCES public.resources(id);
ALTER TABLE public.resource_booking_requests ADD CONSTRAINT fk_pref3_resource FOREIGN KEY (pref3_resource_id) REFERENCES public.resources(id);
ALTER TABLE public.resource_booking_requests ADD CONSTRAINT fk_allocated_resource FOREIGN KEY (allocated_resource_id) REFERENCES public.resources(id);

-- ─── 5. Claim-based Admin Flow Support ───────────────────────────────────────
ALTER TABLE public.resource_booking_requests ADD COLUMN IF NOT EXISTS claimed_by_id BIGINT;
ALTER TABLE public.resource_booking_requests ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMP;

ALTER TABLE public.resource_booking_requests ADD CONSTRAINT fk_claimed_by_user FOREIGN KEY (claimed_by_id) REFERENCES public.users(id);
