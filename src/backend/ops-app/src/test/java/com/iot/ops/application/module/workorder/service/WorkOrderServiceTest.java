package com.iot.ops.application.module.workorder.service;

import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.domain.WorkOrderAudit;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private WorkOrder workOrderWithId(Long id, String status) {
        return WorkOrder.builder().id(id).title("Test").type("repair").status(status).build();
    }

    @Test
    void create_shouldGenerateOrderNoWithCorrectPrefix() {
        WorkOrder input = WorkOrder.builder().title("Test Work Order").type("repair").build();

        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.create(input);

        assertNotNull(result.getOrderNo());
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertTrue(result.getOrderNo().startsWith("WO" + datePart));
    }

    @Test
    void create_shouldDefaultStatusToPendingAssign() {
        WorkOrder input = WorkOrder.builder().title("Test Work Order").type("replenishment").build();

        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.create(input);

        assertEquals("pending_assign", result.getStatus());
    }

    @Test
    void assign_shouldChangeStatusAndCreateAudit() {
        WorkOrder wo = workOrderWithId(1L, "pending_assign");
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.assign(1L, 100L);

        assertEquals("assigned", result.getStatus());
        assertEquals(100L, result.getAssigneeId());
        verify(auditRepository).save(any(WorkOrderAudit.class));
    }

    @Test
    void arrive_shouldChangeStatusToArrived() {
        WorkOrder wo = workOrderWithId(1L, "assigned");
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.arrive(1L);

        assertEquals("arrived", result.getStatus());
        assertNotNull(result.getArrivedAt());
    }

    @Test
    void process_shouldChangeStatusToProcessing() {
        WorkOrder wo = workOrderWithId(1L, "arrived");
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.process(1L);

        assertEquals("processing", result.getStatus());
    }

    @Test
    void complete_shouldSetActualQtyAndChangeStatusToPendingReview() {
        WorkOrder wo = workOrderWithId(1L, "processing");
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.complete(1L, 5);

        assertEquals("pending_review", result.getStatus());
        assertEquals(5, result.getActualQty());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void review_approved_shouldCloseOrder() {
        WorkOrder wo = workOrderWithId(1L, "pending_review");
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.review(1L, "approved", "All good");

        assertEquals("closed", result.getStatus());
        assertEquals("approved", result.getReviewResult());
        assertNotNull(result.getClosedAt());
    }

    @Test
    void review_nonApproved_shouldSetReviewResultAndNotClose() {
        WorkOrder wo = new WorkOrder();
        wo.setId(1L);
        wo.setTitle("Test");
        wo.setType("repair");
        wo.setStatus("pending_review");
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.review(1L, "rejected", "Not satisfied");

        assertEquals("rejected", result.getReviewResult());
        assertNull(result.getClosedAt());
    }

    @Test
    void cancel_shouldChangeStatusToCancelled() {
        WorkOrder wo = workOrderWithId(1L, "pending_assign");
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = workOrderService.cancel(1L);

        assertEquals("cancelled", result.getStatus());
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(workOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workOrderService.findById(99L));
    }
}
