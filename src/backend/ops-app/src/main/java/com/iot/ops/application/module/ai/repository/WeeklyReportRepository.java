package com.iot.ops.application.module.ai.repository;

import com.iot.ops.application.module.ai.domain.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    List<WeeklyReport> findBySiteIdOrderByPeriodStartDesc(Long siteId);
}
