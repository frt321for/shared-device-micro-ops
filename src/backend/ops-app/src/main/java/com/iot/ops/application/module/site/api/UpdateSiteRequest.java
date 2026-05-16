package com.iot.ops.application.module.site.api;

import jakarta.validation.constraints.Size;

public record UpdateSiteRequest(
    @Size(max = 128) String name,
    @Size(max = 256) String address,
    @Size(max = 128) String building,
    @Size(max = 32) String floor,
    Double latitude,
    Double longitude,
    @Size(max = 64) String businessHours,
    @Size(max = 16) String serviceLevel,
    @Size(max = 16) String status,
    @Size(max = 64) String contactName,
    @Size(max = 32) String contactPhone,
    String description
) {}
