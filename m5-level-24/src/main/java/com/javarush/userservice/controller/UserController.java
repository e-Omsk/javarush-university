package com.javarush.userservice.controller;

import com.javarush.userservice.entity.User;
import com.javarush.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API user-service. Базовый путь /users — фронтенд через Gateway
 * ходит на /api/users/**, а фильтр StripPrefix=1 в маршруте убирает
 * сегмент /api, и сюда приходит уже /users/...
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Порт пишем в ответ, чтобы было видно, какой инстанс ответил —
     * пригодится, когда поднимем второй экземпляр user-service для демонстрации
     * балансировки нагрузки Spring Cloud LoadBalancer.
     */
    @Value("${server.port}")
    private int port;

    @GetMapping
    public Map<String, Object> getAll(@RequestHeader(value = "X-Request-Source", required = false) String source) {
        return Map.of(
                "servedByPort", port,
                "requestSource", source == null ? "direct" : source,
                "users", userService.getAllUsers()
        );
    }

    @GetMapping("/{id}")
    public User getOne(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PostMapping
    public User create(@RequestParam String name, @RequestParam String email) {
        return userService.createUser(name, email);
    }
}
