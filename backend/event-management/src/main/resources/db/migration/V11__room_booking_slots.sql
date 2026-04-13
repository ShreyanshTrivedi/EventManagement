-- ============================================================================
-- V11: Introduce room_booking_slots — slot-based booking model
-- ============================================================================
-- ARCHITECTURAL CHANGE: Instead of allocating ONE room per booking request,
-- we now allocate rooms PER DAY via the room_booking_slots child table.
--
-- Each RoomBookingRequest gets one or more RoomBookingSlot rows:
--   - Single-day event/meeting → 1 slot
--   - Multi-day event (e.g., 3-day workshop) → 3 slots (one per day)
--   - Overnight event (15:00→05:00) → 2 slots (split at midnight)
--
-- Admin approves by allocating a room to EACH slot independently,
-- or by assigning one room to ALL slots (bulk mode).
-- ============================================================================

CREATE TABLE IF NOT EXISTS room_booking_slots (
    id         BIGSERIAL PRIMARY KEY,
    request_id BIGINT       NOT NULL REFERENCES room_booking_requests(id) ON DELETE CASCADE,
    slot_date  DATE         NOT NULL,
    start_time TIME         NOT NULL,
    end_time   TIME         NOT NULL,
    room_id    BIGINT       REFERENCES rooms(id),
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX IF NOT EXISTS idx_rbs_request_id ON room_booking_slots(request_id);
CREATE INDEX IF NOT EXISTS idx_rbs_room_date  ON room_booking_slots(room_id, slot_date);
CREATE INDEX IF NOT EXISTS idx_rbs_date_time  ON room_booking_slots(slot_date, start_time, end_time);

-- ============================================================================
-- Backfill: create one slot per existing booking request
-- This ensures backward compatibility — existing bookings become slot-aware.
-- ============================================================================
INSERT INTO room_booking_slots (request_id, slot_date, start_time, end_time, room_id, status)
SELECT
    rbr.id,
    CAST(COALESCE(e.start_time, rbr.meeting_start) AS date),
    CAST(COALESCE(e.start_time, rbr.meeting_start) AS time),
    CAST(COALESCE(e.end_time, rbr.meeting_end) AS time),
    rbr.allocated_room_id,
    rbr.status
FROM room_booking_requests rbr
LEFT JOIN events e ON rbr.event_id = e.id
WHERE COALESCE(e.start_time, rbr.meeting_start) IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM room_booking_slots rbs WHERE rbs.request_id = rbr.id
  );
