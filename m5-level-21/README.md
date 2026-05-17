# Модуль 5. Уровень 21: Тестирование микросервисов

Демо к лекции [Уровень 21: Тестирование микросервисов](docs/ru.javarush.spring.presentation.level21.html).
Базируется на инфраструктуре `m5-level-06` (Spring Boot 4, встроенная H2), Docker не используется.

## Что показано

| Тема из лекции | Где смотреть |
|---|---|
| Unit-тесты (JUnit 5 + Mockito) | `src/test/.../service/UserServiceUnitTest.java`, `RegistrationServiceUnitTest.java` |
| @WebMvcTest – срез веб-слоя, сервисы мокаются | `src/test/.../controller/HelloControllerWebMvcTest.java` |
| @DataJpaTest – срез слоя данных, H2 | `src/test/.../repository/UserRepositoryDataJpaTest.java` |
| Интеграционный тест "внутри сервиса", `@MockitoBean` для соседнего сервиса | `src/test/.../integration/RegistrationIntegrationTest.java` |
| Контрактное тестирование (Pact) – Consumer | `src/test/.../contract/NotificationConsumerPactTest.java` |
| Контрактное тестирование (Pact) – Provider | `src/test/.../contract/NotificationProviderPactTest.java` |
| Тестирование `@Async` через Awaitility | `src/test/.../async/AsyncWelcomeAwaitilityTest.java` |

## Как устроен проект

```
HelloController       ── REST API (см. эндпоинты ниже)
NotificationController ── "локальный" Notification Service (POST /api/notifications)

UserService           ── @Transactional методы из m5-level-06
RegistrationService   ── сохраняет User + дёргает NotificationServiceClient
AsyncWelcomeProcessor ── @Async, пишет в WELCOME_LOG (для Awaitility)

NotificationServiceClient        ── интерфейс к внешнему сервису нотификаций
RestNotificationServiceClient    ── боевая реализация на RestClient (URL из application.properties)
```

В демо `NotificationController` живёт здесь же – чтобы можно было показать обе стороны Pact (Consumer + Provider) в одном модуле. В реальной жизни они в разных репозиториях.

## Запуск приложения

```bash
mvn -pl m5-level-21 spring-boot:run
```

Приложение поднимается на `:8080`. H2-консоль: http://localhost:8080/h2-console
(JDBC URL: `jdbc:h2:mem:testdb`, user `sa`, без пароля)

## Запуск тестов

```bash
mvn -pl m5-level-21 test
```

Сурфайр настроен на `runOrder=alphabetical`, чтобы `NotificationConsumerPactTest` отработал раньше `NotificationProviderPactTest` – первый генерирует JSON-контракт в `target/pacts/`, второй его проверяет.

Запустить только один срез:
```bash
mvn -pl m5-level-21 test -Dtest=UserServiceUnitTest
mvn -pl m5-level-21 test -Dtest=RegistrationIntegrationTest
mvn -pl m5-level-21 test -Dtest=NotificationConsumerPactTest
mvn -pl m5-level-21 test -Dtest=AsyncWelcomeAwaitilityTest
```

После прогона Pact Consumer-теста контракт лежит тут:
```
m5-level-21/target/pacts/user-service-notification-service.json
```

## REST API и curl-команды

### Hello

```bash
curl http://localhost:8080/
```

### Получить всех пользователей

```bash
curl -s http://localhost:8080/users | jq .
```

### Регистрация пользователя (UserRepository + NotificationServiceClient)

```bash
curl -X POST "http://localhost:8080/register?name=John&email=john@example.com"
```
Сервис сохранит пользователя в БД и отправит POST на `/api/notifications` (то есть сам в себя).
Если NotificationController упадёт – транзакция в `RegistrationService` откатится (это проверяется в `RegistrationIntegrationTest.register_shouldRollbackUserWhenNotificationFails`).

### Обновление email двух пользователей в одной транзакции

```bash
curl -X POST "http://localhost:8080/users/update-emails?id1=1&id2=2&email1=alice.new@example.com&email2=bob.new@example.com"
```

### Обновление с checked-исключением после save (для иллюстрации поведения транзакций)

