package com.iot.ops.application.module.device.service;

import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.device.domain.DeviceType;
import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.device.repository.DeviceTypeRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final EntityManager entityManager;

    public Optional<Device> findById(Long id) {
        return deviceRepository.findById(id);
    }

    public Page<Device> findAll(Long siteId, Long deviceTypeId, String status, int page, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Device> dataQuery = cb.createQuery(Device.class);
        Root<Device> root = dataQuery.from(Device.class);
        List<Predicate> predicates = buildPredicates(cb, root, siteId, deviceTypeId, status);
        dataQuery.where(predicates.toArray(new Predicate[0]));
        dataQuery.orderBy(cb.desc(root.get("createdAt")));

        TypedQuery<Device> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Device> countRoot = countQuery.from(Device.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, siteId, deviceTypeId, status);
        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(new Predicate[0]));

        long total = entityManager.createQuery(countQuery).getSingleResult();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return new PageImpl<>(typedQuery.getResultList(), pageRequest, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Device> root,
                                            Long siteId, Long deviceTypeId, String status) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isNull(root.get("deletedAt")));
        if (siteId != null) {
            predicates.add(cb.equal(root.get("siteId"), siteId));
        }
        if (deviceTypeId != null) {
            predicates.add(cb.equal(root.get("deviceTypeId"), deviceTypeId));
        }
        if (status != null && !status.isEmpty()) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        return predicates;
    }

    public Device create(Device device) {
        return deviceRepository.save(device);
    }

    public Device update(Device device) {
        return deviceRepository.save(device);
    }

    public void delete(Long id) {
        deviceRepository.findById(id).ifPresent(device -> {
            device.setDeletedAt(LocalDateTime.now());
            deviceRepository.save(device);
        });
    }

    @Transactional(readOnly = true)
    public List<DeviceType> getDeviceTypes() {
        return deviceTypeRepository.findAll();
    }
}
