package com.iot.ops.application.module.revenue.repository;

import com.iot.ops.application.module.revenue.domain.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findBySiteId(Long siteId);

    List<OrderEvent> findByDeviceId(Long deviceId);

    List<OrderEvent> findBySiteIdAndEventTimeBetween(Long siteId, LocalDateTime start, LocalDateTime end);
}