```bash
curl -X POST "http://localhost:8080/users/1/email?email=alice.default@example.com"
```

### Async-обработчик (для Awaitility)

```bash
curl -X POST "http://localhost:8080/async-welcome?email=async@example.com"
```
Эндпоинт возвращает `202 Accepted` мгновенно. Запись в таблице `WELCOME_LOG` появится через ~300 мс.
Проверить:
```bash
# В H2-консоли:
SELECT * FROM WELCOME_LOG;
```

### Notification Service (Pact Provider)

```bash
curl -X POST http://localhost:8080/api/notifications \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","userName":"Alice"}'
```
Возвращает `202 Accepted` – ровно то, что записано в Pact-контракте.

## Что осталось за кадром и почему

Часть тем из лекции мы здесь не показали – ниже отдельно по каждой:

- **Testcontainers с PostgreSQL/Kafka.** Требует Docker, который мы договорились не использовать. Внутри `@DataJpaTest` мы заменили реальный PostgreSQL на встроенную H2. В реальном проекте критичные SQL-фичи (JSONB, оконные функции, специфичные типы) на H2 не проверишь – нужен либо Testcontainers, либо отдельный интеграционный стенд.
- **Интеграция с Kafka и Awaitility поверх асинхронной шины.** Брокер опять же требует Docker. Чтобы все-таки показать саму идею "результат появится с задержкой", мы оставили внутри JVM `@Async`-обработчик – Awaitility-тест работает один в один.
- **End-to-End тесты с `DockerComposeContainer`.** То же ограничение: для запуска двух+ сервисов одновременно нужен Docker. В качестве "хвоста" пирамиды у нас работает `RegistrationIntegrationTest` – он самый дорогой в этом модуле.
- **Pact Broker и `can-i-deploy`.** Брокер обычно поднимают рядом с CI/CD (docker-образ `pactfoundation/pact-broker`). Без брокера контракт всё равно генерируется и проверяется – просто через файл в `target/pacts/`, а не через сетевой обмен версиями.

## Структура

```
m5-level-21/
├── docs/
│   └── ru.javarush.spring.presentation.level21.html   ← материалы вебинара
├── src/main/java/com/javarush/
│   ├── Main.java
│   ├── config/AsyncConfig.java
│   ├── controller/
│   │   ├── HelloController.java
│   │   └── NotificationController.java
│   ├── entity/
│   │   ├── User.java
│   │   └── WelcomeLogEntry.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── WelcomeLogRepository.java
│   ├── client/
│   │   ├── NotificationServiceClient.java
│   │   ├── NotificationRequest.java
│   │   └── RestNotificationServiceClient.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── RegistrationService.java
│   │   └── AsyncWelcomeProcessor.java
│   └── exception/BusinessException.java
├── src/main/resources/
│   ├── application.properties
│   ├── schema.sql
│   └── data.sql
└── src/test/java/com/javarush/
    ├── async/AsyncWelcomeAwaitilityTest.java
    ├── contract/
    │   ├── NotificationConsumerPactTest.java
    │   └── NotificationProviderPactTest.java
    ├── controller/HelloControllerWebMvcTest.java
    ├── integration/RegistrationIntegrationTest.java
    ├── repository/UserRepositoryDataJpaTest.java
    └── service/
        ├── UserServiceUnitTest.java
        └── RegistrationServiceUnitTest.java
```

## Заметки

- В `data.sql` используется `MERGE INTO USERS KEY (email)` вместо `INSERT`. Причина: in-memory H2 с именем `testdb` переживает между `@SpringBootTest`-контекстами, и обычный `INSERT` падал бы на `UNIQUE` при втором запуске.
- Pact Provider-тест по факту проверяет наш же `NotificationController` – это сделано для демонстрации полного цикла в одном модуле. В реальной жизни этот тест жил бы в репозитории notification-service.
- Spring Boot 4 переименовал тестовые автоконфиги: `@WebMvcTest` теперь в `org.springframework.boot.webmvc.test.autoconfigure`, `@DataJpaTest` – в `org.springframework.boot.data.jpa.test.autoconfigure`. И `@MockBean` заменён на `@MockitoBean` из `org.springframework.test.context.bean.override.mockito`.
