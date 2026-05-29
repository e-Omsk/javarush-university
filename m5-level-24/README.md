# Модуль 5. Уровень 24: API Gateway и Service Discovery

Минимальное демо к лекции [Уровень 24: API Gateway и Service Discovery](docs/ru.javarush.spring.presentation.level24.html).

Две связки:

- **Service Discovery** (Netflix Eureka) — сервисы регистрируются в центральном реестре по логическому имени и находят друг друга, не зная конкретных адресов.
- **API Gateway** (Spring Cloud Gateway, реактивный) — единая точка входа для внешних клиентов. Маршрутизирует `/api/users/**` → `lb://USER-SERVICE`, `/api/products/**` → `lb://PRODUCT-SERVICE`, прикручивает фильтры (`StripPrefix`, `AddRequestHeader`).

В одном модуле живут **четыре** `@SpringBootApplication`:

| Приложение | Порт | Назначение |
|---|---|---|
| `EurekaServerApplication` | `8761` | Service Registry. `@EnableEurekaServer`. Дашборд: http://localhost:8761 |
| `UserServiceApplication` (`USER-SERVICE`) | `8081` | Бизнес-сервис на базе m5-level-06: JPA + H2, `/users`. Eureka-клиент. |
| `ProductServiceApplication` (`PRODUCT-SERVICE`) | `8082` | Бизнес-сервис, in-memory список товаров, `/products`. Eureka-клиент. |
| `ApiGatewayApplication` (`API-GATEWAY`) | `8080` | Spring Cloud Gateway (реактивный). Маршрутизация через `lb://`, фильтры. |

## Что лежит в модуле

```
m5-level-24/
├── pom.xml                                    ← четыре main-class'а; по умолчанию запускается user-service
├── docs/                                      ← материалы вебинара
└── src/main/
    ├── java/com/javarush/
    │   ├── eurekaserver/EurekaServerApplication.java   ← @EnableEurekaServer
    │   ├── apigateway/ApiGatewayApplication.java       ← Spring Cloud Gateway
    │   ├── userservice/
    │   │   ├── UserServiceApplication.java
    │   │   ├── controller/UserController.java          ← /users (REST)
    │   │   ├── service/UserService.java
    │   │   ├── entity/User.java
    │   │   └── repository/UserRepository.java
    │   └── productservice/
    │       ├── ProductServiceApplication.java
    │       ├── controller/ProductController.java       ← /products (REST, in-memory)
    │       └── model/Product.java
    └── resources/
        ├── application.yml      ← общая конфигурация + профили eureka / user / product / gateway
        ├── schema.sql           ← таблица USERS (для user-service)
        └── data.sql             ← seed-записи
```

## Запуск

Нужны **четыре терминала** — каждое приложение поднимается отдельно. Стартовать **строго в порядке**: Eureka → user-service → product-service → Gateway, чтобы клиенты сразу регистрировались в живом реестре, а Gateway знал, куда резолвить `lb://`.

> Все команды запускаются **из корня репозитория**. Префикс `-pl m5-level-24` указывает Maven работать с этим модулем.

### 1. Терминал A — Eureka Server (порт 8761)

```bash
mvn -pl m5-level-24 spring-boot:run \
  -Dspring-boot.run.main-class=com.javarush.eurekaserver.EurekaServerApplication \
  -Dspring-boot.run.profiles=eureka
```

Готовность:
```bash
curl -sf http://localhost:8761/actuator/health && echo " OK"
open http://localhost:8761    # дашборд
```

### 2. Терминал B — user-service (порт 8081)

```bash
mvn -pl m5-level-24 spring-boot:run \
  -Dspring-boot.run.profiles=user
```

(main-class по умолчанию = `UserServiceApplication`, флаг можно опустить.) Через ~30 секунд сервис появится в дашборде Eureka под именем **USER-SERVICE**.

### 3. Терминал C — product-service (порт 8082)

