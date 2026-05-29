package com.javarush.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration;

/**
 * product-service — второй бизнес-микросервис, специально упрощён до
 * in-memory списка. Нужен, чтобы у Gateway было два разных маршрута
 * (/api/users/** и /api/products/**) и было видно, как Service Discovery
 * + lb:// направляют запросы на разные сервисы.
 *
 * Запуск:
 *   mvn spring-boot:run \
 *     -Dspring-boot.run.main-class=com.javarush.productservice.ProductServiceApplication \
 *     -Dspring-boot.run.profiles=product
 *
 * JPA/DataSource выключаем — этому сервису БД не нужна, а на classpath
 * она есть из-за user-service.
 */
@SpringBootApplication(
        scanBasePackages = "com.javarush.productservice",
        exclude = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                GatewayClassPathWarningAutoConfiguration.class
        }
)
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
