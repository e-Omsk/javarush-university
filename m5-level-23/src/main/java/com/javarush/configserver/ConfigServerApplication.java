package com.javarush.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Server. Поднимается на порту 8888 и отдаёт .yml-конфиги
 * микросервисам по REST API (см. http://localhost:8888/greeting-service/default).
 *
 * Запуск:
 *   mvn spring-boot:run \
 *     -Dspring-boot.run.main-class=com.javarush.configserver.ConfigServerApplication \
 *     -Dspring-boot.run.profiles=server
 *
 * scanBasePackages ограничен текущим пакетом — иначе при сканировании
 * подтянулись бы JPA-репозитории клиента из соседнего пакета.
 *
 * DataSource/JPA автоконфигурацию выключаем явно: на classpath лежит H2 и data-jpa
 * (нужны клиенту), но Config Server'у база не нужна и поднимать её ни к чему.
 */
@SpringBootApplication(
        scanBasePackages = "com.javarush.configserver",
        exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class}
)
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
