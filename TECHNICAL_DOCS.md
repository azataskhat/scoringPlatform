# IoT Security OSINT Platform — Техническая документация

---

## 1. Обзор (Overview)

### Назначение

Платформа агрегирует данные об уязвимых IoT-устройствах из четырёх OSINT-источников (Shodan, Censys, GreyNoise, NVD) и оценивает качество каждого источника по мультикритериальной модели скоринга.

### Бизнес-логика

1. Сборщики данных (Python) периодически опрашивают OSINT API.
2. Собранные устройства и уязвимости отправляются в backend через REST API.
3. Скоринг-модель вычисляет балл качества источника по 10 параметрам, сгруппированным в 4 категории: Reliability (R), Timeliness (T), Completeness (C), Accessibility (A).
4. Frontend визуализирует результаты: дашборд, карта устройств, таблицы уязвимостей, радар-диаграммы скоринга, генерация PDF-отчётов.

### Стек технологий

| Слой | Технологии |
|------|-----------|
| Backend | Java 17, Spring Boot 3.4.3 (WebFlux), R2DBC |
| Data Collector | Python 3.11, aiohttp, APScheduler, Shodan/Censys SDK |
| Frontend | React 18, TypeScript 5.4, Vite 5.3, Tailwind CSS 3.4, Leaflet 1.9, Recharts 2.12 |
| Database | PostgreSQL 16 + PostGIS 3.4 |
| Deployment | Docker, Docker Compose, Nginx |

### Целевая аудитория

- Разработчики, расширяющие функциональность платформы.
- Архитекторы, оценивающие архитектурные решения.
- Исследователи в области IoT-безопасности и OSINT.

---

## 2. Установка и запуск (Setup)

### Предварительные требования

- Docker >= 24.0
- Docker Compose >= 2.20
- Git

### Клонирование и конфигурация

```bash
git clone <repository-url>
cd Askhat
```

Создать файл `.env` в корне проекта:

```env
COMPOSE_PROJECT_NAME=iotsecurity

# Database
DB_NAME=iot_security
DB_USER=iot_app
DB_PASS=<secure_password>

# Internal API Key
API_KEY=<your_api_key>

# OSINT API Keys
SHODAN_API_KEY=<shodan_key>
CENSYS_API_ID=<censys_id>
CENSYS_API_SECRET=<censys_secret>
GREYNOISE_API_KEY=<greynoise_key>
NVD_API_KEY=<nvd_key>
```

### Запуск

```bash
docker compose up -d
```

Сервисы поднимаются в порядке зависимостей:

1. **postgres** — PostgreSQL + PostGIS (порт 5432)
2. **backend** — Spring Boot API (порт 8080)
3. **data-collector** — Python-сборщики (без внешних портов)
4. **frontend** — Nginx + React SPA (порт 3000)

### Проверка работоспособности

```bash
# Статус контейнеров
docker compose ps

# Health-check backend
curl http://localhost:8080/api/dashboard/stats

# UI
# Открыть http://localhost:3000
```

### Конфигурационные файлы

| Файл | Назначение |
|------|-----------|
| `.env` | Секреты и переменные окружения |
| `backend/src/main/resources/application.yml` | Spring Boot: порт, БД, пул соединений, веса скоринга |
| `data_collector/config.yaml` | Расписание сборщиков, поисковые запросы, эндпоинты backend |
| `docker-compose.yml` | Оркестрация сервисов |
| `frontend/vite.config.ts` | Настройки сборки и проксирование API |

### Ключевые переменные `application.yml`

```yaml
scoring:
  weights:
    reliability: 0.35
    timeliness: 0.25
    completeness: 0.25
    accessibility: 0.15
  ema-beta: 0.3        # коэффициент EMA-сглаживания
  lambda-decay: 0.05   # скорость затухания свежести данных
```

---

## 3. Архитектура системы

### Общая схема

```
┌─────────────┐     HTTP/JSON      ┌─────────────────┐     R2DBC      ┌──────────────┐
│  Frontend   │ ◄────────────────► │    Backend      │ ◄────────────► │  PostgreSQL   │
│  React SPA  │     SSE stream     │  Spring WebFlux │                │  + PostGIS    │
│  (Nginx)    │                    │                 │                │              │
│  :3000      │                    │  :8080          │                │  :5432       │
└─────────────┘                    └────────▲────────┘                └──────────────┘
                                            │
                                   POST /api/ingest
                                            │
                                   ┌────────┴────────┐
                                   │  Data Collector  │
                                   │  Python 3.11     │
                                   │  APScheduler     │
                                   └────────┬────────┘
                                            │
                              ┌─────────────┼─────────────┐
                              ▼             ▼             ▼
                          Shodan        Censys       GreyNoise
                                        NVD
```

