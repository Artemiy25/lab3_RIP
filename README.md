# Реактивный сервис статистики

Микросервисная архитектура на Spring Boot 3.x WebFlux для генерации и обработки статистики случайных чисел с использованием реактивного программирования.

## Профилирование и оптимизация

Проект был профилирован с использованием **Java Flight Recorder (JFR)** и **JDK Mission Control (JMC)**.

### Результаты профилирования

**ДО оптимизации:**

![Allocations ДО](screenshots/before/before_alloc.png)
*Высокая частота аллокаций ArrayList и Long объектов*

![Memory ДО](screenshots/before/before_memory.png)
*Большое потребление heap при обработке 10M чисел (~250 MB)*

**ПОСЛЕ оптимизации:**

![Allocations ПОСЛЕ](screenshots/after/after_alloc.png)
*Значительное снижение аллокаций*

![Memory ПОСЛЕ](screenshots/after/after_memory.png)
*Минимальное потребление heap (~10 MB)*

### Устранённые проблемы производительности

1. **Множественные проходы по данным** → Single-pass алгоритм через `RandomStatsAggregator`
2. **new Random() при каждом запросе** → `ThreadLocalRandom.current()`
3. **Хранение всех чисел в ArrayList<Long>** → Streaming без хранения данных

### Результаты

| Метрика | ДО | ПОСЛЕ | Улучшение |
|---------|-----|--------|-----------|
| Heap usage (10M чисел) | ~250 MB | ~10 MB | **96% ↓** |
| Allocation rate | Высокий | Низкий | **70-90% ↓** |
| GC паузы | Частые | Редкие | **80-95% ↓** |
| Response time | Медленно | Быстро | **50-80% ↓** |

## Описание проекта

- **Сервис A (Клиент)** - REST API, который запрашивает статистику у Сервиса B
- **Сервис B (Сервер)** - генерирует случайные числа и вычисляет статистические показатели

## Стек технологий

- **Framework**: Spring Boot 3.3.0
- **Язык**: Java 17
- **Реактивность**: Project Reactor (Mono, Flux, WebFlux)
- **База данных**: PostgreSQL 16 с R2DBC (реактивный драйвер)
- **Сборка**: Maven 3.9
- **Контейнеризация**: Docker & Docker Compose
- **Логирование**: SLF4J + Logback
- **Утилиты**: Lombok

## Быстрый старт

### Запуск через Docker Compose (рекомендуется)

```bash
# Собрать и запустить все сервисы
docker-compose up --build

# Или использовать Makefile
make docker-up

# Посмотреть логи
docker-compose logs -f

# Остановить сервисы
docker-compose down
```

### Локальная разработка (требуется PostgreSQL)

```bash
# Запустите PostgreSQL локально на порту 5432
# Создайте БД: CREATE DATABASE andesis;
# Создайте пользователя: CREATE USER andesis WITH PASSWORD 'andesis';

# Соберите проект
mvn clean package -DskipTests

# Запустите приложение
java -jar target/andesis-1.0.0.jar
```

## API эндпоинты

### Сервис A (Клиент) - Порт 8080

**Получить статистику через Сервис B:**

```http
GET /api/client/random-stats?count=N&min=X&max=Y&range=Z
```

**Параметры:**
- `count` (необязательный, по умолчанию: 1000) - количество случайных чисел (макс: 10 000 000)
- `min` (необязательный, по умолчанию: -1000000) - минимальное значение (включительно)
- `max` (необязательный, по умолчанию: 1000000) - максимальное значение (включительно)
- `range` (необязательный) - диапазон значений (если указан, переопределяет min-max)

**Примеры запросов:**

```bash
# Базовый запрос (1000 чисел, диапазон по умолчанию)
curl "http://localhost:8080/api/client/random-stats?count=1000"

# Свой диапазон
curl "http://localhost:8080/api/client/random-stats?count=10000&min=-100000&max=100000"

# Большой датасет
curl "http://localhost:8080/api/client/random-stats?count=1000000&min=-500000&max=500000"
```

### Сервис B (Сервер) - Порт 8081

**Получить статистику напрямую:**

```http
GET /api/random/statistics?count=N&min=X&max=Y
```

Параметры те же, что и у Сервиса A.

**Примеры:**

```bash
# Базовый запрос
curl "http://localhost:8081/api/random/statistics?count=5000"

# С параметрами
curl "http://localhost:8081/api/random/statistics?count=50000&min=-10000&max=10000"
```

### Проверка здоровья сервисов

```bash
# Service A
curl "http://localhost:8080/api/actuator/health"

# Service B
curl "http://localhost:8081/api/actuator/health"
```

## Формат ответа

Все успешные ответы возвращают `RandomStatisticsDto` в формате JSON:

