package com.campus.event.testsupport;

import com.campus.event.domain.Building;

/**
 * Shared {@link Building} instances for unit tests (events require a non-null building).
 */
public final class TestBuildings {

    private TestBuildings() {
    }

    /** Typical campus building used across service tests. */
    public static Building defaultBuilding() {
        Building b = new Building("Academic Block 1", "AB1", "Test building");
        b.setActive(true);
        return b;
    }
}