### Модули backend

| Пакет | Назначение |
|-------|-----------|
| `controller/` | REST-контроллеры (7 шт.), SSE-стриминг |
| `service/` | Бизнес-логика: IngestService, ScoringService, ReportService |
| `repository/` | Реактивные R2DBC-репозитории (5 шт.) |
| `model/` | Доменные сущности: IoTDevice, Vulnerability, ScoringResult, SecurityEvent, OsintSource |
| `dto/` | DTO: DashboardStats, IngestRequest/Response, ScoringWeightsDto |
| `config/` | ApiKeyAuthFilter, ScoringProperties, WebFluxConfig (CORS) |

### Модули data_collector

| Модуль | Назначение |
|--------|-----------|
| `main.py` | Точка входа, инициализация сборщиков |
| `scheduler.py` | Конфигурация APScheduler, регистрация задач |
| `collectors/base.py` | Абстрактный `BaseCollector` с общим интерфейсом |
| `collectors/shodan_collector.py` | Интеграция с Shodan Search API |
| `collectors/censys_collector.py` | Интеграция с Censys Search v2 |
| `collectors/greynoise_collector.py` | Интеграция с GreyNoise Community/Enterprise |
| `collectors/nvd_collector.py` | Интеграция с NIST NVD (CVE) |
| `scoring/scorer.py` | Триггер скоринга через backend API |

### Структура БД

```
osint_sources ──┐
                ├──< iot_devices ──< vulnerabilities
                ├──< scoring_results
                └──< security_events >── iot_devices
```

#### Таблица `osint_sources`

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | BIGSERIAL PK | Идентификатор |
| name | VARCHAR(255) UNIQUE | Название источника |
| type | VARCHAR(100) | Тип: `search_engine`, `threat_intel`, `vulnerability_db` |
| base_url | VARCHAR(512) | Базовый URL API |
| api_key_ref | VARCHAR(255) | Ссылка на переменную окружения с ключом |
| active | BOOLEAN | Активен ли источник |
| update_interval_minutes | INTEGER | Интервал обновления |
| created_at, updated_at | TIMESTAMP | Метки времени |

#### Таблица `iot_devices`

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | BIGSERIAL PK | Идентификатор |
| source_id | BIGINT FK | Ссылка на osint_sources |
| ip_address | VARCHAR(45) | IPv4/IPv6-адрес устройства |
| port | INTEGER | Порт |
| protocol | VARCHAR(50) | Протокол (HTTP, MQTT, Modbus и т.д.) |
| device_type | VARCHAR(100) | Тип устройства (camera, router, sensor) |
| manufacturer | VARCHAR(255) | Производитель |
| firmware_version | VARCHAR(255) | Версия прошивки |
| city | VARCHAR(255) | Город |
| latitude, longitude | DOUBLE PRECISION | Координаты |
| raw_data | TEXT | Исходный JSON от источника |
| discovered_at | TIMESTAMP | Время обнаружения |

#### Таблица `vulnerabilities`

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | BIGSERIAL PK | Идентификатор |
| device_id | BIGINT FK (CASCADE) | Ссылка на iot_devices |
| cve_id | VARCHAR(30) | Идентификатор CVE |
| severity | VARCHAR(20) | CRITICAL / HIGH / MEDIUM / LOW |
| cvss_score | DOUBLE PRECISION | Оценка CVSS (0.0–10.0) |
| description | TEXT | Описание уязвимости |
| source_id | BIGINT FK | Источник, обнаруживший CVE |
| detected_at | TIMESTAMP | Время обнаружения |

#### Таблица `scoring_results`

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | BIGSERIAL PK | Идентификатор |
| source_id | BIGINT FK (CASCADE) | Ссылка на osint_sources |
| reliability_score | DOUBLE PRECISION | Балл надёжности (R) |
| timeliness_score | DOUBLE PRECISION | Балл своевременности (T) |
| completeness_score | DOUBLE PRECISION | Балл полноты (C) |
| accessibility_score | DOUBLE PRECISION | Балл доступности (A) |
| total_score | DOUBLE PRECISION | Итоговый балл |
| parameters | TEXT | JSON с 10 sub-метриками: r1, r2, r3, t1, t2, c1, c2, c3, a1, a2 |
| calculated_at | TIMESTAMP | Время расчёта |

