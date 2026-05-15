package com.iot.ops.application.infra.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

public final class UserContext {

    private static final ThreadLocal<UserInfo> holder = new ThreadLocal<>();

    public static void setCurrentUser(String username, String role) {
        holder.set(new UserInfo(username, role));
    }

    public static UserInfo getCurrentUser() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private final String username;
        private final String role;
    }
}
