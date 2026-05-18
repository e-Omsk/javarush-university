package com.javarush.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Микросервис greeting-service — Config Client. Тянет свойства
 * с Config Server (см. application.yml: spring.config.import) и хранит
 * историю посещений в H2 (как и в m5-level-06).
 *
 * Запуск (по умолчанию):
 *   mvn spring-boot:run
 *
 * Запуск с профилем prod (получит greeting-service-prod.yml с сервера):
 *   mvn spring-boot:run -Dspring-boot.run.profiles=prod
 */
@SpringBootApplication(scanBasePackages = "com.javarush.client")
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
}
