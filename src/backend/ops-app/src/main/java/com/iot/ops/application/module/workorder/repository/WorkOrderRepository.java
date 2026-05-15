package com.iot.ops.application.module.workorder.repository;

import com.iot.ops.application.module.workorder.domain.WorkOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    List<WorkOrder> findByStatus(String status);

    List<WorkOrder> findBySiteId(Long siteId);

    List<WorkOrder> findByAssigneeId(Long assigneeId);

    List<WorkOrder> findByDeviceId(Long deviceId);

    List<WorkOrder> findByTypeAndStatus(String type, String status);

    Optional<WorkOrder> findByOrderNo(String orderNo);

    List<WorkOrder> findByDeviceIdAndStatus(Long deviceId, String status);

    long countBySiteId(Long siteId);
    long countByStatus(String status);

    long countBySiteIdAndCreatedAtBetween(Long siteId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    List<WorkOrder> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    List<WorkOrder> findByCreatedAtBetweenOrderByCreatedAtDesc(java.time.LocalDateTime start, java.time.LocalDateTime end);

    List<WorkOrder> findByStatusNotIn(List<String> statuses, Pageable pageable);
}