#### Таблица `security_events`

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | BIGSERIAL PK | Идентификатор |
| device_id | BIGINT FK (CASCADE) | Ссылка на iot_devices |
| event_type | VARCHAR(50) | `new_exposure`, `new_vulnerability`, `port_change` |
| severity | VARCHAR(20) | Уровень серьёзности |
| description | TEXT | Описание события |
| source_id | BIGINT FK | Источник |
| event_time | TIMESTAMP | Время события |

#### Индексы

```sql
CREATE INDEX idx_devices_source   ON iot_devices(source_id);
CREATE INDEX idx_devices_city     ON iot_devices(city);
CREATE INDEX idx_vulns_device     ON vulnerabilities(device_id);
CREATE INDEX idx_vulns_severity   ON vulnerabilities(severity);
CREATE INDEX idx_scoring_source   ON scoring_results(source_id);
CREATE INDEX idx_events_device    ON security_events(device_id);
CREATE INDEX idx_events_type      ON security_events(event_type);
```

---

## 4. Справочник API

Базовый URL: `http://localhost:8080`

### Открытые эндпоинты (GET)

#### `GET /api/sources`

Список всех OSINT-источников.

**Ответ:**

```json
[
  {
    "id": 1,
    "name": "Shodan",
    "type": "search_engine",
    "baseUrl": "https://api.shodan.io",
    "active": true,
    "updateIntervalMinutes": 60,
    "createdAt": "2026-03-01T10:00:00",
    "updatedAt": "2026-03-01T10:00:00"
  }
]
```

#### `GET /api/sources/{id}`

Детали конкретного источника.

**Параметры пути:** `id` — идентификатор источника (Long).

#### `GET /api/devices`

Список IoT-устройств с фильтрацией.

**Query-параметры:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| city | String | Фильтр по городу |
| type | String | Фильтр по типу устройства |
| sourceId | Long | Фильтр по источнику |

**Ответ:**

```json
[
  {
    "id": 42,
    "sourceId": 1,
    "ipAddress": "185.120.77.34",
    "port": 8883,
    "protocol": "MQTT",
    "deviceType": "sensor",
    "manufacturer": "Hikvision",
    "firmwareVersion": "V5.7.1",
    "city": "Almaty",
    "latitude": 43.238,
    "longitude": 76.945,
    "discoveredAt": "2026-03-15T14:22:00"
  }
]
```

#### `GET /api/devices/{id}`

Детали конкретного устройства.

#### `GET /api/devices/map`

Устройства с координатами для отображения на карте. Возвращает только записи с ненулевыми `latitude` и `longitude`.

#### `GET /api/vulnerabilities`

Список CVE-уязвимостей.

**Query-параметры:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| severity | String | CRITICAL / HIGH / MEDIUM / LOW |
| deviceId | Long | Фильтр по устройству |
| sourceId | Long | Фильтр по источнику |

**Ответ:**

```json
[
  {
    "id": 7,
    "deviceId": 42,
    "cveId": "CVE-2024-23456",
    "severity": "CRITICAL",
    "cvssScore": 9.8,
    "description": "Remote code execution in Hikvision firmware",
    "sourceId": 4,
    "detectedAt": "2026-03-15T15:00:00"
  }
]
```

#### `GET /api/dashboard/stats`

Агрегированная статистика.

**Ответ:**

```json
{
  "totalDevices": 1247,
  "totalVulnerabilities": 389,
  "averageScore": 0.72,
  "activeSources": 4
}
```

#### `GET /api/dashboard/events/latest`

Последние события безопасности (до 50).

#### `GET /api/scoring/results`

История результатов скоринга.

**Query-параметры:**

| Параметр | Тип | Описание |
|----------|-----|----------|
| sourceId | Long | Фильтр по источнику |
| from | ISO DateTime | Начало периода |
| to | ISO DateTime | Конец периода |

**Ответ:**

```json
[
  {
    "id": 100,
    "sourceId": 1,
    "reliabilityScore": 0.82,
    "timelinessScore": 0.91,
    "completenessScore": 0.75,
    "accessibilityScore": 0.88,
    "totalScore": 0.84,
    "parameters": "{\"r1\":0.85,\"r2\":0.78,\"r3\":0.83,\"t1\":0.92,\"t2\":0.90,\"c1\":0.70,\"c2\":0.83,\"c3\":0.72,\"a1\":0.95,\"a2\":0.81}",
    "calculatedAt": "2026-03-15T16:00:00"
  }
]
```

#### `GET /api/scoring/stream`

SSE-поток обновлений скоринга в реальном времени.

**Формат событий:**

```
event: scoring-update
data: {"sourceId":1,"totalScore":0.84,"calculatedAt":"2026-03-15T16:00:00"}

: heartbeat (каждые 15 секунд)
```

#### `GET /api/reports/generate`

