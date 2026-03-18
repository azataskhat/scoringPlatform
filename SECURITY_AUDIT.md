# Security Audit Report — OSINT IoT Security Scoring Platform

**Дата аудита:** 2026-03-03
**Итого: 4 Critical, 11 High, 12 Medium, 8 Low = 35 уязвимостей**

---

## CRITICAL (4)

| # | Уязвимость | Где | Статус |
|---|-----------|-----|--------|
| 1 | **Нет аутентификации/авторизации** — все API-эндпоинты публично доступны. Любой может создавать/удалять источники, инжестить данные, запускать скоринг, читать IP-адреса устройств | Все контроллеры backend | **Исправлено** — добавлен API-key фильтр (`ApiKeyAuthFilter.java`) для всех write-операций (POST/PUT/DELETE) |
| 2 | **Захардкоженные credentials БД** — `postgres:postgres` + `sslMode=disable` | `docker-compose.yml`, `application.yml` | **Исправлено** — credentials вынесены в `.env`, `sslMode=disable` убран, дефолтный пользователь `iot_app` |
| 3 | **Порт PostgreSQL открыт наружу** (`0.0.0.0:5433`) — с дефолтными кредами любой может подключиться к БД | `docker-compose.yml:9-10` | **Исправлено** — `ports` заменён на `expose`, БД доступна только внутри Docker-сети |
| 4 | **Data Poisoning через неаутентифицированный `/api/ingest`** — можно залить фейковые устройства и уязвимости без ограничений | `IngestController.java`, `IngestService.java` | **Исправлено** — POST `/api/ingest` защищён API-key аутентификацией |

---

## HIGH (11)

| # | Уязвимость | Где | Статус |
|---|-----------|-----|--------|
| 5 | **Mass Assignment** — доменная сущность `OsintSource` используется как request body (можно подменить `id`, `apiKeyRef`) | `SourceController.java:33-53` | Не исправлено |
| 6 | **Утечка API-ключей** — поле `apiKeyRef` возвращается в ответах без `@JsonIgnore` | `OsintSource.java:28`, `SourceController.java` | Не исправлено |
| 7 | **Утечка чувствительных данных** — IP-адреса, GPS-координаты, raw scan data доступны без авторизации | `DeviceController.java`, `DeviceMap.tsx` | Не исправлено |
| 8 | **Нулевая валидация входных данных** — нет `@Valid`, нет Bean Validation, можно отправить `null`, `NaN`, отрицательные числа | Все DTO и контроллеры | Не исправлено |
| 9 | **Unbounded queries** — нет пагинации, эндпоинт `limit` без верхней границы -> DoS | Все list-эндпоинты, `DashboardController.java:48` | Не исправлено |
| 10 | **Scoring weights manipulation** — веса можно выставить в `NaN`/`Infinity`/отрицательные, ломая весь скоринг | `ScoringService.java:74-84` | **Исправлено** — добавлена валидация: [0.0, 1.0], отклонение NaN/Infinity |
| 11 | **Backend порт 8080 открыт напрямую**, минуя nginx | `docker-compose.yml:21-22` | Не исправлено |
| 12 | **Нет security headers в nginx** — нет CSP, X-Frame-Options, X-Content-Type-Options, HSTS | `nginx.conf` | Не исправлено |
| 13 | **Нет TLS/HTTPS** — весь трафик открытым текстом | `nginx.conf`, `application.yml` | Не исправлено |
| 14 | **Docker-контейнеры запущены от root** | Все 3 Dockerfile | Не исправлено |
| 15 | **Нет аутентификации при отправке данных на backend** из data_collector | `base.py:40-45`, `scorer.py:74-78` | **Исправлено** — добавлен заголовок `X-API-Key` при отправке данных |

---

## MEDIUM (12)

| # | Уязвимость | Где | Статус |
|---|-----------|-----|--------|
| 16 | **Stored XSS** — пользовательский ввод (`ipAddress`) сохраняется без санитизации в `security_events` | `IngestService.java:76-77` | Не исправлено |
| 17 | **Нет Rate Limiting** — ни на бэкенде, ни в nginx | Все эндпоинты | Не исправлено |
| 18 | **CORS** — `allowedHeaders("*")` + `allowCredentials(true)`, только dev-origins | `WebFluxConfig.java:13-19` | Не исправлено |
| 19 | **NPE -> 500 ошибка** при `null` в `request.getData()` | `IngestService.java:34` | Не исправлено |
| 20 | **Нет валидации ответов от внешних API** — данные из Shodan/Censys/GreyNoise/NVD идут напрямую без проверки | Все collectors | Не исправлено |
| 21 | **Broad exception handling** — `except Exception` глушит SSL-ошибки (MITM не будет замечена) | Все collectors, `scorer.py` | Не исправлено |
| 22 | **Type confusion в scorer** — `int(r.get("port"))` на не-числовой строке -> crash | `scorer.py:67` | Не исправлено |
| 23 | **Нет `.dockerignore`** — `.git`, `.env`, IDE-файлы могут попасть в образ | Все Dockerfile | Не исправлено |
| 24 | **`npm install` без lockfile** в Docker build -> supply chain risk | `frontend/Dockerfile:3-4` | Не исправлено |
| 25 | **Тесты пропущены** в Docker build backend (`-DskipTests`) | `backend/Dockerfile:6` | Не исправлено |
| 26 | **Java version mismatch** — pom.xml: Java 24, Dockerfile: Java 17 | `pom.xml:20`, `backend/Dockerfile` | **Исправлено** |
| 27 | **Неаутентифицированный SSE endpoint** без reconnection backoff | `useSSE.ts` | Не исправлено |

---

## LOW (8)

| # | Уязвимость | Где | Статус |
|---|-----------|-----|--------|
| 28 | **DEBUG-логирование** включено по умолчанию | `application.yml:20-22` | Не исправлено |
| 29 | **Нет security response headers** (backend без Spring Security) | Все конфиги | Не исправлено |
| 30 | **`rawData` типа `Object`** — потенциальная небезопасная десериализация | `IngestRequest.java:31` | Не исправлено |
| 31 | **Error messages** могут утекать через консоль/API | `ReportForm.tsx`, `useApi.ts`, `IngestService.java` | Не исправлено |
| 32 | **Устаревшие/неиспользуемые зависимости** (`pandas`, `pydantic` не используются) | `requirements.txt` | Не исправлено |
| 33 | **API-ключ Shodan в URL query string** (логируется серверами) | `shodan_collector.py:49` | Не исправлено |
| 34 | **Нет Docker HEALTHCHECK** | Все Dockerfile | Не исправлено |
| 35 | **Seed-данные с реальными IP** могут быть спутаны с реальной разведкой | `data.sql` | Не исправлено |
