package com.javarush.service;

import com.javarush.entity.User;
import com.javarush.exception.BusinessException;
import com.javarush.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тест: класс UserService проверяем в изоляции, репозиторий мокается.
 * Никакого Spring-контекста, никакой БД – на порядки быстрее интеграционных тестов.
 * Это и есть фундамент пирамиды.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void updateUserEmails_shouldUpdateBothUsers() {
        User alice = new User(1L, "Alice", "alice@example.com");
        User bob = new User(2L, "Bob", "bob@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserEmails(1L, 2L, "alice.new@example.com", "bob.new@example.com");

        assertThat(alice.getEmail()).isEqualTo("alice.new@example.com");
        assertThat(bob.getEmail()).isEqualTo("bob.new@example.com");
        verify(userRepository).save(alice);
        verify(userRepository).save(bob);
    }

    @Test
    void updateUserEmails_shouldFailIfFirstUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.updateUserEmails(99L, 2L, "a@a", "b@b"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found: 99");
    }

    @Test
    void updateUserEmailWithChecked_shouldThrowBusinessException() {
        User alice = new User(1L, "Alice", "alice@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> userService.updateUserEmailWithChecked(1L, "x@y.com"))
                .isInstanceOf(BusinessException.class);

        // Сохранение всё равно произошло – исключение выбрасывается ПОСЛЕ save.
        verify(userRepository).save(alice);
    }

    @Test
    void getAllUsers_shouldReturnRepoContent() {
        when(userRepository.findAll()).thenReturn(List.of(
                new User(1L, "Alice", "alice@example.com")));

        assertThat(userService.getAllUsers()).hasSize(1);
    }
}
