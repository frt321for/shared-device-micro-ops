package com.iot.ops.application.module.auth.api;

import com.iot.ops.application.infra.cache.SessionCache;
import com.iot.ops.application.infra.security.JwtFilter;
import com.iot.ops.application.module.auth.domain.User;
import com.iot.ops.application.module.auth.repository.UserRepository;
import com.iot.ops.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionCache sessionCache;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "用户名和密码不能为空"));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "用户名或密码错误"));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "用户名或密码错误"));
        }

        String token = JwtFilter.generateToken(username, user.getRole());
        sessionCache.set(token, username, 86400);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "token", token,
            "username", username,
            "displayName", user.getDisplayName(),
            "role", user.getRole()
        )));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            sessionCache.remove(authHeader.substring(7));
        }
        return ResponseEntity.ok(ApiResponse.success("已登出"));
    }

    @GetMapping("/users")
    public ApiResponse<List<User>> listUsers() {
        return ApiResponse.success(userRepository.findAll());
    }
}
