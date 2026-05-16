package com.iot.ops.application.module.workorder.service;

import com.iot.ops.application.module.workorder.domain.WorkOrderStatus;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderStatusTest {

    @Test
    void enumSize_shouldBeEight() {
        assertEquals(8, WorkOrderStatus.values().length);
    }

    @Test
    void allEnumValues_shouldExist() {
        assertNotNull(WorkOrderStatus.valueOf("PENDING_ASSIGN"));
        assertNotNull(WorkOrderStatus.valueOf("ASSIGNED"));
        assertNotNull(WorkOrderStatus.valueOf("ARRIVED"));
        assertNotNull(WorkOrderStatus.valueOf("PROCESSING"));
        assertNotNull(WorkOrderStatus.valueOf("PENDING_REVIEW"));
        assertNotNull(WorkOrderStatus.valueOf("CLOSED"));
        assertNotNull(WorkOrderStatus.valueOf("REJECTED"));
        assertNotNull(WorkOrderStatus.valueOf("CANCELLED"));
    }

    @Test
    void displayName_shouldBeNonEmpty() {
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            assertNotNull(status.getDisplayName(), "DisplayName should not be null for " + status.name());
            assertFalse(status.getDisplayName().isEmpty(), "DisplayName should not be empty for " + status.name());
        }
    }

    @Test
    void displayNames_shouldMatchExactly() {
        assertEquals("待派单", WorkOrderStatus.PENDING_ASSIGN.getDisplayName());
        assertEquals("已派单", WorkOrderStatus.ASSIGNED.getDisplayName());
        assertEquals("已到场", WorkOrderStatus.ARRIVED.getDisplayName());
        assertEquals("处理中", WorkOrderStatus.PROCESSING.getDisplayName());
        assertEquals("待复核", WorkOrderStatus.PENDING_REVIEW.getDisplayName());
        assertEquals("已关闭", WorkOrderStatus.CLOSED.getDisplayName());
        assertEquals("已驳回", WorkOrderStatus.REJECTED.getDisplayName());
        assertEquals("已取消", WorkOrderStatus.CANCELLED.getDisplayName());
    }

    @Test
    void allDisplayNames_shouldBeUnique() {
        Set<String> displayNames = new HashSet<>();
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            boolean added = displayNames.add(status.getDisplayName());
            assertTrue(added, "Duplicate displayName found: " + status.getDisplayName());
        }
        assertEquals(8, displayNames.size());
    }

    @Test
    void toStatusString_shouldReturnCorrectDbMapping() {
        Map<WorkOrderStatus, String> expectedDbMappings = new HashMap<>();
        expectedDbMappings.put(WorkOrderStatus.PENDING_ASSIGN, "pending_assign");
        expectedDbMappings.put(WorkOrderStatus.ASSIGNED, "assigned");
        expectedDbMappings.put(WorkOrderStatus.ARRIVED, "arrived");
        expectedDbMappings.put(WorkOrderStatus.PROCESSING, "processing");
        expectedDbMappings.put(WorkOrderStatus.PENDING_REVIEW, "pending_review");
        expectedDbMappings.put(WorkOrderStatus.CLOSED, "closed");
        expectedDbMappings.put(WorkOrderStatus.REJECTED, "rejected");
        expectedDbMappings.put(WorkOrderStatus.CANCELLED, "cancelled");

        for (Map.Entry<WorkOrderStatus, String> entry : expectedDbMappings.entrySet()) {
            WorkOrderStatus status = entry.getKey();
            String expectedDbString = entry.getValue();
            String actual = status.name().toLowerCase();
            assertEquals(expectedDbString, actual,
                "DB string for " + status.name() + " should be '" + expectedDbString + "'");
        }
    }

    @Test
    void allStatusStrings_shouldBeUnique() {
        Set<String> statusStrings = new HashSet<>();
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            String dbString = status.name().toLowerCase();
            boolean added = statusStrings.add(dbString);
            assertTrue(added, "Duplicate status string found: " + dbString);
        }
        assertEquals(8, statusStrings.size());
    }

    @Test
    void valueOf_withInvalidName_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> WorkOrderStatus.valueOf("UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> WorkOrderStatus.valueOf("PENDING"));
        assertThrows(IllegalArgumentException.class, () -> WorkOrderStatus.valueOf(""));
    }
}
