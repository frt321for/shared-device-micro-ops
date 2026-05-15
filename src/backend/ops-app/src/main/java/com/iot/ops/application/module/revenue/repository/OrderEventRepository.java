package com.iot.ops.application.module.revenue.repository;

import com.iot.ops.application.module.revenue.domain.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findBySiteId(Long siteId);

    List<OrderEvent> findByDeviceId(Long deviceId);

    List<OrderEvent> findBySiteIdAndEventTimeBetween(Long siteId, LocalDateTime start, LocalDateTime end);

    List<OrderEvent> findByEventTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM OrderEvent e")
    BigDecimal sumAmount();

    @Query("SELECT COUNT(e) FROM OrderEvent e")
    long countAll();

    @Query("SELECT e.siteId, COALESCE(SUM(e.amount), 0), COUNT(e) FROM OrderEvent e WHERE e.eventTime BETWEEN :start AND :end GROUP BY e.siteId ORDER BY e.siteId")
    List<Object[]> findSiteRevenueByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
