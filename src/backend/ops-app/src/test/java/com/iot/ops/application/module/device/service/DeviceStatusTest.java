package com.iot.ops.application.module.device.service;

import com.iot.ops.application.module.device.domain.DeviceStatus;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeviceStatusTest {

    @Test
    void enumSize_shouldBeEight() {
        assertEquals(8, DeviceStatus.values().length);
    }

    @Test
    void allEnumValues_shouldExist() {
        assertNotNull(DeviceStatus.valueOf("INACTIVE"));
        assertNotNull(DeviceStatus.valueOf("ONLINE"));
        assertNotNull(DeviceStatus.valueOf("LOW_STOCK"));
        assertNotNull(DeviceStatus.valueOf("OUT_OF_STOCK"));
        assertNotNull(DeviceStatus.valueOf("FAULT"));
        assertNotNull(DeviceStatus.valueOf("MAINTENANCE"));
        assertNotNull(DeviceStatus.valueOf("RECOVERING"));
        assertNotNull(DeviceStatus.valueOf("RETIRED"));
    }

    @Test
    void displayName_shouldBeNonEmpty() {
        for (DeviceStatus status : DeviceStatus.values()) {
            assertNotNull(status.getDisplayName(), "DisplayName should not be null for " + status.name());
            assertFalse(status.getDisplayName().isEmpty(), "DisplayName should not be empty for " + status.name());
        }
    }

    @Test
    void displayNames_shouldMatchChineseTextExactly() {
        assertEquals("未启用", DeviceStatus.INACTIVE.getDisplayName());
        assertEquals("在线", DeviceStatus.ONLINE.getDisplayName());
        assertEquals("缺货预警", DeviceStatus.LOW_STOCK.getDisplayName());
        assertEquals("缺货", DeviceStatus.OUT_OF_STOCK.getDisplayName());
        assertEquals("故障", DeviceStatus.FAULT.getDisplayName());
        assertEquals("维护中", DeviceStatus.MAINTENANCE.getDisplayName());
        assertEquals("恢复在线", DeviceStatus.RECOVERING.getDisplayName());
        assertEquals("停用", DeviceStatus.RETIRED.getDisplayName());
    }

    @Test
    void allDisplayNames_shouldBeUnique() {
        Set<String> displayNames = new HashSet<>();
        for (DeviceStatus status : DeviceStatus.values()) {
            boolean added = displayNames.add(status.getDisplayName());
            assertTrue(added, "Duplicate displayName found: " + status.getDisplayName());
        }
        assertEquals(8, displayNames.size());
    }

    @Test
    void valueOf_withInvalidName_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> DeviceStatus.valueOf("UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> DeviceStatus.valueOf(""));
    }

    @Test
    void valueOf_shouldBeCaseSensitive() {
        assertThrows(IllegalArgumentException.class, () -> DeviceStatus.valueOf("inactive"));
        assertThrows(IllegalArgumentException.class, () -> DeviceStatus.valueOf("Online"));
    }
}
