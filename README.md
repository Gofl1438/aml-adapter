

# API-адаптер для проверки клиентов по чёрным спискам

Адаптер для интеграции между REST сервисом "Портфель клиента" и SOAP "Кредитное бюро".  
Позволяет автоматически проверять клиентов по спискам террористов и экстремистов, скрывая техническую сложность от бизнес-менеджера.

---
## Требования

- **Java 17+** (проверь: `java -version`)
- **Maven** (проверь: `mvn -v`)
- **Docker Desktop** (для PostgreSQL)
- Браузер (Chrome / Firefox)

---
## Быстрый старт
## Шаг 1. Поднять PostgreSQL

```bash
docker run --name portfolio-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=portfolio_db \
  -p 5432:5432 \
  -d postgres:16
```

Проверить, что контейнер живой:

```bash
docker ps
```

> Если контейнер уже создавался раньше - просто `docker start portfolio-db`.
> Пароль/логин/имя базы менять нельзя: они прописаны в `portfolio/src/main/resources/application.properties`.

## Шаг 2. Собрать и запустить Портфель (порт 8081)

В Портфель добавлен CORS-конфиг (без него браузер не даст фронту читать список клиентов),
поэтому JAR нужно пересобрать.

```bash
cd "путь-до-проекта/portfolio"
mvn -DskipTests clean package
java -jar target/portfolio-service-1.0.0.jar
```

Ждём в логе `Started PortfolioApplication in ... seconds`.
Таблицы (`clients`, `regions`, `accounts`) Hibernate создаст сам (`ddl-auto=update`).

Проверка:

```bash
curl http://localhost:8081/api/v1/clients
# должно вернуться []  — база пустая, это нормально
```

## Шаг 3. Наполнить базу тестовыми клиентами

База пустая, поэтому фронту нечего показывать. В корне проекта лежит скрипт:

```bash
cd "путь-до-проекта"
bash seed-data.sh
```

Он создаст **10 регионов и 25 клиентов**, разложив клиентов по регионам по кругу.
Повторный запуск безопасен: регионы сверяются по названию и второй раз не создаются,
клиенты защищены уникальностью ИНН/СНИЛС (вернётся 409)

Если хочешь добавить клиента руками - учти, что `regionId` обязателен и регион должен существовать:

```bash
# сначала регион
curl -X POST http://localhost:8081/api/v1/regions \
  -H "Content-Type: application/json" \
  -d '{"name":"Казань","type":"город","federalDistrict":"Приволжский"}'

# потом клиент (regionId — из ответа выше)
curl -X POST http://localhost:8081/api/v1/clients \
  -H "Content-Type: application/json" \
  -d '{"lastName":"Волков","firstName":"Егор","patronymic":"Ильич",
       "inn":"770101000099","snils":"111-111-111 99",
       "phone":"+7-999-100-00-99","regionId":1}'
```

## Шаг 4. Запустить Кредитное бюро (порт 8082)

Отдельное окно терминала. Этот JAR готов, собирать не надо

```bash
cd "путь-до-проекта/credit-bureau/target"
java -jar credit-bureau-1.0.0.jar
```

Ждём `Started CreditBureauApplication`.

Проверка (бюро отвечает только на XML):

```bash
curl -X POST http://localhost:8082/check \
  -H "Content-Type: text/xml;charset=UTF-8" \
  -d '<checkClient><fio>Иванов Иван</fio><inn>123</inn><snils>456</snils></checkClient>'
```

Ответ будет одним из трёх: `<amlStatus>true</amlStatus>`, `<amlStatus>false</amlStatus>`
или SOAP-ошибка. Это заложено специально - бюро нестабильное

## Шаг 5. Собрать и запустить Адаптер (порт 8083)

```bash
cd "путь-до-проекта/aml-adapter"
mvn -DskipTests clean package
java -jar target/aml-adapter-0.0.1-SNAPSHOT.jar
```

> В IntelliJ IDEA: панель Maven → aml-adapter → Lifecycle → `package`,
> предварительно нажав кнопку «Skip Tests» (иконка с молнией).

Ждём `Started AmlAdapterApplication`.

Проверка — адаптер отдаёт только проверку клиента:

