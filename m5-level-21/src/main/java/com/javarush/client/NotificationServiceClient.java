package com.javarush.client;

/**
 * Клиент к внешнему Notification Service.
 * В тестах подменяется через @MockitoBean — это и есть та самая "граница интеграционного теста"
 * из лекции: всё внутри сервиса реальное, а сетевой соседний сервис мокается.
 */
public interface NotificationServiceClient {

    void sendWelcome(NotificationRequest request);

}
