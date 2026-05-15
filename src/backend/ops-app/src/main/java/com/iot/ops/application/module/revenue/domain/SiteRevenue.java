package com.iot.ops.application.module.revenue.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteRevenue {

    private Long siteId;
    private String siteName;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal grossProfit;
    private BigDecimal lossAmount;
    private Long orderCount;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}
