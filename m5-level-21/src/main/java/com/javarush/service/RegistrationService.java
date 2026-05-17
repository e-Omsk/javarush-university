package com.javarush.service;

import com.javarush.client.NotificationRequest;
import com.javarush.client.NotificationServiceClient;
import com.javarush.entity.User;
import com.javarush.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Регистрация пользователя:
 *   1) сохраняем User в БД,
 *   2) дергаем внешний Notification Service.
 *
 * Если Notification Service упал — мы тоже падаем и откатываемся.
 * Это сознательный выбор для демо: иначе нечего проверять в интеграционном тесте.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final NotificationServiceClient notificationClient;

    @Transactional
    public User registerUser(String name, String email) {
        User saved = userRepository.save(new User(name, email));
        log.info("User {} saved, sending welcome", email);

        // Вот эту строчку в интеграционных тестах будем мокать через @MockitoBean —
        // именно тут проходит граница нашего сервиса.
        notificationClient.sendWelcome(new NotificationRequest(email, name));

        return saved;
    }
}
