package com.javarush.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Боевая реализация клиента к Notification Service.
 * URL берём из настроек, в проде указывал бы на реальный сервис,
 * в demo-режиме указывает на свой же контроллер /api/notifications.
 *
 * В Pact Consumer тесте URL подменяется на адрес mock-сервера Pact.
 */
@Slf4j
@Component
public class RestNotificationServiceClient implements NotificationServiceClient {

    private final RestClient restClient;

    public RestNotificationServiceClient(
            @Value("${notification.service.url:http://localhost:8080}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public void sendWelcome(NotificationRequest request) {
        log.debug("Calling notification-service for {}", request.getEmail());
        restClient.post()
                .uri("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
