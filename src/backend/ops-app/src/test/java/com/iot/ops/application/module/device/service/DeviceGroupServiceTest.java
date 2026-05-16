package com.iot.ops.application.module.device.service;

import com.iot.ops.application.module.device.domain.DeviceGroup;
import com.iot.ops.application.module.device.domain.DeviceGroupMember;
import com.iot.ops.application.module.device.repository.DeviceGroupMemberRepository;
import com.iot.ops.application.module.device.repository.DeviceGroupRepository;
import com.iot.ops.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceGroupServiceTest {

    @Mock
    private DeviceGroupRepository deviceGroupRepository;

    @Mock
    private DeviceGroupMemberRepository deviceGroupMemberRepository;

    @InjectMocks
    private DeviceGroupService deviceGroupService;

    private DeviceGroup groupWithId(Long id) {
        return DeviceGroup.builder().id(id).name("Group-" + id).siteId(1L).description("Test group").build();
    }

    // ==================== findAll ====================

    @Test
    void findAll_shouldReturnAllWhenSiteIdNull() {
        DeviceGroup g1 = groupWithId(1L);
        DeviceGroup g2 = groupWithId(2L);
        when(deviceGroupRepository.findAll()).thenReturn(List.of(g1, g2));

        List<DeviceGroup> result = deviceGroupService.findAll(null);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(deviceGroupRepository).findAll();
        verify(deviceGroupRepository, never()).findBySiteId(any());
    }

    @Test
    void findAll_shouldFilterBySiteId() {
        DeviceGroup g1 = groupWithId(1L);
        when(deviceGroupRepository.findBySiteId(1L)).thenReturn(List.of(g1));

        List<DeviceGroup> result = deviceGroupService.findAll(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(g1, result.get(0));
        verify(deviceGroupRepository).findBySiteId(1L);
        verify(deviceGroupRepository, never()).findAll();
    }

    // ==================== findById ====================

    @Test
    void findById_shouldReturnGroupWhenFound() {
        DeviceGroup group = groupWithId(1L);
        when(deviceGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        DeviceGroup result = deviceGroupService.findById(1L);

        assertNotNull(result);
        assertSame(group, result);
        verify(deviceGroupRepository).findById(1L);
    }

    @Test
    void findById_shouldThrowBusinessExceptionWhenNotFound() {
        when(deviceGroupRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deviceGroupService.findById(99L));
        assertTrue(ex.getMessage().contains("99"));
        verify(deviceGroupRepository).findById(99L);
    }

    // ==================== create ====================

    @Test
    void create_shouldBuildAndSaveGroup() {
        when(deviceGroupRepository.save(any(DeviceGroup.class)))
                .thenAnswer(invocation -> {
                    DeviceGroup g = invocation.getArgument(0);
                    g.setId(1L);
                    return g;
                });

        DeviceGroup result = deviceGroupService.create("New Group", 10L, "A description");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("New Group", result.getName());
        assertEquals(10L, result.getSiteId());
        assertEquals("A description", result.getDescription());
        verify(deviceGroupRepository).save(any(DeviceGroup.class));
    }

    @Test
    void create_shouldSetProvidedFields() {
        when(deviceGroupRepository.save(any(DeviceGroup.class)))
                .thenAnswer(invocation -> {
                    DeviceGroup g = invocation.getArgument(0);
                    g.setId(2L);
                    return g;
                });

        DeviceGroup result = deviceGroupService.create("Group-X", 5L, null);

        assertNotNull(result);
        assertEquals("Group-X", result.getName());
        assertEquals(5L, result.getSiteId());
        assertNull(result.getDescription());
    }

    // ==================== update ====================

    @Test
    void update_shouldFindAndUpdateFields() {
        DeviceGroup existing = groupWithId(1L);
        when(deviceGroupRepository.findById(1L)).thenReturn(Optional.of(existing));
        DeviceGroup updated = groupWithId(1L);
        updated.setName("Updated Name");
        updated.setDescription("Updated Desc");
        when(deviceGroupRepository.save(existing)).thenReturn(updated);

        DeviceGroup result = deviceGroupService.update(1L, "Updated Name", "Updated Desc");

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Desc", result.getDescription());
        verify(deviceGroupRepository).findById(1L);
        verify(deviceGroupRepository).save(existing);
    }

    @Test
    void update_shouldThrowBusinessExceptionWhenGroupNotFound() {
        when(deviceGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> deviceGroupService.update(99L, "Name", "Desc"));
        verify(deviceGroupRepository).findById(99L);
        verify(deviceGroupRepository, never()).save(any());
    }

    // ==================== delete ====================

    @Test
    void delete_shouldDeleteMembersAndGroup() {
        DeviceGroup group = groupWithId(1L);
        when(deviceGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        deviceGroupService.delete(1L);

        verify(deviceGroupMemberRepository).deleteByGroupId(1L);
        verify(deviceGroupRepository).delete(group);
    }

    @Test
    void delete_shouldThrowWhenGroupNotFound() {
        when(deviceGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> deviceGroupService.delete(99L));
        verify(deviceGroupMemberRepository, never()).deleteByGroupId(any());
        verify(deviceGroupRepository, never()).delete(any());
    }

    // ==================== listMembers ====================

    @Test
    void listMembers_shouldReturnMembersForGroup() {
        DeviceGroup group = groupWithId(1L);
        when(deviceGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        DeviceGroupMember m1 = DeviceGroupMember.builder().groupId(1L).deviceId(10L).build();
        DeviceGroupMember m2 = DeviceGroupMember.builder().groupId(1L).deviceId(20L).build();
        when(deviceGroupMemberRepository.findByGroupId(1L)).thenReturn(List.of(m1, m2));

        List<DeviceGroupMember> result = deviceGroupService.listMembers(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(deviceGroupRepository).findById(1L);
        verify(deviceGroupMemberRepository).findByGroupId(1L);
    }

    @Test
    void listMembers_shouldThrowWhenGroupNotFound() {
        when(deviceGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> deviceGroupService.listMembers(99L));
        verify(deviceGroupMemberRepository, never()).findByGroupId(any());
    }

    // ==================== addMember ====================

    @Test
    void addMember_shouldSaveWhenNotExists() {
        DeviceGroup group = groupWithId(1L);
        when(deviceGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(deviceGroupMemberRepository.findById(any(DeviceGroupMember.DeviceGroupMemberId.class)))
                .thenReturn(Optional.empty());

        deviceGroupService.addMember(1L, 10L);

        verify(deviceGroupMemberRepository).save(any(DeviceGroupMember.class));
    }

    @Test
    void addMember_shouldNotSaveDuplicate() {
        DeviceGroup group = groupWithId(1L);
        when(deviceGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        DeviceGroupMember existing = DeviceGroupMember.builder().groupId(1L).deviceId(10L).build();
        when(deviceGroupMemberRepository.findById(
                new DeviceGroupMember.DeviceGroupMemberId(1L, 10L)))
                .thenReturn(Optional.of(existing));

        deviceGroupService.addMember(1L, 10L);

        verify(deviceGroupMemberRepository, never()).save(any(DeviceGroupMember.class));
    }

    @Test
    void addMember_shouldThrowWhenGroupNotFound() {
        when(deviceGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> deviceGroupService.addMember(99L, 10L));
        verify(deviceGroupMemberRepository, never()).save(any());
    }

    // ==================== removeMember ====================

    @Test
    void removeMember_shouldDeleteByCompositeId() {
        DeviceGroup group = groupWithId(1L);
        when(deviceGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        deviceGroupService.removeMember(1L, 10L);

        verify(deviceGroupRepository).findById(1L);
        verify(deviceGroupMemberRepository).deleteById(
                new DeviceGroupMember.DeviceGroupMemberId(1L, 10L));
    }

    @Test
    void removeMember_shouldThrowWhenGroupNotFound() {
        when(deviceGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> deviceGroupService.removeMember(99L, 10L));
        verify(deviceGroupMemberRepository, never()).deleteById(any());
    }
}
