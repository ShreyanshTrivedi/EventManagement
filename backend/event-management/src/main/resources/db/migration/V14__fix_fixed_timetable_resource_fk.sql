-- =============================================================================
-- V14: Fix fixed_timetable.resource_id FK to reference resources(id)
-- =============================================================================
-- Root cause:
--   V13 renamed fixed_timetable.room_id -> resource_id, but the existing FK created in V8
--   still referenced rooms(id). After the refactor, the application inserts resource IDs,
--   which do not exist in rooms(id), causing startup/runtime FK violations.
--
-- This migration:
--   1) Drops any FK on fixed_timetable(resource_id) that references rooms(id)
--   2) Backfills legacy rows where resource_id still contains old room IDs
--      (maps via resources.room_ref_id)
--   3) Adds the correct FK fixed_timetable(resource_id) -> resources(id)
--   4) Enforces NOT NULL if safe (no NULLs remain)

DO $$
DECLARE
    conname_to_drop text;
BEGIN
    -- Drop any FK on fixed_timetable that references rooms(id)
    SELECT c.conname INTO conname_to_drop
    FROM pg_constraint c
    JOIN pg_class t       ON t.oid = c.conrelid
    JOIN pg_namespace ns  ON ns.oid = t.relnamespace
    JOIN pg_class ft      ON ft.oid = c.conrelid
    JOIN pg_class ref     ON ref.oid = c.confrelid
    WHERE c.contype = 'f'
      AND ns.nspname = 'public'
      AND ft.relname = 'fixed_timetable'
      AND ref.relname = 'rooms'
    LIMIT 1;

    IF conname_to_drop IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.fixed_timetable DROP CONSTRAINT %I', conname_to_drop);
    END IF;
END $$;

-- Backfill: if fixed_timetable.resource_id still holds a legacy room id, map it to the new resources row
UPDATE public.fixed_timetable ft
SET    resource_id = res.id
FROM   public.resources res
WHERE  ft.resource_id IS NOT NULL
  AND  res.room_ref_id = ft.resource_id
  AND  NOT EXISTS (SELECT 1 FROM public.resources r2 WHERE r2.id = ft.resource_id);

-- Ensure correct FK exists (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace ns ON ns.oid = t.relnamespace
        WHERE c.contype = 'f'
          AND ns.nspname = 'public'
          AND t.relname = 'fixed_timetable'
          AND c.conname = 'fk_fixed_timetable_resource'
    ) THEN
        ALTER TABLE public.fixed_timetable
            ADD CONSTRAINT fk_fixed_timetable_resource
            FOREIGN KEY (resource_id)
            REFERENCES public.resources(id);
    END IF;
END $$;

-- Enforce NOT NULL only if safe (avoids failing existing environments with null timetable rows)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.fixed_timetable WHERE resource_id IS NULL) THEN
        -- Leave nullable; application-level code expects a resource, but we avoid breaking old data here.
        RETURN;
    END IF;

    ALTER TABLE public.fixed_timetable
        ALTER COLUMN resource_id SET NOT NULL;
END $$;
