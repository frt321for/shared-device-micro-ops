package com.iot.ops.application.module.workorder.repository;

import com.iot.ops.application.module.workorder.domain.WorkOrderAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderAuditRepository extends JpaRepository<WorkOrderAudit, Long> {

    List<WorkOrderAudit> findByWorkOrderIdOrderByCreatedAtAsc(Long workOrderId);
}
