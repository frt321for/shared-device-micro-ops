package com.iot.ops.application.module.site.service;

import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.repository.SiteRepository;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import com.iot.ops.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @InjectMocks
    private SiteService siteService;

    @Test
    void findById_shouldReturnSite() {
        Site site = Site.builder().id(1L).name("Test Site").status("active").serviceLevel("standard").build();
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));

        Site result = siteService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Site", result.getName());
    }

    @Test
    void findById_shouldThrowBusinessExceptionWhenNotFound() {
        when(siteRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> siteService.findById(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void findAll_shouldReturnPageOfSites() {
        Site site = Site.builder().id(1L).name("Test Site").status("active").serviceLevel("standard").build();
        Page<Site> page = new PageImpl<>(List.of(site));
        when(siteRepository.findByDeletedAtIsNull(any(Pageable.class))).thenReturn(page);

        Page<Site> result = siteService.findAll(null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Site", result.getContent().get(0).getName());
        verify(siteRepository).findByDeletedAtIsNull(any(Pageable.class));
    }

    @Test
    void findAll_shouldFilterBySearch() {
        Site site = Site.builder().id(1L).name("Test Site").status("active").serviceLevel("standard").build();
        Page<Site> page = new PageImpl<>(List.of(site));
        when(siteRepository.findByDeletedAtIsNullAndNameContainingIgnoreCase(eq("Test"), any(Pageable.class)))
                .thenReturn(page);

        Page<Site> result = siteService.findAll("Test", null, 0, 10);

        assertEquals(1, result.getTotalElements());
        verify(siteRepository).findByDeletedAtIsNullAndNameContainingIgnoreCase(eq("Test"), any(Pageable.class));
    }

    @Test
    void findAll_shouldFilterByStatus() {
        Site site = Site.builder().id(1L).name("Active Site").status("active").serviceLevel("standard").build();
        Page<Site> page = new PageImpl<>(List.of(site));
        when(siteRepository.findByDeletedAtIsNullAndStatus(eq("active"), any(Pageable.class)))
                .thenReturn(page);

        Page<Site> result = siteService.findAll(null, "active", 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("active", result.getContent().get(0).getStatus());
        verify(siteRepository).findByDeletedAtIsNullAndStatus(eq("active"), any(Pageable.class));
    }

    @Test
    void findAll_shouldFilterBySearchAndStatus() {
        Site site = Site.builder().id(1L).name("Office Site").status("active").serviceLevel("premium").build();
        Page<Site> page = new PageImpl<>(List.of(site));
        when(siteRepository.findByDeletedAtIsNullAndStatusAndNameContainingIgnoreCase(
                eq("active"), eq("Office"), any(Pageable.class)))
                .thenReturn(page);

        Page<Site> result = siteService.findAll("Office", "active", 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("Office Site", result.getContent().get(0).getName());
        assertEquals("active", result.getContent().get(0).getStatus());
        verify(siteRepository).findByDeletedAtIsNullAndStatusAndNameContainingIgnoreCase(
                eq("active"), eq("Office"), any(Pageable.class));
    }

    @Test
    void create_shouldSaveAndReturnSite() {
        Site input = Site.builder().name("New Site").status("active").serviceLevel("standard").build();
        Site saved = Site.builder().id(1L).name("New Site").status("active").serviceLevel("standard").build();
        when(siteRepository.save(any(Site.class))).thenReturn(saved);

        Site result = siteService.create(input);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("New Site", result.getName());
        verify(siteRepository).save(input);
    }

    @Test
    void update_shouldModifyFieldsAndSave() {
        Site existing = Site.builder()
                .id(1L).name("Old Name").status("active").serviceLevel("standard")
                .address("Old Address").contactName("Old Contact").contactPhone("111")
                .description("Old description").build();
        Site input = Site.builder()
                .id(1L).name("Updated Name").status("inactive").serviceLevel("premium")
                .address("New Address").building("B1").floor("3F")
                .latitude(31.23).longitude(121.47)
                .businessHours("9:00-18:00")
                .contactName("New Contact").contactPhone("222")
                .description("Updated description").build();

        when(siteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Site result = siteService.update(input);

        assertEquals("Updated Name", result.getName());
        assertEquals("inactive", result.getStatus());
        assertEquals("premium", result.getServiceLevel());
        assertEquals("New Address", result.getAddress());
        assertEquals("B1", result.getBuilding());
        assertEquals("3F", result.getFloor());
        assertEquals(31.23, result.getLatitude(), 0.001);
        assertEquals(121.47, result.getLongitude(), 0.001);
        assertEquals("9:00-18:00", result.getBusinessHours());
        assertEquals("New Contact", result.getContactName());
        assertEquals("222", result.getContactPhone());
        assertEquals("Updated description", result.getDescription());
        verify(siteRepository).save(existing);
    }

    @Test
    void delete_shouldSetDeletedAt() {
        Site site = Site.builder().id(1L).name("Test Site").status("active").serviceLevel("standard").build();
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(siteRepository.save(any(Site.class))).thenReturn(site);

        siteService.delete(1L);

        assertNotNull(site.getDeletedAt());
        assertTrue(site.getDeletedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        verify(siteRepository).save(site);
    }

    @Test
    void getStatistics_shouldReturnCorrectCounts() {
        Site site = Site.builder().id(1L).name("Test Site").build();
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(deviceRepository.countBySiteId(1L)).thenReturn(5L);
        when(deviceRepository.countBySiteIdAndStatus(1L, "online")).thenReturn(3L);
        when(workOrderRepository.countBySiteId(1L)).thenReturn(10L);

        Map<String, Long> stats = siteService.getStatistics(1L);

        assertNotNull(stats);
        assertEquals(3, stats.size());
        assertEquals(5L, stats.get("deviceCount"));
        assertEquals(3L, stats.get("activeDeviceCount"));
        assertEquals(10L, stats.get("workOrderCount"));
    }
}
