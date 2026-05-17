package com.javarush.async;

import com.javarush.entity.WelcomeLogEntry;
import com.javarush.repository.WelcomeLogRepository;
import com.javarush.service.AsyncWelcomeProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Демонстрация проблемы из лекции: проверять асинхронный результат "сразу"
 * почти всегда означает упасть. Awaitility даёт нам "подожди, пока...".
 */
@SpringBootTest
class AsyncWelcomeAwaitilityTest {

    @Autowired
    private AsyncWelcomeProcessor asyncWelcomeProcessor;

    @Autowired
    private WelcomeLogRepository welcomeLogRepository;

    @Test
    void processShouldEventuallyWriteToLog() {
        String email = "async-" + System.nanoTime() + "@example.com";

        asyncWelcomeProcessor.process(email);

        // Так делать не надо – почти наверняка упадёт, потому что @Async
        // возвращает управление до того, как обработка действительно завершится:
        //
        //   assertThat(welcomeLogRepository.findByEmail(email)).isPresent();

        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    Optional<WelcomeLogEntry> entry = welcomeLogRepository.findByEmail(email);
                    assertThat(entry).isPresent();
                    assertThat(entry.get().getProcessedAt()).isNotNull();
                });
    }
}
