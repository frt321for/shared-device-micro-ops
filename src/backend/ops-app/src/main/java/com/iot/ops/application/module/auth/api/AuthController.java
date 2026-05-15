package com.iot.ops.application.module.auth.api;

import com.iot.ops.application.infra.security.JwtFilter;
import com.iot.ops.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "admin");
        String role = "admin";
        String token = JwtFilter.generateToken(username, role);
        return ApiResponse.success(Map.of(
            "token", token,
            "username", username,
            "role", role
        ));
    }
}
