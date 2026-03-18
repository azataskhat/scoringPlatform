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

## Быстрый старт

### 1. Клонирование и настройка

```bash
cp .env.example .env
# Заполните .env: пароль БД, API-ключ аутентификации и ключи OSINT-сервисов
```

### 2. Запуск через Docker Compose

```bash
docker compose up -d
```

Сервисы будут доступны:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- PostgreSQL: localhost:5432

### 3. Локальная разработка

**PostgreSQL:**
```bash
docker compose up -d postgres
```

**Backend:**
```bash
cd backend
./mvnw spring-boot:run
```

**Data Collector:**
```bash
cd data_collector
pip install -r requirements.txt
python main.py
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
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
