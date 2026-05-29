package com.javarush.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration;

/**
 * user-service — бизнес-микросервис на основе m5-level-06.
 * Хранит пользователей в H2, регистрируется в Eureka под именем USER-SERVICE.
 *
 * Запуск (это main-class по умолчанию):
 *   mvn spring-boot:run -Dspring-boot.run.profiles=user
 *
 * Eureka-клиент включается автоматически при наличии
 * spring-cloud-starter-netflix-eureka-client на classpath.
 *
 * GatewayClassPathWarningAutoConfiguration исключаем — см. подробный комментарий
 * в EurekaServerApplication.
 */
@SpringBootApplication(
        scanBasePackages = "com.javarush.userservice",
        exclude = GatewayClassPathWarningAutoConfiguration.class
)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
