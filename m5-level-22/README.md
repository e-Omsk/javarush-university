# Модуль 5. Уровень 22: Упаковка Spring Boot в Docker

Минимальное демо к лекции [Уровень 22: CI/CD и автоматизация развертывания](docs/ru.javarush.spring.presentation.level22.html).
Покрываем только часть **«контейнеризация»** — упаковка приложения в Docker и запуск через Docker Desktop. Полноценный пайплайн `.gitlab-ci.yml` не делаем, его пример есть в материалах вебинара.

В основе — модуль `m5-level-06` (Spring Boot 4 + H2 in-memory), доменная модель `User` сохранена ради консистентности с соседними уровнями.

## Что лежит в модуле

```
m5-level-22/
├── Dockerfile               ← multi-stage сборка (Maven → JRE)
├── .dockerignore            ← что НЕ копировать в build context
├── docker-compose.yml       ← запуск одной командой + env-переменные
├── pom.xml                  ← finalName=app, чтобы Dockerfile брал ровно target/app.jar
├── docs/                    ← материалы вебинара
└── src/main/java/com/javarush/
    ├── Main.java
    ├── controller/HelloController.java
    ├── entity/User.java
    ├── repository/UserRepository.java
    ├── service/{UserService, RegistrationService}.java
    └── exception/BusinessException.java
```

## Локальный запуск (без Docker, как в m5-level-06)

```bash
mvn -pl m5-level-22 spring-boot:run
```
Приложение на `:8080`. H2-консоль: http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:mem:testdb`, user `sa`, без пароля)

## Запуск в Docker

### Вариант 1. `docker build` + `docker run`

```bash
cd m5-level-22
```

# 1. Собираем образ. Имя:тэг = m5-level-22:1.0
```bash
docker build -t m5-level-22:1.0 .
```

# 2. Смотрим, что образ появился
```bash
docker images | grep m5-level-22
```

# 3. Запускаем контейнер:
````text
#    -d              запуск в фоне
#    --name          имя контейнера (видно в Docker Desktop -> Containers)
#    -p 8080:8080    проброс порта host:container
#    -e APP_GREETING указываем переменную окружения – Spring подхватит как app.greeting
````

```bash
docker run -d --name m5-level-22 \
  -p 8080:8080 \
  -e APP_GREETING="Привет из контейнера\!" \
  m5-level-22:1.0
```

Проверка:
```bash
curl http://localhost:8080/
```

```text
# -> Привет из контейнера!
```

```bash
curl -s http://localhost:8080/info
```

```text
# -> {"greeting":"Привет из контейнера!","host":"...","java":"17.0.19"}
```


Остановить и удалить:
```bash
docker rm -f m5-level-22
```

### Вариант 2. `docker compose` (удобнее)

```bash
cd m5-level-22
```
# Соберёт образ (если не собран) и поднимет контейнер
```bash
docker compose up --build
```

# Или в фоне:
```bash
docker compose up -d --build
```

# Логи:
```bash
docker compose logs -f app
```
# Снести всё:
```bash
docker compose down
```

`docker-compose.yml` уже задаёт порт `8080:8080` и переменную окружения `APP_GREETING`.

## Что смотреть в Docker Desktop GUI

После `docker run` / `docker compose up`:

- **Containers** → `m5-level-22` — статус, аптайм, использование CPU/Memory.
- Кнопка **Logs** — живой `stdout`/`stderr` Spring Boot (то же, что `docker logs -f`).
- Кнопка **Exec** → откроется shell внутри контейнера. Можно зайти и посмотреть процессы:
  ```bash
  ps -ef
  ls /app
  ```
- **Images** → `m5-level-22:1.0`. Видно размер слоёв, дату сборки.

## REST API и curl-команды

### Hello (переопределяемое приветствие)
```bash
curl http://localhost:8080/
```

### Диагностический эндпоинт – проверить, что переменная окружения дошла
```bash
curl -s http://localhost:8080/info
```
Возвращает `greeting`, `host` (это `HOSTNAME` контейнера = его ID) и версию Java.

### Получить всех пользователей
```bash
curl -s http://localhost:8080/users
```

### Регистрация пользователя
```bash
curl -X POST "http://localhost:8080/register?name=Docker&email=docker@example.com"
```

### Обновление двух email в одной транзакции
```bash
curl -X POST "http://localhost:8080/users/update-emails?id1=1&id2=2&email1=alice.new@example.com&email2=bob.new@example.com"
```

### Обновление с checked-исключением после save (демо отката транзакции)
```bash
curl -X POST "http://localhost:8080/users/1/email?email=alice@example.com"
```

## Тесты

```bash
mvn -pl m5-level-22 test
```
Для модуля с упором на контейнеризацию тесты не добавляли — детальная пирамида тестирования покрыта в `m5-level-21`.

## Как устроен Dockerfile

Используем **многоэтапную сборку** (multi-stage), как в лекции:

| Этап | Образ | Зачем |
|---|---|---|
| `builder` | `maven:3.9-eclipse-temurin-17` | Полный JDK + Maven. Тут собираем jar. |
| финальный | `eclipse-temurin:17-jre-jammy` | Только JRE, без Maven и без JDK. Образ меньше, поверхность атаки – тоже. |

Пара тонкостей, которые отличаются от примера в HTML:

1. **Слой с зависимостями отдельно от исходников.** Сначала `COPY pom.xml` + `mvn dependency:go-offline`, и только потом `COPY src`. Если код менялся, а `pom.xml` нет – Docker переиспользует кэш и не перекачивает зависимости заново.
2. **Непривилегированный пользователь.** В рантайм-стейдже создаём `app:app` и переключаемся на него через `USER app`. Запускать java от root в проде — плохая практика.
3. **`finalName=app`** в `pom.xml` — на выходе всегда `target/app.jar`. В Dockerfile нет маски `target/*.jar`, неоднозначности тоже нет.
4. **Тэги образов в лекции** (`maven:3.8.5-openjdk-17`, `openjdk:17-jdk-slim`) на Docker Hub уже помечены deprecated. Берём актуальные `maven:3.9-eclipse-temurin-17` и `eclipse-temurin:17-jre-jammy`.

## Переменные окружения и секреты

В `application.properties` приветствие записано так:
```
app.greeting=Hello World!
```

Через переменную окружения значение переопределяется автоматически — Spring Boot маппит `APP_GREETING` → `app.greeting`. Именно так в проде подсовывают пароли БД, ключи API и пр. — секреты **не должны** лежать в `Dockerfile` или образе:
```bash
docker run -d -p 8080:8080 \
  -e APP_GREETING="Hello from prod" \
  -e SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD_FROM_VAULT" \
  m5-level-22:1.0
```

## Что осталось за кадром

- `.gitlab-ci.yml` пайплайн — есть в материалах вебинара (см. секции *Простой пайплайн (CI)* и *Продвинутый пайплайн (CD)*). В минимальное демо не входит.
- Push образа в registry (`docker push`) — нужна учётка GitLab/Docker Hub/ECR. Локально не показываем.
- Деплой через SSH / Kubernetes — тоже только теория в лекции.
