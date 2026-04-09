-- ============================================================================
-- V9: Fix rooms_type_check constraint to include all RoomType enum values
-- ============================================================================
-- ROOT CAUSE: Hibernate auto-generated a CHECK constraint on rooms.type that
-- only included the original enum values (CLASSROOM, LAB, HALL). When newer
-- values (AUDITORIUM, LECTURE_HALL, SEMINAR_HALL, MEETING_ROOM) were added to
-- the Java RoomType enum, the DB constraint was never updated.
--
-- This causes a startup crash:
--   ERROR: new row violates check constraint "rooms_type_check"
--   Failing value: AUDITORIUM
--
-- FIX: Drop the stale constraint and re-create it with all 6 current values.
-- ============================================================================

ALTER TABLE public.rooms
    DROP CONSTRAINT IF EXISTS rooms_type_check;

ALTER TABLE public.rooms
    ADD CONSTRAINT rooms_type_check
    CHECK (type IN ('CLASSROOM', 'LAB', 'HALL', 'AUDITORIUM', 'LECTURE_HALL', 'SEMINAR_HALL', 'MEETING_ROOM'));