```bash
mvn -pl m5-level-24 spring-boot:run \
  -Dspring-boot.run.main-class=com.javarush.productservice.ProductServiceApplication \
  -Dspring-boot.run.profiles=product
```

### 4. Терминал D — API Gateway (порт 8080)

```bash
mvn -pl m5-level-24 spring-boot:run \
  -Dspring-boot.run.main-class=com.javarush.apigateway.ApiGatewayApplication \
  -Dspring-boot.run.profiles=gateway
```

Gateway тоже регистрируется в Eureka — это нужно, чтобы он знал об **USER-SERVICE** и **PRODUCT-SERVICE** и мог резолвить `lb://USER-SERVICE` → конкретный адрес.

## Пошаговый сценарий демо

### Шаг 0. Поднять все 4 приложения (4 терминала)

См. раздел «Запуск» выше: Eureka → user-service → product-service → Gateway. Между шагами 1 и 4 подождите ~20–30 секунд, чтобы регистрации в Eureka успели уйти.

### Шаг 1. Eureka видит всех клиентов

```bash
curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print('\n'.join(sorted(a['name']+'\t'+a['instance'][0]['status'] for a in d['applications']['application'])))"
```

Ожидаемый вывод:
```
API-GATEWAY      UP
PRODUCT-SERVICE  UP
USER-SERVICE     UP
```

