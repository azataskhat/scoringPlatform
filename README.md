# OSINT IoT Security Scoring Platform

Платформа мониторинга безопасности IoT-устройств на основе OSINT-источников
с количественной оценкой качества данных по скоринговой модели.

## Архитектура

```
┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│  Frontend   │────▸│   Backend    │◂────│    Data       │
│  React+TS   │ SSE │ Spring WebFlux│POST │  Collector   │
│  Port 3000  │◂────│  Port 8080   │     │  Python      │
└─────────────┘     └──────┬───────┘     └──────────────┘
                           │ R2DBC
                    ┌──────▼───────┐
                    │  PostgreSQL  │
                    │   + PostGIS  │
                    │  Port 5432   │
                    └──────────────┘
```

## Стек технологий

| Компонент | Технологии |
|-----------|-----------|
| Backend | Java 17, Spring Boot 3.2, WebFlux, R2DBC, SSE |
| Data Collector | Python 3.11, aiohttp, APScheduler, Shodan/Censys/GreyNoise/NVD |
| Frontend | React 18, TypeScript, Recharts, Leaflet, Tailwind CSS |
| Database | PostgreSQL 16 + PostGIS 3.4 |

## Требования

- Docker и Docker Compose

## Быстрый старт

### 1. Настройка переменных окружения

Отредактируйте файл `.env` в корне проекта — укажите пароль БД, API-ключ и ключи OSINT-сервисов:

```env
DB_PASS=<пароль_базы_данных>
API_KEY=<ключ_для_внутренней_аутентификации>
SHODAN_API_KEY=<ваш_ключ>
CENSYS_API_ID=<ваш_id>
CENSYS_API_SECRET=<ваш_secret>
GREYNOISE_API_KEY=<ваш_ключ>
NVD_API_KEY=<ваш_ключ>
```

### 2. Сборка и запуск

```bash
docker compose up -d
```

### 3. Инициализация базы данных (один раз при первом запуске)

```bash
docker compose exec -T postgres psql -U iot_app -d iot_security < backend/src/main/resources/schema.sql
docker compose exec -T postgres psql -U iot_app -d iot_security < backend/src/main/resources/data.sql
```

### 4. Открыть в браузере

- **http://localhost:3000** — веб-интерфейс (дашборд, карта устройств, скоринг)

### Остановка

```bash
docker compose down          # остановить (данные БД сохраняются)
docker compose down -v       # остановить и удалить данные БД
```

### Логи

```bash
docker compose logs -f                 # все сервисы
docker compose logs -f backend         # только backend
docker compose logs -f data-collector  # только collector
```

### Пересборка после изменений в коде

```bash
docker compose up -d --build
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/sources | Список OSINT-источников |
| GET | /api/sources/{id} | Детали источника |
| POST | /api/sources | Создать источник |
| PUT | /api/sources/{id} | Обновить источник |
| POST | /api/ingest | Приём данных от коллектора |
| GET | /api/scoring/stream | SSE-стрим обновлений |
| POST | /api/scoring/run | Запуск скоринга |
| GET | /api/scoring/results | История скоринга |
| PUT | /api/scoring/weights | Обновить веса |
| GET | /api/devices | Список устройств |
| GET | /api/devices/map | Устройства для карты |
| GET | /api/vulnerabilities | Уязвимости |
| GET | /api/dashboard/stats | Метрики дашборда |
| GET | /api/reports/generate | Генерация отчёта |

## Скоринговая модель

```
S = wR·R + wT·T + wC·C + wA·A

R = (r1 + r2 + r3) / 3     — Reliability
T = (t1 + t2) / 2           — Timeliness
C = (c1 + c2 + c3) / 3      — Completeness
A = (a1 + a2) / 2           — Accessibility

Веса: wR=0.35, wT=0.25, wC=0.25, wA=0.15
EMA: S_new = 0.3·S_current + 0.7·S_previous
```

Подробное обоснование модели: [SCORING_MODEL.md](SCORING_MODEL.md)

## Безопасность

- **API-аутентификация**: write-эндпоинты (POST/PUT/DELETE) защищены API-ключом через заголовок `X-API-Key`
- **Credentials**: все пароли и ключи хранятся в `.env` (не в коде), `.env` добавлен в `.gitignore`
- **PostgreSQL**: база данных доступна только внутри Docker-сети (не открыта наружу)
- **Валидация**: весовые коэффициенты скоринга проверяются на допустимый диапазон [0.0, 1.0]

Полный аудит безопасности: [SECURITY_AUDIT.md](SECURITY_AUDIT.md)
