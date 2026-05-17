package com.javarush.repository;

import com.javarush.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Срез слоя данных: JPA + встроенная H2.
 * В лекции на этом месте обычно стоит Testcontainers с настоящим PostgreSQL,
 * но без Docker мы ограничены H2 – база отличается, поэтому "критичные" SQL-фичи
 * лучше проверять руками или интеграционным тестом против реальной БД.
 */
@DataJpaTest
class UserRepositoryDataJpaTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_shouldReturnUserWhenExists() {
        userRepository.save(new User("Dan", "dan@example.com"));

        Optional<User> found = userRepository.findByEmail("dan@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Dan");
    }

    @Test
    void findByEmail_shouldReturnEmptyWhenMissing() {
        assertThat(userRepository.findByEmail("nope@example.com")).isEmpty();
    }
}
