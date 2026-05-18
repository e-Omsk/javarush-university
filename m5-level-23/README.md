# Модуль 5. Уровень 23: Управление конфигурациями (Spring Cloud Config)

Минимальное демо к лекции [Уровень 23: Управление конфигурациями и секретами](docs/ru.javarush.spring.presentation.level23.html).

Покрываем часть **«централизованная конфигурация»**: Config Server отдаёт `.yml` микросервису, профили переключают значения, `@RefreshScope` + `/actuator/refresh` дают горячее обновление без перезапуска. Интеграция с **HashiCorp Vault** в код не вынесена — про неё в конце README.

В одном модуле живут **два** `@SpringBootApplication`:

| Приложение | Порт | Назначение |
|---|---|---|
| `ConfigServerApplication` | `8888` | Spring Cloud Config Server. Источник конфигов — папка `config-repo/` (профиль `native`) |
| `ClientApplication` (`greeting-service`) | `8080` | Микросервис-клиент. Тянет свойства с Config Server, пишет визиты в H2 |

## Что лежит в модуле

```
m5-level-23/
├── pom.xml                       ← два main-class'а, по умолчанию запускается клиент
├── config-repo/                  ← "Git-репозиторий" Config Server'а (читается с диска)
│   ├── greeting-service.yml      ← дефолтный профиль
│   ├── greeting-service-dev.yml  ← профиль dev: зелёный баннер, история включена
│   └── greeting-service-prod.yml ← профиль prod: красный баннер, история выключена
├── docs/                         ← материалы вебинара
└── src/main/
    ├── java/com/javarush/
    │   ├── configserver/ConfigServerApplication.java   ← @EnableConfigServer
    │   └── client/
    │       ├── ClientApplication.java
    │       ├── controller/GreetingController.java
    │       ├── service/GreetingService.java            ← @RefreshScope + @Value
    │       ├── entity/Visit.java
    │       └── repository/VisitRepository.java
    └── resources/
        ├── application.yml       ← spring.config.import=configserver:... + профиль server
        ├── schema.sql            ← таблица VISITS
        └── data.sql              ← seed-запись
```

## Запуск

Нужны **два терминала** — Config Server и клиент работают параллельно.

### 1. Терминал A — Config Server (порт 8888)

Запускать **из директории модуля**, чтобы относительный путь `file:./config-repo/` разрешился:

```bash
cd m5-level-23
mvn spring-boot:run \
  -Dspring-boot.run.main-class=com.javarush.configserver.ConfigServerApplication \
  -Dspring-boot.run.profiles=server,native
```

Профиль `server` активирует секцию из `application.yml` (порт `8888`, `search-locations: file:./config-repo/`). Профиль `native` встроен в Spring Cloud Config и переключает источник конфигурации с Git на файловую систему.

Проверяем, что сервер отдаёт конфиги:

```bash
# Дефолтный профиль
curl -s http://localhost:8888/greeting-service/default | jq

# Профиль dev
curl -s http://localhost:8888/greeting-service/dev | jq

# Профиль prod
curl -s http://localhost:8888/greeting-service/prod | jq
```

### 2. Терминал B — клиент `greeting-service` (порт 8080)

По умолчанию (без профиля):

```bash
mvn -pl m5-level-23 spring-boot:run
```

С профилем `dev` или `prod` (получит соответствующий `.yml` с сервера):

```bash
mvn -pl m5-level-23 spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl m5-level-23 spring-boot:run -Dspring-boot.run.profiles=prod
```

`spring.config.import` в `application.yml` задан как **optional** — если Config Server не поднят, клиент стартует с дефолтами из `@Value("${... :fallback}")`, а не падает. Это удобно для локальной разработки.

## REST API и curl-команды

```bash
# Корень — sanity check
curl http://localhost:8080/

# Приветствие + запись в БД (значения тянутся из Config Server'а)
curl -s http://localhost:8080/greeting | jq

# Снимок текущих значений конфигурации в бине
curl -s http://localhost:8080/config | jq

# История визитов (вся таблица)
curl -s http://localhost:8080/visits | jq

# H2-консоль: http://localhost:8080/h2-console
#   JDBC URL: jdbc:h2:mem:testdb, user: sa, без пароля
```

### Что должно меняться от профиля

| Профиль | `app.greeting` | `app.banner-color` | `app.feature.show-history` |
|---|---|---|---|
| _(none)_ | `Hello from default config` | `gray`  | `false` |
| `dev` | `Hello, dev! Logs verbose, ...` | `green` | `true` (в `/greeting` появляется поле `recent`) |
| `prod` | `Welcome. Production environment.` | `red`   | `false` |

## Горячее обновление (`@RefreshScope` + `/actuator/refresh`)

Главный фокус лекции — менять конфигурацию **без перезапуска** клиента.

1. Открываем `config-repo/greeting-service.yml` и меняем, например, `banner-color: "gray"` → `"yellow"`.
2. Дёргаем эндпоинт Actuator на **клиенте** (не на сервере):
   ```bash
   curl -X POST http://localhost:8080/actuator/refresh
   ```
   В ответе придёт список обновлённых ключей, например `["app.banner-color"]`.
3. Проверяем:
   ```bash
   curl -s http://localhost:8080/config | jq
   # -> "app.banner-color": "yellow"
   ```

Это работает благодаря `@RefreshScope` на `GreetingService`: при `/refresh` бин пересоздаётся, новые `@Value` подхватываются. Без `@RefreshScope` поля «замораживаются» на момент старта.

В реальном проекте Config Server обычно смотрит на Git, и шаг 1 — это `git push`. Опционально настраивается webhook + Spring Cloud Bus, чтобы рассылать `/refresh` сразу всему флоту микросервисов.

## Иерархия источников конфигурации

То, что доезжает до `GreetingService`, собирается так (от низшего приоритета к высшему):

1. Дефолты в `@Value("${app.greeting:Hello}")` — если Config Server недоступен.
2. Локальный `application.yml` клиента — почти пустой, только подключение к Config Server и H2.
3. `greeting-service.yml` с Config Server'а — общий для всех профилей.
4. `greeting-service-{profile}.yml` с Config Server'а — переопределяет (3) для активного профиля.

То есть профильный файл бьёт дефолтный, удалённая конфигурация бьёт локальную.

## Тесты

```bash
mvn -pl m5-level-23 test
```

Отдельных тестов для этого модуля не пишем — поведение проверяется руками через curl и `/actuator/refresh`. Пирамида тестирования покрыта в `m5-level-21`.

## Что осталось за кадром

- **HashiCorp Vault** (`spring-cloud-starter-vault-config`). Vault и Config Server **не заменяют друг друга**: Config Server хранит нечувствительные настройки в Git (порты, таймауты, фича-флаги), Vault — секреты (пароли БД, API-ключи). В демо не разворачиваем, чтобы не тянуть `vault server -dev` и токены. См. раздел «Vault + Spring Cloud Config» в лекции.
- **Git-источник** для Config Server (`spring.cloud.config.server.git.uri`). Используем `native` (файловую систему) — так демо запускается без отдельного репозитория. В проде источник — внешний Git с code review на изменения конфигов.
- **Spring Cloud Bus** для автоматической рассылки `/refresh` по всем инстансам. В демо обновляем один клиент руками.
