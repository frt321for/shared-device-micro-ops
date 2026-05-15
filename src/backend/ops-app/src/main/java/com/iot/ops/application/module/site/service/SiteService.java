package com.iot.ops.application.module.site.service;

import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;
    private final DeviceRepository deviceRepository;
    private final WorkOrderRepository workOrderRepository;

    public Site findById(Long id) {
        return siteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Site not found with id: " + id));
    }

    public Page<Site> findAll(String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        if (search != null && !search.isBlank() && status != null && !status.isBlank()) {
            return siteRepository.findByDeletedAtIsNullAndStatusAndNameContainingIgnoreCase(status, search, pageable);
        } else if (search != null && !search.isBlank()) {
            return siteRepository.findByDeletedAtIsNullAndNameContainingIgnoreCase(search, pageable);
        } else if (status != null && !status.isBlank()) {
            return siteRepository.findByDeletedAtIsNullAndStatus(status, pageable);
        }
        return siteRepository.findByDeletedAtIsNull(pageable);
    }

    @Transactional
    public Site create(Site site) {
        return siteRepository.save(site);
    }

    @Transactional
    public Site update(Site site) {
        Site existing = findById(site.getId());
        existing.setName(site.getName());
        existing.setAddress(site.getAddress());
        existing.setBuilding(site.getBuilding());
        existing.setFloor(site.getFloor());
        existing.setLatitude(site.getLatitude());
        existing.setLongitude(site.getLongitude());
        existing.setBusinessHours(site.getBusinessHours());
        existing.setServiceLevel(site.getServiceLevel());
        existing.setStatus(site.getStatus());
        existing.setContactName(site.getContactName());
        existing.setContactPhone(site.getContactPhone());
        existing.setDescription(site.getDescription());
        return siteRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Site site = findById(id);
        site.setDeletedAt(LocalDateTime.now());
        siteRepository.save(site);
    }

    public Map<String, Long> getStatistics(Long id) {
        findById(id);
        Map<String, Long> stats = new HashMap<>();
        stats.put("deviceCount", (long) deviceRepository.findBySiteId(id).size());
        stats.put("activeDeviceCount", deviceRepository.countBySiteIdAndStatus(id, "active"));
        stats.put("workOrderCount", (long) workOrderRepository.findBySiteId(id).size());
        return stats;
    }
}