Генерация PDF-отчёта. Возвращает файл `application/pdf`.

---

### Защищённые эндпоинты (требуется `X-API-Key`)

Все запросы должны содержать заголовок:

```
X-API-Key: <значение из .env API_KEY>
```

#### `POST /api/sources`

Создание нового OSINT-источника.

**Тело запроса:**

```json
{
  "name": "NewSource",
  "type": "search_engine",
  "baseUrl": "https://api.example.com",
  "apiKeyRef": "NEW_SOURCE_API_KEY",
  "active": true,
  "updateIntervalMinutes": 120
}
```

**Ответ:** `201 Created` — объект созданного источника.

#### `PUT /api/sources/{id}`

Обновление существующего источника.

**Тело запроса:** Аналогично POST, все поля опциональны.

**Ответ:** `200 OK` — обновлённый объект.

#### `DELETE /api/sources/{id}`

Удаление источника.

**Ответ:** `204 No Content`.

#### `POST /api/ingest`

Приём данных от сборщиков.

**Тело запроса:**

```json
{
  "sourceId": 1,
  "devices": [
    {
      "ipAddress": "185.120.77.34",
      "port": 8883,
      "protocol": "MQTT",
      "deviceType": "sensor",
      "manufacturer": "Hikvision",
      "firmwareVersion": "V5.7.1",
      "city": "Almaty",
      "latitude": 43.238,
      "longitude": 76.945,
      "rawData": "{...}"
    }
  ],
  "vulnerabilities": [
    {
      "cveId": "CVE-2024-23456",
      "severity": "CRITICAL",
      "cvssScore": 9.8,
      "description": "Remote code execution"
    }
  ]
}
```

**Ответ:**

```json
{
  "devicesIngested": 15,
  "vulnerabilitiesIngested": 3,
  "eventsCreated": 15
}
```

#### `POST /api/scoring/run`

Запуск скоринга для всех активных источников.

**Ответ:** `202 Accepted`.

#### `POST /api/scoring/run/{sourceId}`

Запуск скоринга для конкретного источника.

**Ответ:** `202 Accepted`.

#### `PUT /api/scoring/weights`

Изменение весов скоринговой модели.

**Тело запроса:**

```json
{
  "reliability": 0.35,
  "timeliness": 0.25,
  "completeness": 0.25,
  "accessibility": 0.15
}
```

**Валидация:** Каждое значение должно быть в диапазоне `[0.0, 1.0]`.

**Ответ:** `200 OK` — обновлённые веса.

---

## 5. Безопасность и аутентификация

### Модель аутентификации

Платформа использует API-ключ для защиты операций записи.

| Тип запроса | Аутентификация |
|-------------|---------------|
| GET (чтение) | Не требуется |
| POST / PUT / DELETE | Заголовок `X-API-Key` |

### Механизм фильтрации

Класс `ApiKeyAuthFilter` (Spring WebFilter) перехватывает все входящие запросы:

1. Пропускает GET-запросы без проверки.
2. Для POST/PUT/DELETE извлекает значение `X-API-Key` из заголовков.
3. Сравнивает с `security.api-key` из `application.yml`.
4. При несовпадении возвращает `401 Unauthorized`.

### Защищённые маршруты

```
POST   /api/ingest
POST   /api/sources
PUT    /api/sources/{id}
DELETE /api/sources/{id}
POST   /api/scoring/run
POST   /api/scoring/run/{sourceId}
PUT    /api/scoring/weights
```

### CORS

Настроен в `WebFluxConfig.java`:
- Разрешённые origins: `http://localhost:3000` (frontend).
- Разрешённые методы: GET, POST, PUT, DELETE.
- Разрешённые заголовки: все (`*`).

### Сетевая изоляция

Docker Compose создаёт внутреннюю сеть. PostgreSQL доступен только контейнерам внутри этой сети — порт 5432 не публикуется на хост.

### Валидация данных

- Веса скоринга: проверка диапазона `[0.0, 1.0]`.
- Числовые входы: отклонение `NaN` и `Infinity`.
- БД: внешние ключи с `ON DELETE CASCADE`.

### Управление секретами

Все секреты хранятся в `.env` и передаются в контейнеры через `docker-compose.yml`. Файл `.env` исключён из Git через `.gitignore`.

### Известные уязвимости

Проведён аудит безопасности (см. `SECURITY_AUDIT.md`):

| Уровень | Количество | Примеры |
|---------|-----------|---------|
| Critical | 4 | Отсутствие аутентификации на GET, хардкод секретов, открытый порт БД |
| High | 11 | Mass assignment, утечка ключей, неограниченные запросы |
| Medium | 12 | XSS, отсутствие rate limiting, широкие exception handlers |
| Low | 8 | Debug-логи, устаревшие зависимости |

