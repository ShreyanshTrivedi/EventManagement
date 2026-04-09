-- ============================================================================
-- V10: Remove invalid/unknown buildings and add unique constraint on code
-- ============================================================================
-- ISSUE: A building with code 'RLHC' (or similar unexpected codes) appeared in
-- production despite never being defined in the DataInitializer. This was likely
-- inserted by an earlier version of the seed logic, or manually. We also clean
-- up the fallback 'MAIN' building that DataInitializer creates when no
-- buildings exist at event-seed time.
--
-- FIX:
--   1. Delete any buildings NOT in the approved set (BLD_A, BLD_B).
--      - CASCADE handles FK cleanup for floors/rooms/bookings referencing them.
--   2. Add UNIQUE constraint on buildings.code to prevent future duplicates.
-- ============================================================================

-- Step 1: Reassign events that reference non-standard buildings to BLD_A
UPDATE events
SET building_id = (SELECT id FROM buildings WHERE code = 'BLD_A' LIMIT 1)
WHERE building_id IN (
    SELECT id FROM buildings WHERE code NOT IN ('BLD_A', 'BLD_B')
)
AND EXISTS (SELECT 1 FROM buildings WHERE code = 'BLD_A');

-- Step 2: Delete rooms on floors of invalid buildings
DELETE FROM rooms
WHERE floor_id IN (
    SELECT f.id FROM floors f
    JOIN buildings b ON f.building_id = b.id
    WHERE b.code NOT IN ('BLD_A', 'BLD_B')
);

-- Step 3: Delete floors of invalid buildings
DELETE FROM floors
WHERE building_id IN (
    SELECT id FROM buildings WHERE code NOT IN ('BLD_A', 'BLD_B')
);

-- Step 4: Delete building timetable entries for invalid buildings
DELETE FROM building_timetable
WHERE building_id IN (
    SELECT id FROM buildings WHERE code NOT IN ('BLD_A', 'BLD_B')
);

-- Step 5: Clear managed_building_id references on users pointing to invalid buildings
UPDATE users
SET managed_building_id = NULL
WHERE managed_building_id IN (
    SELECT id FROM buildings WHERE code NOT IN ('BLD_A', 'BLD_B')
);

-- Step 6: Delete the invalid buildings themselves
DELETE FROM buildings WHERE code NOT IN ('BLD_A', 'BLD_B');

-- Step 7: Add unique constraint on building code to prevent future issues
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_buildings_code') THEN
        ALTER TABLE buildings ADD CONSTRAINT uk_buildings_code UNIQUE (code);
    END IF;
END $$;
