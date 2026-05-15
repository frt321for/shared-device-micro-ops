package com.iot.ops.application.module.dispatch.repository;

import com.iot.ops.application.module.dispatch.domain.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByStatus(String status);

    List<Route> findByAssigneeId(Long assigneeId);
}
