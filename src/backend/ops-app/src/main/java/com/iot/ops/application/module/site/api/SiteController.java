package com.iot.ops.application.module.site.api;

import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.service.SiteService;
import com.iot.ops.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;
    private final DeviceRepository deviceRepository;

    @GetMapping
    public ApiResponse<Page<Site>> list(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(siteService.findAll(search, status, page, size));
    }

    @PostMapping
    public ApiResponse<Site> create(@Valid @RequestBody SiteRequest request) {
        Site site = Site.builder()
            .name(request.name())
            .address(request.address())
            .building(request.building())
            .floor(request.floor())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .businessHours(request.businessHours())
            .serviceLevel(request.serviceLevel())
            .status(request.status())
            .contactName(request.contactName())
            .contactPhone(request.contactPhone())
            .description(request.description())
            .build();
        return ApiResponse.created(siteService.create(site));
    }

    @GetMapping("/{id}")
    public ApiResponse<Site> detail(@PathVariable Long id) {
        return ApiResponse.success(siteService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Site> update(@PathVariable Long id, @Valid @RequestBody UpdateSiteRequest request) {
        Site existing = siteService.findById(id);
        if (request.name() != null) existing.setName(request.name());
        if (request.address() != null) existing.setAddress(request.address());
        if (request.building() != null) existing.setBuilding(request.building());
        if (request.floor() != null) existing.setFloor(request.floor());
        if (request.latitude() != null) existing.setLatitude(request.latitude());
        if (request.longitude() != null) existing.setLongitude(request.longitude());
        if (request.businessHours() != null) existing.setBusinessHours(request.businessHours());
        if (request.serviceLevel() != null) existing.setServiceLevel(request.serviceLevel());
        if (request.status() != null) existing.setStatus(request.status());
        if (request.contactName() != null) existing.setContactName(request.contactName());
        if (request.contactPhone() != null) existing.setContactPhone(request.contactPhone());
        if (request.description() != null) existing.setDescription(request.description());
        return ApiResponse.success(siteService.update(existing));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        siteService.delete(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/statistics")
    public ApiResponse<Map<String, Long>> statistics(@PathVariable Long id) {
        return ApiResponse.success(siteService.getStatistics(id));
    }
}
