package com.javarush.integration;

import com.javarush.client.NotificationServiceClient;
import com.javarush.entity.User;
import com.javarush.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест "внутри сервиса" из лекции:
 *   - реальный Spring-контекст,
 *   - реальный контроллер -> сервис -> репозиторий -> H2,
 *   - "соседний микросервис" NotificationServiceClient – мок (@MockitoBean).
 *
 * Это та самая граница из схемы в HTML: всё зелёное – настоящее, NotificationServiceClient – красный мок.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private NotificationServiceClient notificationClient;

    @Test
    void register_shouldPersistUserAndCallNotifications() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Eve")
                        .param("email", "eve@example.com"))
                .andExpect(status().isOk());

        Optional<User> persisted = userRepository.findByEmail("eve@example.com");
        assertThat(persisted).isPresent();
        verify(notificationClient).sendWelcome(any());
    }

    @Test
    void register_shouldRollbackUserWhenNotificationFails() throws Exception {
        doThrow(new RuntimeException("boom"))
                .when(notificationClient).sendWelcome(any());

        mockMvc.perform(post("/register")
                        .param("name", "Frank")
                        .param("email", "frank@example.com"))
                .andExpect(status().is5xxServerError());

        // Транзакция должна откатиться – пользователя в БД нет.
        assertThat(userRepository.findByEmail("frank@example.com")).isEmpty();
    }
}
