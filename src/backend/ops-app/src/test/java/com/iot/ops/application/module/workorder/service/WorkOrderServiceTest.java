package com.iot.ops.application.module.workorder.service;

import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.repository.WorkOrderAuditRepository;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderAuditRepository auditRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private WorkOrderService workOrderService;

    @Test
    void create_shouldGenerateOrderNoWithCorrectPrefix() {
        WorkOrder input = new WorkOrder();
        input.setTitle("Test Work Order");
        input.setType("repair");

        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.create(input);

        assertNotNull(result.getOrderNo());
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertTrue(result.getOrderNo().startsWith("WO" + datePart));
    }

    @Test
    void create_shouldDefaultStatusToPendingAssign() {
        WorkOrder input = new WorkOrder();
        input.setTitle("Test Work Order");
        input.setType("replenishment");

        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.create(input);

        assertEquals("pending_assign", result.getStatus());
    }
}
