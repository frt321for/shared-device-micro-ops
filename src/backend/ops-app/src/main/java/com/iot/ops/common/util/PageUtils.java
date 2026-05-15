package com.iot.ops.common.util;

import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.Map;

public final class PageUtils {

    private PageUtils() {
    }

    public static Map<String, Object> toPageResponse(Page<?> page) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", page.getContent());
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("number", page.getNumber());
        result.put("size", page.getSize());
        return result;
    }
}
