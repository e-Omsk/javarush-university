package com.javarush.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Сторона поставщика (Provider):
 *   1) поднимаем своё приложение целиком (NotificationController должен отвечать),
 *   2) читаем JSON-контракт, сгенерированный консьюмером (target/pacts),
 *   3) Pact реально стучит по HTTP в наш сервис и сверяет ответы с тем, что записано в контракте.
 *
 * Если NotificationController вернёт не 202, или захочет другой формат тела –
 * provider-тест упадёт, и breaking change не попадёт в продакшн.
 *
 * В реальной жизни этот тест жил бы в репозитории notification-service. Тут он рядом
 * только для демонстрации полного цикла в одном модуле.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("notification-service")
@PactFolder("target/pacts")
class NotificationProviderPactTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPactsAgainstProvider(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("notification service is available")
    void notificationServiceIsAvailable() {
        // Состояния нужны, когда провайдер должен подготовить данные перед запросом.
        // Здесь готовить нечего – контроллер просто принимает запрос.
    }
}
