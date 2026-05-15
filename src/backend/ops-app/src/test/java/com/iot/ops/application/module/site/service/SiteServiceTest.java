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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}
