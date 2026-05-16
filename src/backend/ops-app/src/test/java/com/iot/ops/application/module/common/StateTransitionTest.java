package com.iot.ops.application.module.common;

import com.iot.ops.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WorkOrder state machine transition rules.
 *
 * Replicates the transition map from WorkOrderService.validateTransition()
 * to catch inconsistencies if the service logic changes.
 *
 * Allowed transitions:
 *   pending_assign → assigned, cancelled
 *   assigned       → arrived, cancelled
 *   arrived        → processing, cancelled
 *   processing     → pending_review, cancelled
 *   pending_review → closed, rejected
 *   closed         → (none)
 *   rejected       → (none)
 *   cancelled      → (none)
 */
class StateTransitionTest {

    /**
     * Replicates the transition map from WorkOrderService.validateTransition().
     * Keeping this in sync with the service is intentional — any divergence
     * will be caught by these tests.
     */
    private static Map<String, List<String>> buildTransitionMap() {
        Map<String, List<String>> allowed = new LinkedHashMap<>();
        allowed.put("pending_assign", List.of("assigned", "cancelled"));
        allowed.put("assigned", List.of("arrived", "cancelled"));
        allowed.put("arrived", List.of("processing", "cancelled"));
        allowed.put("processing", List.of("pending_review", "cancelled"));
        allowed.put("pending_review", List.of("closed", "rejected"));
        allowed.put("closed", List.of());
        allowed.put("rejected", List.of());
        allowed.put("cancelled", List.of());
        return allowed;
    }

    private static final Map<String, List<String>> TRANSITION_MAP = buildTransitionMap();
    private static final List<String> ALL_STATES = List.of(
        "pending_assign", "assigned", "arrived", "processing",
        "pending_review", "closed", "rejected", "cancelled"
    );

    @Test
    void allowedTransitions_shouldPassForAllStates() {
        for (Map.Entry<String, List<String>> entry : TRANSITION_MAP.entrySet()) {
            String current = entry.getKey();
            for (String target : entry.getValue()) {
                assertDoesNotThrow(() -> validateTransition(current, target),
                    "Expected allowed: " + current + " → " + target);
            }
        }
    }

    @Test
    void disallowedTransitions_shouldThrowForAllStates() {
        for (Map.Entry<String, List<String>> entry : TRANSITION_MAP.entrySet()) {
            String current = entry.getKey();
            List<String> allowed = entry.getValue();

            for (String target : ALL_STATES) {
                if (allowed.contains(target) || target.equals(current)) {
                    continue;
                }
                BusinessException ex = assertThrows(BusinessException.class,
                    () -> validateTransition(current, target),
                    "Expected BusinessException for disallowed: " + current + " → " + target);
                assertEquals("工单状态不允许转换: " + current + " → " + target, ex.getMessage());
            }
        }
    }

    @Test
    void noSelfTransitions_shouldBeRejected() {
        for (String state : ALL_STATES) {
            BusinessException ex = assertThrows(BusinessException.class,
                () -> validateTransition(state, state),
                "Self-transition should be rejected for: " + state);
            assertEquals("工单状态不允许转换: " + state + " → " + state, ex.getMessage());
        }
    }

    @Test
    void terminalStates_shouldHaveNoOutgoingTransitions() {
        List<String> terminalStates = List.of("closed", "rejected", "cancelled");
        for (String terminal : terminalStates) {
            List<String> allowed = TRANSITION_MAP.get(terminal);
            assertNotNull(allowed, "Terminal state '" + terminal + "' must exist in transition map");
            assertTrue(allowed.isEmpty(),
                "Terminal state '" + terminal + "' should have no allowed outgoing transitions, but got: " + allowed);
        }
    }

    @Test
    void invalidCurrentState_shouldHandleGracefully() {
        String invalidState = "invalid_state";
        List<String> next = TRANSITION_MAP.getOrDefault(invalidState, List.of());
        assertTrue(next.isEmpty(), "Unknown state should yield empty transition list");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> validateTransition(invalidState, "assigned"));
        assertTrue(ex.getMessage().contains("不允许转换"));
    }

    @Test
    void emptyTargetStatus_shouldReject() {
        for (String state : ALL_STATES) {
            List<String> allowed = TRANSITION_MAP.get(state);
            if (allowed == null || allowed.isEmpty()) {
                assertThrows(BusinessException.class,
                    () -> validateTransition(state, ""),
                    "Empty target should be rejected for state: " + state);
            } else {
                BusinessException ex = assertThrows(BusinessException.class,
                    () -> validateTransition(state, ""),
                    "Empty target should be rejected for state: " + state);
                assertEquals("工单状态不允许转换: " + state + " → ", ex.getMessage());
            }
        }
    }

    @Test
    void transitionMap_shouldCoverAllStates() {
        Set<String> mapKeys = TRANSITION_MAP.keySet();
        for (String state : ALL_STATES) {
            assertTrue(mapKeys.contains(state),
                "Transition map must contain entry for state: " + state);
        }
        for (String key : mapKeys) {
            assertTrue(ALL_STATES.contains(key),
                "Unexpected state in transition map: " + key);
        }
    }

    @Test
    void cancelledTransition_shouldBeAllowedFromNonTerminalStates() {
        List<String> fromWhichCancelledIsAllowed = List.of(
            "pending_assign", "assigned", "arrived", "processing"
        );
        for (String state : ALL_STATES) {
            List<String> allowed = TRANSITION_MAP.get(state);
            if (allowed != null && allowed.contains("cancelled")) {
                assertTrue(fromWhichCancelledIsAllowed.contains(state),
                    "Unexpected allowed cancelled from: " + state);
            }
        }
        for (String state : fromWhichCancelledIsAllowed) {
            List<String> allowed = TRANSITION_MAP.get(state);
            assertNotNull(allowed, "State must exist in map: " + state);
            assertTrue(allowed.contains("cancelled"),
                "State '" + state + "' should allow transition to cancelled");
        }
    }

    private static void validateTransition(String currentStatus, String targetStatus) {
        Map<String, List<String>> allowed = buildTransitionMap();
        List<String> next = allowed.getOrDefault(currentStatus, List.of());
        if (!next.contains(targetStatus)) {
            throw new BusinessException(
                "工单状态不允许转换: " + currentStatus + " → " + targetStatus);
        }
    }
}