```json
{
  "count": 1000,
  "min": -999987,
  "max": 999999,
  "mean": 1234.567,
  "standardDeviation": 578900.123,
  "histogram": {
    "[-1000000, -800000)": 125,
    "[-800000, -600000)": 130,
    "[-600000, -400000)": 128,
    "...": "..."
  },
  "generatedAt": 1702564800000,
  "processingTimeMs": 1234
}
```

### Описание полей

- `count` - количество сгенерированных чисел
- `min` - минимальное значение в выборке
- `max` - максимальное значение в выборке
- `mean` - среднее арифметическое всех значений
- `standardDeviation` - стандартное отклонение (σ) - мера разброса
- `histogram` - распределение значений по интервалам (по умолчанию 20 корзин)
- `generatedAt` - временная метка в миллисекундах
- `processingTimeMs` - общее время обработки в миллисекундах

## Вычисление статистики

### Среднее значение (Mean)

Среднее арифметическое вычисляется по формуле:
```
mean = Σ(все значения) / count
```

### Стандартное отклонение (Standard Deviation)

Показывает, насколько разбросаны значения относительно среднего:
```
variance = Σ((значение - mean)²) / count
standardDeviation = √variance
```

### Гистограмма (Histogram)

Распределение значений по интервалам:
- Диапазон делится примерно на 20 корзин
- В каждой корзине подсчитывается количество значений
- Помогает понять распределение данных

## Логирование

### Уровни логов

- **DEBUG**: Детальное выполнение методов, параметры
- **INFO**: Запросы/ответы, сводка статистики, метрики производительности
- **WARN**: Невалидные параметры, ошибки валидации
- **ERROR**: Исключения, таймауты, сбои сервисов

### Пример логов

#### Сервис B - генерация статистики

```
23:51:28.757 [reactor-http-epoll-11] INFO  r.b.a.service.StatisticsCalculator - Starting statistics calculation for count=10000, range=[-100000, 100000]
23:51:28.758 [boundedElastic-1] DEBUG r.b.a.s.RandomNumberGenerator - Starting generation of 10000 random numbers in range [-100000, 100000]
23:51:28.810 [boundedElastic-2] DEBUG r.b.a.s.RandomNumberGenerator - Generated 10000 numbers in 52ms
23:51:28.860 [boundedElastic-4] DEBUG r.b.a.s.RandomNumberGenerator - Validation passed for all 10000 numbers
23:51:28.900 [boundedElastic-6] DEBUG r.b.a.s.RandomNumberGenerator - Mean calculated: 234.567 in 40ms
23:51:28.950 [boundedElastic-8] DEBUG r.b.a.s.RandomNumberGenerator - Standard deviation calculated: 57680.123 in 50ms
23:51:29.010 [reactor-http-epoll-11] INFO  r.b.a.s.StatisticsCalculator - Statistics calculation completed: count=10000, mean=234.567, stdDev=57680.123
```

#### Сервис A - фильтр запросов

```
23:51:28.750 [reactor-http-epoll-5] INFO  r.b.a.filter.LoggingFilter - >>> REQUEST: GET /api/client/random-stats | Query: {count=[10000], min=[-100000], max=[100000]}
23:51:29.050 [reactor-http-epoll-5] INFO  r.b.a.filter.LoggingFilter - <<< RESPONSE: GET /api/client/random-stats | Status: 200 | Time: 300ms
```

#### Обработка ошибок

```
23:52:10.123 [reactor-http-epoll-3] WARN  r.b.a.util.ValidationUtil - Invalid parameters provided: Count must be greater than 0
23:52:10.130 [reactor-http-epoll-3] ERROR r.b.a.e.GlobalExceptionHandler - Unexpected error occurred
```

## Обработка ошибок

### HTTP статус-коды

- **200 OK** - успешный запрос
- **400 Bad Request** - невалидные параметры (count ≤ 0, min ≥ max, выход за границы)
- **404 Not Found** - ресурс не найден
- **500 Internal Server Error** - внутренняя ошибка сервера
- **504 Gateway Timeout** - превышен таймаут в 60 секунд

### Формат ответа при ошибке

```json
{
  "error": "INVALID_PARAMETERS",
  "message": "Count cannot exceed 10000000 (provided: 20000000)",
  "timestamp": 1702564800000,
  "status": 400
}
```

## Тестирование

### Ручные тесты

```bash
# Тест 1: Малый датасет
curl "http://localhost:8080/api/client/random-stats?count=100"

# Тест 2: Свой диапазон
curl "http://localhost:8080/api/client/random-stats?count=5000&min=-50000&max=50000"

# Тест 3: Большой датасет (смотрим время обработки)
curl "http://localhost:8080/api/client/random-stats?count=500000"

# Тест 4: Невалидный count (должен вернуть 400)
curl "http://localhost:8080/api/client/random-stats?count=-100"

# Тест 5: Невалидный диапазон (должен вернуть 400)
curl "http://localhost:8080/api/client/random-stats?count=1000&min=100&max=50"

# Тест 6: Проверка здоровья
curl "http://localhost:8080/api/actuator/health"
```
