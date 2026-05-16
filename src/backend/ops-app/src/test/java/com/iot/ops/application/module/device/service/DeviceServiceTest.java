package com.iot.ops.application.module.device.service;

import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.device.domain.DeviceType;
import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.device.repository.DeviceTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceTypeRepository deviceTypeRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private DeviceService deviceService;

    private Device deviceWithId(Long id) {
        return Device.builder().id(id).deviceCode("DEV-" + id).name("Device " + id)
                .deviceTypeId(1L).siteId(1L).status("online").build();
    }

    // ==================== findById ====================

    @Test
    void findById_shouldReturnDeviceWhenFound() {
        Device device = deviceWithId(1L);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        Optional<Device> result = deviceService.findById(1L);

        assertTrue(result.isPresent());
        assertSame(device, result.get());
        verify(deviceRepository).findById(1L);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Device> result = deviceService.findById(99L);

        assertFalse(result.isPresent());
        verify(deviceRepository).findById(99L);
    }

    // ==================== findAll (Criteria API) ====================

    @SuppressWarnings("unchecked")
    private TypedQuery<Device> mockCriteriaForFindAll(long totalCount) {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Device> dataQuery = mock(CriteriaQuery.class);
        Root<Device> root = mock(Root.class);
        TypedQuery<Device> typedQuery = mock(TypedQuery.class);
        CriteriaQuery<Long> countQuery = mock(CriteriaQuery.class);
        Root<Device> countRoot = mock(Root.class);
        TypedQuery<Long> countTypedQuery = mock(TypedQuery.class);

        Path<Object> anyPath = mock(Path.class);
        Predicate isNullPredicate = mock(Predicate.class);
        Order order = mock(Order.class);
        Expression<Long> countExpr = mock(Expression.class);

        lenient().when(root.get(anyString())).thenReturn(anyPath);
        lenient().when(countRoot.get(anyString())).thenReturn(anyPath);

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Device.class)).thenReturn(dataQuery);
        when(dataQuery.from(Device.class)).thenReturn(root);
        when(cb.createQuery(Long.class)).thenReturn(countQuery);
        when(countQuery.from(Device.class)).thenReturn(countRoot);

        when(entityManager.createQuery(dataQuery)).thenReturn(typedQuery);
        when(entityManager.createQuery(countQuery)).thenReturn(countTypedQuery);

        lenient().when(cb.isNull(any())).thenReturn(isNullPredicate);
        lenient().when(cb.equal(any(), any())).thenReturn(mock(Predicate.class));
        lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
        lenient().when(cb.desc(any())).thenReturn(order);
        lenient().when(cb.count(any())).thenReturn(countExpr);

        when(dataQuery.where(any(Predicate[].class))).thenReturn(dataQuery);
        when(dataQuery.orderBy(any(Order.class))).thenReturn(dataQuery);
        when(countQuery.select(any())).thenReturn(countQuery);
        when(countQuery.where(any(Predicate[].class))).thenReturn(countQuery);

        when(countTypedQuery.getSingleResult()).thenReturn(totalCount);

        return typedQuery;
    }

    @Test
    void findAll_shouldReturnPagedResultsWithNoFilters() {
        TypedQuery<Device> typedQuery = mockCriteriaForFindAll(2L);
        Device d1 = deviceWithId(1L);
        Device d2 = deviceWithId(2L);
        when(typedQuery.getResultList()).thenReturn(List.of(d1, d2));

        Page<Device> result = deviceService.findAll(null, null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(1, result.getTotalPages());
        verify(typedQuery).setFirstResult(0);
        verify(typedQuery).setMaxResults(10);
    }

    @Test
    void findAll_shouldFilterBySiteId() {
        TypedQuery<Device> typedQuery = mockCriteriaForFindAll(1L);
        Device device = deviceWithId(1L);
        when(typedQuery.getResultList()).thenReturn(List.of(device));

        Page<Device> result = deviceService.findAll(1L, null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findAll_shouldFilterByAllParameters() {
        TypedQuery<Device> typedQuery = mockCriteriaForFindAll(1L);
        Device device = deviceWithId(1L);
        when(typedQuery.getResultList()).thenReturn(List.of(device));

        Page<Device> result = deviceService.findAll(1L, 2L, "online", "Device", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(typedQuery).setFirstResult(0);
        verify(typedQuery).setMaxResults(10);
    }

    @Test
    void findAll_shouldReturnEmptyPageWhenNoResults() {
        TypedQuery<Device> typedQuery = mockCriteriaForFindAll(0L);
        when(typedQuery.getResultList()).thenReturn(List.of());

        Page<Device> result = deviceService.findAll(null, null, null, null, 0, 10);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void findAll_shouldRespectPaginationParameters() {
        TypedQuery<Device> typedQuery = mockCriteriaForFindAll(15L);
        when(typedQuery.getResultList()).thenReturn(List.of(deviceWithId(1L)));

        Page<Device> result = deviceService.findAll(null, null, null, null, 2, 5);

        assertNotNull(result);
        assertEquals(15, result.getTotalElements());
        verify(typedQuery).setFirstResult(10);
        verify(typedQuery).setMaxResults(5);
    }

    // ==================== create ====================

    @Test
    void create_shouldSaveAndReturnDevice() {
        Device input = Device.builder().deviceCode("DEV-001").name("New Device")
                .deviceTypeId(1L).siteId(1L).status("offline").build();
        Device saved = deviceWithId(1L);
        when(deviceRepository.save(input)).thenReturn(saved);

        Device result = deviceService.create(input);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(deviceRepository).save(input);
    }

    @Test
    void create_shouldReturnDeviceWithGeneratedId() {
        Device input = Device.builder().deviceCode("DEV-002").name("Another Device")
                .deviceTypeId(2L).siteId(1L).status("offline").build();
        Device saved = deviceWithId(2L);
        when(deviceRepository.save(any(Device.class))).thenReturn(saved);

        Device result = deviceService.create(input);

        assertEquals(2L, result.getId());
        assertEquals("DEV-2", result.getDeviceCode());
    }

    // ==================== update ====================

    @Test
    void update_shouldSaveAndReturnDevice() {
        Device device = deviceWithId(1L);
        device.setName("Updated Name");
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = deviceService.update(device);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        verify(deviceRepository).save(device);
    }

    @Test
    void update_shouldDelegateToRepositorySave() {
        Device device = deviceWithId(5L);
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = deviceService.update(device);

        assertSame(device, result);
    }

    // ==================== delete ====================

    @Test
    void delete_shouldSetDeletedAtWhenFound() {
        Device device = deviceWithId(1L);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        deviceService.delete(1L);

        assertNotNull(device.getDeletedAt());
        verify(deviceRepository).findById(1L);
        verify(deviceRepository).save(device);
    }

    @Test
    void delete_shouldDoNothingWhenNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        deviceService.delete(99L);

        verify(deviceRepository).findById(99L);
        verify(deviceRepository, never()).save(any(Device.class));
    }

    // ==================== getDeviceTypes ====================

    @Test
    void getDeviceTypes_shouldReturnAllDeviceTypes() {
        DeviceType dt1 = DeviceType.builder().id(1L).code("soda").name("Soda Machine").category("vending").build();
        DeviceType dt2 = DeviceType.builder().id(2L).code("snack").name("Snack Machine").category("vending").build();
        when(deviceTypeRepository.findAll()).thenReturn(List.of(dt1, dt2));

        List<DeviceType> result = deviceService.getDeviceTypes();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(dt1));
        assertTrue(result.contains(dt2));
        verify(deviceTypeRepository).findAll();
    }

    @Test
    void getDeviceTypes_shouldReturnEmptyListWhenNoTypes() {
        when(deviceTypeRepository.findAll()).thenReturn(List.of());

        List<DeviceType> result = deviceService.getDeviceTypes();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(deviceTypeRepository).findAll();
    }
}
