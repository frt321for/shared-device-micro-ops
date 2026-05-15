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
    public ApiResponse<Site> update(@PathVariable Long id, @Valid @RequestBody SiteRequest request) {
        Site site = Site.builder()
            .id(id)
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
        return ApiResponse.success(siteService.update(site));
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