```bash
# проверка клиента с id=1 (может занять до 25 секунд из-за ретраев)
curl -i -X POST http://localhost:8083/api/v1/clients/1/check
```

## Шаг 6. Открыть фронт

Вариант А - просто открыть файл:

```bash
open "путь-до-проекта/prod/index.html"
```

Вариант Б (надёжнее, рекомендую) - поднять статический сервер:

```bash
cd "путь-до-проекта/prod"
python3 -m http.server 5500
# затем открыть http://localhost:5500
```

Индикатор в шапке должен стать зелёным ("подключено"), в таблице — 25 клиентов на трёх страницах
(по 10 на страницу). Заодно проверишь пагинацию и поиск по ФИО/ИНН/СНИЛС.
Жмём «Проверить» → через несколько секунд бейдж: **"Не в списках"** или **"В списке"**.

---

## Проверка работы

### Проверь Портфель:

```bash
curl http://localhost:8081/api/v1/clients
```

Должен вернуть JSON-массив.

### Проверь Бюро:

```bash
curl -X POST http://localhost:8082/check \
  -H "Content-Type: text/xml;charset=UTF-8" \
  -d '<checkClient><fio>Иванов</fio><inn>123</inn><snils>456</snils></checkClient>'
```

Может вернуть `<amlStatus>true</amlStatus>`, `<amlStatus>false</amlStatus>` или SOAP-ошибку — это нормально.

---
## Конфигурация

Настройки адаптера можно изменить в `aml-adapter/src/main/resources/application.properties`:

|Параметр|Значение по умолчанию|Описание|
|---|---|---|
|`credit-bureau.url`|`http://localhost:8082/check`|Адрес Кредитного бюро|
|`portfolio.url`|`http://localhost:8081/api/v1/clients`|Адрес Портфеля|
|`server.port`|`8083`|Порт адаптера|
|`retry.max-attempts`|`5`|Максимальное число попыток при ошибке Бюро|

---

## Частые проблемы и их решение

|Симптом|Вероятная причина|Решение|
|---|---|---|
|**Список клиентов не грузится**|Портфель не запущен или занят порт 8081|Проверь `curl http://localhost:8081/api/v1/clients`. Посмотри логи Портфеля.|
|**Кнопка «Проверить» всегда ошибка**|Адаптер или Бюро не запущены|Проверь `curl -i -X POST http://localhost:8083/api/v1/clients/1/check`|
|**Браузер пишет `blocked by CORS`**|Запущен старый JAR Портфеля без CORS-конфига|Пересобери Портфель (`mvn -DskipTests clean package`) и перезапусти|
|**Список пустой («Никого не нашлось»)**|База пуста|Выполни `bash seed-data.sh`|
|**«Результат не определён, повторите через 5 минут»**|Бюро выдало ошибку (25% случаев), ретраи исчерпаны|Подожди 5 минут и попробуй снова — это нормальное поведение|
|**«Повторите через 4:37» сразу после проверки**|Таймер 5 минут активен|Либо подожди, либо сбрось поле `last_aml_check` в БД|
|**Порт занят (`Port 8081 was already in use`)**|Другой процесс слушает этот порт|Найди процесс (`lsof -i :8081`) и убей его (`kill -9 PID`)|
|**Ошибка `UnsupportedClassVersionError`**|Используется Java младше 17|Установи JDK 17+ и проверь `java -version`|
|**Сборка падает с `cannot find symbol` (Lombok)**|Неправильная версия JDK или Lombok|Используй JDK 17 или 21. Если нужно — установи `JAVA_HOME` на нужную версию.|
|**Сборка адаптера падает на тестах**|Два интеграционных теста красные|Собирай с `-DskipTests` (они к работе не критичны)|

---

## Команда

- **Тим-лид** - Андронов Егор
- **Бэкенд-разработчик (ведущий)** - Буров Кирилл
- **Бэкенд-разработчик (интеграции)** - Суханов Егор
- **Data Engineer / SQL** - Смирнов Данила
- **Frontend-разработчик** - Тихомиров Никита
- **QA / Test Architect** - Ганичев Иван
- **Аналитик / Git-менеджер / Документатор** - Антипин Степан
