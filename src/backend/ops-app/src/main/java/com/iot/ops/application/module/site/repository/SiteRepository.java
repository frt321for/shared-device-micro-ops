package com.iot.ops.application.module.site.repository;

import com.iot.ops.application.module.site.domain.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {
    List<Site> findByStatus(String status);
    List<Site> findByNameContainingIgnoreCase(String name);

    Page<Site> findByDeletedAtIsNull(Pageable pageable);
    Page<Site> findByDeletedAtIsNullAndStatus(String status, Pageable pageable);
    Page<Site> findByDeletedAtIsNullAndNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Site> findByDeletedAtIsNullAndStatusAndNameContainingIgnoreCase(String status, String name, Pageable pageable);
}
