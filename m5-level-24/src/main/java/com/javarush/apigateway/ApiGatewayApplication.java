package com.javarush.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * API Gateway — единая точка входа для всех внешних запросов.
 * Поднимается на порту 8080, регистрируется в Eureka, маршруты описаны
 * декларативно в application.yml (профиль gateway).
 *
 * Запуск:
 *   mvn spring-boot:run \
 *     -Dspring-boot.run.main-class=com.javarush.apigateway.ApiGatewayApplication \
 *     -Dspring-boot.run.profiles=gateway
 *
 * Реактивный (на WebFlux). Так как в classpath есть и spring-boot-starter-webmvc,
 * и spring-boot-starter-webflux, профиль gateway явно выставляет
 * spring.main.web-application-type=reactive — иначе Spring Boot по умолчанию
 * выбрал бы сервлет-стэк, и Gateway не запустился бы.
 *
 * JPA/DataSource выключаем — шлюзу БД не нужна.
 */
@SpringBootApplication(
        scanBasePackages = "com.javarush.apigateway",
        exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class}
)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
