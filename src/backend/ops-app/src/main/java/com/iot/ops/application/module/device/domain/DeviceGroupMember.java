package com.iot.ops.application.module.device.domain;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "device_group_members")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@IdClass(DeviceGroupMember.DeviceGroupMemberId.class)
public class DeviceGroupMember {

    @Id
    private Long groupId;

    @Id
    private Long deviceId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceGroupMemberId implements Serializable {
        private Long groupId;
        private Long deviceId;
    }
}
