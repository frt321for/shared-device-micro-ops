package com.iot.ops.application.infra.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    ADMIN("ADMIN", "系统管理员"),
    MANAGER("MANAGER", "运营经理"),
    REPLENISHER("REPLENISHER", "补货员"),
    MAINTAINER("MAINTAINER", "维护员"),
    WAREHOUSE_KEEPER("WAREHOUSE_KEEPER", "仓管员");

    private final String value;
    private final String displayName;
}
