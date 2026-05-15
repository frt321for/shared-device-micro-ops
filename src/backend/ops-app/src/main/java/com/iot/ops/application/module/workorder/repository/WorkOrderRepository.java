package com.iot.ops.application.module.workorder.repository;

import com.iot.ops.application.module.workorder.domain.WorkOrder;
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
}
