package com.iot.ops.application.module.workorder.service;

import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.domain.WorkOrderAudit;
import com.iot.ops.application.module.workorder.repository.WorkOrderAuditRepository;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import com.iot.ops.common.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderAuditRepository auditRepository;
    private final EntityManager entityManager;

    public WorkOrder findById(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("WorkOrder not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<WorkOrder> findAll(String status, String type, Long assigneeId, Long siteId, int page, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<WorkOrder> dataQuery = cb.createQuery(WorkOrder.class);
        Root<WorkOrder> root = dataQuery.from(WorkOrder.class);
        List<Predicate> predicates = buildPredicates(cb, root, status, type, assigneeId, siteId);
        dataQuery.where(predicates.toArray(new Predicate[0]));
        dataQuery.orderBy(cb.desc(root.get("createdAt")));

        TypedQuery<WorkOrder> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<WorkOrder> countRoot = countQuery.from(WorkOrder.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, status, type, assigneeId, siteId);
        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        long total = entityManager.createQuery(countQuery).getSingleResult();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return new PageImpl<>(typedQuery.getResultList(), pageRequest, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<WorkOrder> root,
                                            String status, String type, Long assigneeId, Long siteId) {
        List<Predicate> predicates = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (type != null && !type.isEmpty()) {
            predicates.add(cb.equal(root.get("type"), type));
        }
        if (assigneeId != null) {
            predicates.add(cb.equal(root.get("assigneeId"), assigneeId));
        }
        if (siteId != null) {
            predicates.add(cb.equal(root.get("siteId"), siteId));
        }
        return predicates;
    }

    public WorkOrder create(WorkOrder workOrder) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq = String.format("%04d", System.currentTimeMillis() % 10000);
        workOrder.setOrderNo("WO" + datePart + seq);
        workOrder.setStatus("pending_assign");
        return workOrderRepository.save(workOrder);
    }

    public WorkOrder assign(Long id, Long assigneeId) {
        WorkOrder wo = findById(id);
        validateTransition(wo.getStatus(), "assigned");
        String fromStatus = wo.getStatus();
        wo.setAssigneeId(assigneeId);
        wo.setStatus("assigned");
        workOrderRepository.save(wo);
        addAudit(wo.getId(), fromStatus, "assigned", assigneeId, null);
        return wo;
    }

    public WorkOrder arrive(Long id) {
        WorkOrder wo = findById(id);
        validateTransition(wo.getStatus(), "arrived");
        String fromStatus = wo.getStatus();
        wo.setArrivedAt(LocalDateTime.now());
        wo.setStatus("arrived");
        workOrderRepository.save(wo);
        addAudit(wo.getId(), fromStatus, "arrived", null, null);
        return wo;
    }

    public WorkOrder process(Long id) {
        WorkOrder wo = findById(id);
        validateTransition(wo.getStatus(), "processing");
        String fromStatus = wo.getStatus();
        wo.setStatus("processing");
        workOrderRepository.save(wo);
        addAudit(wo.getId(), fromStatus, "processing", null, null);
        return wo;
    }

    public WorkOrder complete(Long id, Integer actualQty) {
        WorkOrder wo = findById(id);
        validateTransition(wo.getStatus(), "pending_review");
        String fromStatus = wo.getStatus();
        wo.setActualQty(actualQty);
        wo.setCompletedAt(LocalDateTime.now());
        wo.setStatus("pending_review");
        workOrderRepository.save(wo);
        addAudit(wo.getId(), fromStatus, "pending_review", null, null);
        return wo;
    }

    public WorkOrder review(Long id, String result, String remark) {
        WorkOrder wo = findById(id);
        String targetStatus = "approved".equals(result) ? "closed" : "rejected";
        validateTransition(wo.getStatus(), targetStatus);
        String fromStatus = wo.getStatus();
        wo.setReviewResult(result);
        wo.setReviewRemark(remark);
        if ("approved".equals(result)) {
            wo.setStatus("closed");
            wo.setClosedAt(LocalDateTime.now());
        } else {
            wo.setStatus("rejected");
        }
        workOrderRepository.save(wo);
        addAudit(wo.getId(), fromStatus, wo.getStatus(), null, result + ": " + (remark != null ? remark : ""));
        return wo;
    }

    public WorkOrder cancel(Long id) {
        WorkOrder wo = findById(id);
        validateTransition(wo.getStatus(), "cancelled");
        String fromStatus = wo.getStatus();
        wo.setStatus("cancelled");
        workOrderRepository.save(wo);
        addAudit(wo.getId(), fromStatus, "cancelled", null, null);
        return wo;
    }

    @Transactional(readOnly = true)
    public List<WorkOrderAudit> getAuditLogs(Long workOrderId) {
        return auditRepository.findByWorkOrderIdOrderByCreatedAtAsc(workOrderId);
    }

    private void validateTransition(String currentStatus, String targetStatus) {
        Map<String, List<String>> allowed = Map.of(
            "pending_assign", List.of("assigned", "cancelled"),
            "assigned", List.of("arrived", "cancelled"),
            "arrived", List.of("processing", "cancelled"),
            "processing", List.of("pending_review", "cancelled"),
            "pending_review", List.of("closed", "rejected"),
            "closed", List.of(),
            "rejected", List.of(),
            "cancelled", List.of()
        );
        List<String> next = allowed.getOrDefault(currentStatus, List.of());
        if (!next.contains(targetStatus)) {
            throw new BusinessException(
                "工单状态不允许转换: " + currentStatus + " → " + targetStatus);
        }
    }

    private void addAudit(Long workOrderId, String fromStatus, String toStatus,
                          Long operatorId, String remark) {
        WorkOrderAudit audit = WorkOrderAudit.builder()
                .workOrderId(workOrderId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .operatorId(operatorId)
                .remark(remark)
                .build();
        auditRepository.save(audit);
    }
}