Дашборд: открыть [http://localhost:8761](http://localhost:8761), посмотреть секцию **Instances currently registered with Eureka** — три строки.

### Шаг 2. Прямое обращение к сервисам (минуя Gateway)

#### user-service напрямую — поле "requestSource" покажет "direct"
```bash
curl -s http://localhost:8081/users | jq
```

#### product-service напрямую
```bash
curl -s http://localhost:8082/products | jq
```

В JSON-ответе:
- `"servedByPort": 8081` / `8082` — какой инстанс ответил;
- `"requestSource": "direct"` — заголовок `X-Request-Source` не пришёл (минуем Gateway).

### Шаг 3. Тот же запрос через Gateway (порт 8080)

#### /api/users/** -> StripPrefix=1 -> /users/** -> lb://USER-SERVICE
```bash
curl -s http://localhost:8080/api/users | jq
```
```bash
curl -s http://localhost:8080/api/users/1 | jq
```

#### /api/products/** -> StripPrefix=1 -> /products/** -> lb://PRODUCT-SERVICE
```bash
curl -s http://localhost:8080/api/products | jq
```
```bash
curl -s http://localhost:8080/api/products/2 | jq
```

В ответе:
- `"servedByPort": 8081` / `8082` — Gateway проксировал на тот же сервис;
- `"requestSource": "api-gateway"` — глобальный фильтр `default-filters: AddRequestHeader=X-Request-Source, api-gateway` сработал.

### Шаг 4. Запись через Gateway (POST)

```bash
curl -s -X POST "http://localhost:8080/api/users?name=Dave&email=dave@example.com" | jq
```

#### -> {"id":4,"name":"Dave","email":"dave@example.com"}

#### Проверяем — пользователь сохранён в H2 user-service:
```bash
curl -s http://localhost:8080/api/users | jq '.users[-1]'
```
#### -> {"id":4,"name":"Dave","email":"dave@example.com"}

### Шаг 5. Активные маршруты Gateway (Actuator)

```bash
curl -s http://localhost:8080/actuator/gateway/routes | jq
```

Ожидаемый вывод — два маршрута `user_service_route` (`lb://USER-SERVICE`) и `product_service_route` (`lb://PRODUCT-SERVICE`), у каждого фильтры `AddRequestHeader` + `StripPrefix`.

### Шаг 6 (опционально). Что произойдёт, если убить сервис

В терминале B нажать `Ctrl+C` (остановить user-service). Подождать ~90 секунд (Eureka вычеркнет инстанс по lease expiration) и повторить:

```bash
curl -i http://localhost:8080/api/users
```

#### -> HTTP/1.1 503 Service Unavailable

Gateway не нашёл ни одного живого инстанса `USER-SERVICE` — это и есть «отказоустойчивая» часть Service Discovery. Поднимаем user-service обратно — Gateway сам подхватит без перезапуска.

### H2-консоль (для user-service)

```
http://localhost:8081/h2-console
  JDBC URL: jdbc:h2:mem:testdb
  user:     sa
  password: (пусто)
```

## Что должно работать в итоге

1. **Eureka-дашборд** показывает три зарегистрированных приложения: `USER-SERVICE`, `PRODUCT-SERVICE`, `API-GATEWAY`.
2. Запрос на `http://localhost:8080/api/users` возвращает данные из user-service: в JSON поле `"servedByPort": 8081` (откуда ответили) и `"requestSource": "api-gateway"` (Gateway добавил заголовок глобальным фильтром).
3. Если остановить user-service, Eureka через ~90 секунд выкинет его из реестра, и Gateway начнёт отвечать `503` — это и есть «отказоустойчивая» часть Service Discovery.

## Балансировка нагрузки (опционально)

Чтобы увидеть `lb://` в работе, поднимите второй экземпляр user-service на другом порту:

```bash
SERVER_PORT=8181 mvn -pl m5-level-24 spring-boot:run \
  -Dspring-boot.run.profiles=user \
  -Dspring-boot.run.jvmArguments="-Dserver.port=8181"
```

После регистрации второго инстанса повторно дёргайте `curl -s http://localhost:8080/api/users | jq '.servedByPort'` — поле будет чередоваться между `8081` и `8181`, что и есть встроенный round-robin Spring Cloud LoadBalancer.

## Архитектура (упрощённая схема из лекции)

```
                              Eureka Server
                              (порт 8761)
                             ▲     ▲     ▲
                  register   │     │     │   register
                             │     │     │
                 ┌───────────┘     │     └────────────┐
                 │                 │                  │
            USER-SERVICE     PRODUCT-SERVICE      API-GATEWAY
            (порт 8081)       (порт 8082)         (порт 8080)
                 ▲                 ▲                  ▲
                 │                 │                  │
                 └──── lb:// ──────┴──── lb:// ───────┤
                                                      │ HTTP
                                                      │
                                                  Клиент
```

## Что осталось за кадром

- **Auth/JWT в Gateway**. В лекции упомянуто, что шлюз обычно валидирует токены и пробрасывает claim'ы дальше. Здесь мы добавляем только демонстрационный заголовок `X-Request-Source: api-gateway` через `default-filters` — этого достаточно, чтобы показать, как фильтры работают. Безопасность подробно разбирается в более ранних уровнях модуля 5.
- **Rate limiting / Circuit Breaker**. Spring Cloud Gateway умеет это «из коробки» (`RequestRateLimiter`, `Resilience4J`), но для базового демо достаточно `AddRequestHeader` + `StripPrefix`.
- **Авто-маршрутизация по именам сервисов** (`spring.cloud.gateway.server.webflux.discovery.locator.enabled=true`). Намеренно выключена — явные `routes:` с `Path=` предикатами понятнее и точно соответствуют примеру из лекции.
- **Совместимость с Spring Cloud Gateway 5.x**. В лекции используются старые имена: артефакт `spring-cloud-starter-gateway` и YAML-префикс `spring.cloud.gateway.routes`. В BOM Spring Cloud 2025.1.x они переехали — артефакт стал `spring-cloud-starter-gateway-server-webflux`, а свойства живут под `spring.cloud.gateway.server.webflux.*`. Семантика та же. Если будете писать руками — берите новые имена, иначе Maven не найдёт зависимость, а YAML-свойства будут проигнорированы.
- **Eureka-кластер**. В лекции упомянута отказоустойчивость через несколько копий реестра. Здесь — один экземпляр, потому что демо.

## References

1. Заготовка проекта взята из [m5-level-06](../m5-level-06/README.md) (Spring Data JPA + H2).
2. Паттерн «несколько `@SpringBootApplication` в одном модуле» — как в [m5-level-23](../m5-level-23/README.md) (Spring Cloud Config).
