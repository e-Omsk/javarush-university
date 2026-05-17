package com.javarush.controller;

import com.javarush.client.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Локальный" Notification Service – здесь же, в этом приложении, чтобы можно было
 * показать обе стороны Pact (Consumer и Provider) в одном модуле.
 *
 * В реальной жизни этот контроллер живёт в другом репозитории / другом сервисе,
 * и Pact Provider-тест запускается там, не здесь.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @PostMapping
    public ResponseEntity<Void> send(@RequestBody NotificationRequest request) {
        log.info("Welcome notification accepted for {}", request.getEmail());
        return ResponseEntity.accepted().build();
    }
}
