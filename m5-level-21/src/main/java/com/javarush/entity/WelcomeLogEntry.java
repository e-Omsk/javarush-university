package com.javarush.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Запись о том, что асинхронный обработчик welcome-нотификации отработал.
 * Нужна для демонстрации Awaitility: тест ждёт появления строки в таблице.
 */
@Entity
@Table(name = "WELCOME_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public WelcomeLogEntry(String email) {
        this.email = email;
        this.processedAt = Instant.now();
    }
}
