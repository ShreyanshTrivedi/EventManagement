package com.campus.event.domain;

import java.time.LocalTime;

public enum TimeSlot {
    SLOT_1(9, 0, 9, 50),      // 9:00-9:50
    SLOT_2(9, 50, 10, 40),     // 9:50-10:40
    SLOT_3(10, 40, 11, 30),    // 10:40-11:30
    SLOT_4(11, 30, 12, 20),    // 11:30-12:20
    SLOT_5(12, 20, 13, 10),    // 12:20-13:10
    SLOT_6(13, 10, 14, 0),     // 13:10-14:00
    SLOT_7(14, 0, 14, 50),     // 14:00-14:50
    SLOT_8(14, 50, 15, 40),    // 14:50-15:40
    SLOT_9(15, 40, 16, 30),    // 15:40-16:30
    SLOT_10(16, 30, 17, 20),   // 16:30-17:20
    SLOT_11(17, 20, 18, 10);   // 17:20-18:10
    
    private final LocalTime start;
    private final LocalTime end;
    
    TimeSlot(int startHour, int startMin, int endHour, int endMin) {
        this.start = LocalTime.of(startHour, startMin);
        this.end = LocalTime.of(endHour, endMin);
    }
    
    public LocalTime getStart() {
        return start;
    }
    
    public LocalTime getEnd() {
        return end;
    }
    
    public String getDisplayName() {
        return start + "-" + end;
    }
}
