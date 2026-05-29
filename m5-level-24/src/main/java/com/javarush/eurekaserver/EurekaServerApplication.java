package com.javarush.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server — "телефонная книга" микросервисов (Service Registry).
 * Поднимается на порту 8761 и принимает регистрации от Eureka-клиентов.
 * Дашборд: http://localhost:8761
 *
 * Запуск:
 *   mvn spring-boot:run \
 *     -Dspring-boot.run.main-class=com.javarush.eurekaserver.EurekaServerApplication \
 *     -Dspring-boot.run.profiles=eureka
 *
 * scanBasePackages ограничен текущим пакетом — иначе при сканировании подтянулись
 * бы JPA-репозитории user-service и контроллеры product-service.
 *
 * DataSource/JPA автоконфигурацию выключаем явно: на classpath лежит H2 и data-jpa
 * (нужны user-service), но реестру база не нужна и поднимать её ни к чему.
 */
@SpringBootApplication(
        scanBasePackages = "com.javarush.eurekaserver",
        // GatewayClassPathWarningAutoConfiguration жёстко падает, если на classpath
        // одновременно лежат webmvc и webflux (наш случай — gateway требует webflux,
        // остальные приложения — webmvc). У не-gateway приложений эту автоконфигурацию
        // выключаем явно. Сам gateway-профиль ставит web-application-type=reactive
        // и проверку проходит.
        exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                GatewayClassPathWarningAutoConfiguration.class
        }
)
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
