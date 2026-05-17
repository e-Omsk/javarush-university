package com.javarush.service;

import com.javarush.client.NotificationRequest;
import com.javarush.client.NotificationServiceClient;
import com.javarush.entity.User;
import com.javarush.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тест регистрации: мокаем И репозиторий, И клиент к внешнему сервису.
 * Проверяем: 1) пользователь сохранён, 2) уведомление отправлено с правильным телом.
 *
 * Слабая сторона такого теста – он ничего не знает про настоящий формат API нотификаций.
 * Эту проблему решает контрактный тест (см. NotificationConsumerPactTest).
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationServiceClient notificationClient;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registerUser_shouldPersistAndNotify() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        User saved = registrationService.registerUser("John", "john@example.com");

        assertThat(saved.getId()).isEqualTo(42L);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).sendWelcome(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(captor.getValue().getUserName()).isEqualTo("John");
    }

    @Test
    void registerUser_shouldPropagateClientFailure() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("notification-service down"))
                .when(notificationClient).sendWelcome(any());

        assertThatThrownBy(() ->
                registrationService.registerUser("John", "john@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("notification-service down");
    }
}
