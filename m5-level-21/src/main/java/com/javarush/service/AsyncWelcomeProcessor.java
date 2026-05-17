package com.javarush.service;

import com.javarush.entity.WelcomeLogEntry;
import com.javarush.repository.WelcomeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Асинхронный кусок: имитируем фоновую обработку welcome-нотификации.
 *
 * Метод возвращает управление сразу, а запись в WELCOME_LOG появляется
 * с задержкой – на этом и строится демо для Awaitility.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncWelcomeProcessor {

    private final WelcomeLogRepository welcomeLogRepository;

    @Async
    public void process(String email) {
        try {
            // Чтобы тест без Awaitility гарантированно падал.
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        welcomeLogRepository.save(new WelcomeLogEntry(email));
        log.info("Async welcome processed for {}", email);
    }
}
