package com.javarush.controller;

import com.javarush.entity.User;
import com.javarush.service.AsyncWelcomeProcessor;
import com.javarush.service.RegistrationService;
import com.javarush.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Срез веб-слоя: поднимаем только MVC-инфраструктуру, сервисы – моки.
 * Сюда же относится "интеграционный тест внутри сервиса" из лекции – проверяем
 * связку контроллер ↔ сериализация ↔ маршрутизация, но без БД и без других сервисов.
 */
@WebMvcTest(HelloController.class)
class HelloControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private AsyncWelcomeProcessor asyncWelcomeProcessor;

    @Test
    void rootShouldReturnHello() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World!"));
    }

    @Test
    void getUsersShouldReturnJsonList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(
                new User(1L, "Alice", "alice@example.com"),
                new User(2L, "Bob", "bob@example.com")
        ));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));
    }

    @Test
    void registerShouldDelegateToService() throws Exception {
        when(registrationService.registerUser("John", "john@example.com"))
                .thenReturn(new User(42L, "John", "john@example.com"));

        mockMvc.perform(post("/register")
                        .param("name", "John")
                        .param("email", "john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));

        verify(registrationService).registerUser(eq("John"), eq("john@example.com"));
    }
}
