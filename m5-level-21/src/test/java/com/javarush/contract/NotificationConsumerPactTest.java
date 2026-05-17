package com.javarush.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.javarush.client.NotificationRequest;
import com.javarush.client.RestNotificationServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Сторона потребителя (Consumer):
 *   1) описываем наши ожидания от Notification Service (что шлём и что хотим получить),
 *   2) Pact поднимает mock-HTTP-сервер на этом ожидании,
 *   3) наш реальный клиент стучится в mock и должен отработать без ошибок,
 *   4) Pact сохраняет JSON-контракт в target/pacts.
 *
 * Этот контракт потом проверяется в NotificationProviderPactTest.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "notification-service", pactVersion = PactSpecVersion.V3)
class NotificationConsumerPactTest {

    @Pact(consumer = "user-service")
    public RequestResponsePact welcomeNotificationPact(PactDslWithProvider builder) {
        PactDslJsonBody requestBody = new PactDslJsonBody()
                .stringType("email", "alice@example.com")
                .stringType("userName", "Alice");

        return builder
                .given("notification service is available")
                .uponReceiving("welcome notification for a new user")
                    .path("/api/notifications")
                    .method("POST")
                    .headers("Content-Type", "application/json")
                    .body(requestBody)
                .willRespondWith()
                    .status(202)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "welcomeNotificationPact")
    void shouldSendWelcomeNotification(MockServer mockServer) {
        // Берём не Spring-бин, а новый клиент, нацеленный на адрес mock-сервера Pact.
        RestNotificationServiceClient client = new RestNotificationServiceClient(mockServer.getUrl());

        // Если клиент пошлёт что-то не то – mock-сервер вернёт 500, и тест упадёт.
        client.sendWelcome(new NotificationRequest("alice@example.com", "Alice"));
    }
}
