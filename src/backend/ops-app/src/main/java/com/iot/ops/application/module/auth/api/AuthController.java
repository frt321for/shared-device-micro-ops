package com.iot.ops.application.module.auth.api;

import com.iot.ops.application.infra.security.JwtFilter;
import com.iot.ops.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String ADMIN_PASSWORD_HASH = new BCryptPasswordEncoder().encode("admin123");

    private final PasswordEncoder passwordEncoder;

    public AuthController(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if ("admin".equals(username) && passwordEncoder.matches(password, ADMIN_PASSWORD_HASH)) {
            String role = "admin";
            String token = JwtFilter.generateToken(username, role);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "token", token,
                "username", username,
                "role", role
            )));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(401, "Invalid username or password"));
    }
}