---

## 6. Ограничения и зависимости

### Внешние сервисы

| Сервис | Назначение | Rate Limits |
|--------|-----------|-------------|
| [Shodan API](https://api.shodan.io) | Поиск IoT-устройств | 1 запрос/сек (free), 100 credits/месяц |
| [Censys Search v2](https://search.censys.io/api) | Поиск хостов и сервисов | 0.4 запроса/сек (free), 250/день |
| [GreyNoise](https://api.greynoise.io) | Threat intelligence | 50 запросов/день (community) |
| [NIST NVD](https://services.nvd.nist.gov/rest/json/cves/2.0) | База CVE-уязвимостей | 50 запросов за 30 сек (с ключом) |

### Зависимости backend (Maven)

| Артефакт | Версия |
|----------|--------|
| spring-boot-starter-webflux | 3.4.3 |
| spring-boot-starter-data-r2dbc | 3.4.3 |
| r2dbc-postgresql | — |
| postgresql (JDBC, для schema init) | — |
| r2dbc-pool | — |
| jackson-databind | — |
| jackson-datatype-jsr310 | — |
| lombok | — |
| mapstruct | 1.5.5 |

### Зависимости data_collector (pip)

| Пакет | Версия |
|-------|--------|
| aiohttp | 3.9.5 |
| shodan | 1.31.0 |
| censys | 2.2.14 |
| pydantic | 2.6.4 |
| pydantic-settings | 2.2.1 |
| apscheduler | 3.10.4 |
| pandas | 2.2.2 |
| numpy | 1.26.4 |
| pyyaml | 6.0.1 |

### Зависимости frontend (npm)

| Пакет | Версия |
|-------|--------|
| react | 18.3.1 |
| react-dom | 18.3.1 |
| react-router-dom | 6.23.1 |
| axios | 1.7.2 |
| recharts | 2.12.7 |
| leaflet | 1.9.4 |
| react-leaflet | 4.2.1 |
| tailwindcss | 3.4.4 |
| typescript | 5.4.5 |
| vite | 5.3.1 |

### Известные ограничения

1. **Геолокация**: демо-режим ограничен регионом Almaty, Kazakhstan. Для расширения — изменить query в `config.yaml`.
2. **EMA-сглаживание**: первый скоринг не имеет исторических данных — начальный балл менее репрезентативен.
3. **Параметр `a1` (uptime)**: захардкожен как `0.95` — реальный мониторинг доступности API не реализован.
4. **PDF-отчёты**: генерация синхронная, при большом объёме данных может вызвать таймаут.
5. **Горизонтальное масштабирование**: не поддерживается — один инстанс каждого сервиса.
6. **TLS**: не настроен между сервисами внутри Docker-сети.
7. **Rate limiting**: отсутствует на уровне API — подвержен злоупотреблениям.

### Формула скоринга

```
S = wR·R + wT·T + wC·C + wA·A

По умолчанию: wR=0.35, wT=0.25, wC=0.25, wA=0.15

R = (r1 + r2 + r3) / 3
  r1 = valid_records / total_records                              — точность
  r2 = |IPs_src ∩ IPs_others| / |IPs_src ∪ IPs_others|           — согласованность (Jaccard)
  r3 = devices_with_CVE / total_devices                           — верифицируемость

T = (t1 + t2) / 2
  t1 = exp(-λ · hours_since_discovery), λ=0.05                   — свежесть
  t2 = min(1.0, device_count / 100)                               — частота сбора

C = (c1 + c2 + c3) / 3
  c1 = filled_fields / (9 × records)                              — заполненность полей
  c2 = distinct_device_types / 6                                   — покрытие типов
  c3 = (cve_depth + raw_depth) / 2                                — глубина данных

A = (a1 + a2) / 2
  a1 = uptime_ratio                                                — доступность API
  a2 = max(0, 1 - avg_response_time / 10)                         — скорость ответа

S_final = β · S_raw + (1 - β) · S_previous, β=0.3                — EMA-сглаживание
```

### Расписание сборщиков

| Источник | Интервал | Поисковый запрос |
|----------|----------|-----------------|
| Shodan | 60 мин | `city:Almaty port:80,443,1883,8883,5683,502` |
| Censys | 120 мин | `location.city: Almaty AND services.port: {80, 443, 1883}` |
| GreyNoise | 90 мин | `city:Almaty` |
| NVD | 360 мин | Keywords: IoT, MQTT, Modbus, camera, router |
| Scoring | 30 мин | Автоматический запуск для всех источников |
