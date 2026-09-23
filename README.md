# Data Credits — Sistema de Gestión de Préstamos Bancarios

API REST en **Spring Boot (WebFlux + JPA, arquitectura hexagonal)** y SPA en **Angular 20 (PrimeNG + Tailwind)**
para solicitar créditos, simular cuotas con **tasa efectiva anual** y que un analista los apruebe o rechace.

| | |
|---|---|
| Front | http://localhost:4200 |
| API | http://localhost:8080/api |
| Colección de peticiones | [`backend/http/data_credits.http`](backend/http/data_credits.http) |

---

## 1. Ejecución

### Opción A — Todo con Docker (recomendada para evaluar)

Requisitos: Docker Desktop.

```bash
docker compose up -d --build
```

Levanta PostgreSQL 16, el backend (aplica las migraciones Flyway con datos demo) y el front servido por Nginx.
La primera construcción tarda unos minutos (descarga dependencias de Maven y npm).

```bash
docker compose down        # detener
docker compose down -v     # detener y borrar la base de datos
```

### Opción B — Desarrollo local

Requisitos: Java 21, Maven 3.9, Node 20+ (probado con 24), Docker (solo para Postgres).

```bash
# 1. Base de datos (puerto 5433 en el host)
docker compose up -d postgres

# 2. Backend → http://localhost:8080
cd backend
mvn spring-boot:run

# 3. Front → http://localhost:4200
cd frontend
npm install
npm start
```

Variables de entorno del backend (con valores por defecto para local): `DB_URL`, `DB_USER`, `DB_PASSWORD`,
`JWT_SECRET` (Base64, ≥ 256 bits), `JWT_EXPIRATION_MINUTES`, `CORS_ORIGINS`.

---

## 2. Credenciales

Todas las cuentas usan la contraseña **`123`**.

| Correo | Rol | Notas |
|---|---|---|
| `usuario@test.com` | Usuario | Cuenta del enunciado |
| `admin@test.com` | Administrador | Cuenta del enunciado |
| `sofia.ramirez@test.com` | Administrador | Segundo analista (demo) |
| `maria.lopez@test.com`, `carlos.ruiz@test.com`, `ana.torres@test.com`, `jorge.medina@test.com` | Usuario | Con créditos en distintos estados |
| `pedro.gomez@test.com` | Usuario | **Inactivo** (no puede iniciar sesión) |

En el login, el botón **AUTENTICACIÓN** ofrece las cuentas del enunciado para rellenar el formulario.

---

## 3. Funcionalidad

**Usuario**
- Solicitar un crédito (monto y plazo) viendo la **cuota estimada en vivo**.
- Simulador con tabla de amortización (tasa del tramo o tasa propia).
- "Mis créditos": resumen, filtro por estado, detalle y cancelación de solicitudes pendientes.

**Administrador**
- Bandeja de solicitudes paginada, con búsqueda por nombre/correo y resumen por estado.
- **Aprobar fijando la tasa EA** (precargada con la sugerida, cuota definitiva recalculada en vivo) o **rechazar con motivo**.
- Tramos de tasa por plazo con barra de cobertura (detecta plazos sin tasa).
- CRUD de usuarios: roles, activar/desactivar, cambio de contraseña.

---

## 4. Reglas de negocio

| Regla | Detalle |
|---|---|
| Monto | 1.000.000 – 200.000.000 COP |
| Plazo | 6 – 84 meses |
| Tasa sugerida | Según el tramo del plazo: 6–12 m → 18 % EA · 13–36 m → 22 % EA · 37–84 m → 25 % EA (editable) |
| Tasa al aprobar | La define el analista entre 10 % y 28 % EA (tope de usura, configurable) |
| Estados | `PENDIENTE → APROBADO \| RECHAZADO \| CANCELADO`; cualquier otra transición → 409 |
| Pendientes | Máximo 3 solicitudes pendientes por usuario (seguro ante peticiones concurrentes) |
| Cuatro ojos | Nadie aprueba ni rechaza su propia solicitud |
| Rechazo | Motivo obligatorio (10–500 caracteres), visible para el solicitante |
| Usuarios | Correo único; nadie se desactiva ni se cambia el rol a sí mismo; siempre queda ≥ 1 admin activo; no se elimina un usuario con créditos |

Los límites están en `application.yml` (`data-credits.*`) y el front los lee de `GET /api/interest-rates`.

### Cálculo financiero (sistema francés)

```
Tasa mensual vencida:   i = (1 + EA)^(1/12) − 1
Cuota fija:             C = P · i / (1 − (1 + i)^−n)
```

Se usa `BigDecimal` (`DECIMAL128`), dinero a 2 decimales `HALF_UP`. La última cuota absorbe el redondeo, de modo que la
suma de abonos a capital es exactamente el monto prestado. Ejemplo: 10.000.000 a 12 meses con 24 % EA → 1,808758 % mensual,
cuota de 934.525,09.

---

## 5. Arquitectura

```
backend/src/main/java/com/markers/data_credits/
├── domain/            Núcleo sin Spring: modelos (records inmutables), reglas, InterestCalculator,
│                      excepciones y puertos (in = casos de uso, out = repositorios/servicios)
├── application/       Servicios que implementan los casos de uso (@Transactional)
└── infrastructure/
    ├── adapter/in/web       Controladores WebFlux, DTOs con Hibernate Validator, mappers MapStruct
    ├── adapter/out/persistence  Entidades JPA, repositorios Spring Data, Specifications, adaptadores
    ├── adapter/out/cache    Decoradores con EhCache de los puertos de salida
    ├── security             JWT + Spring Security reactivo
    ├── config               Política de créditos, caché
    └── exception            ApiResponse y manejo global de errores
```

Dependencias: `infrastructure → application → domain`. El dominio no conoce Spring, JPA ni la caché.

```
frontend/src/app/
├── core/       auth (store con signals, guards, interceptores), layout (barra lateral por rol), tema
├── modules/    auth · home · credits · admin/credits · admin/interest-rates · admin/users
│               cada uno: interfaces → services/repositories → services → store → UI/page + UI/components
└── shared/     logo, toast
```

---

## 6. Decisiones técnicas

**WebFlux + JPA.** El enunciado pide JPA (bloqueante) y WebFlux "donde lo amerite". Los controladores son reactivos y
cada caso de uso se ejecuta con `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` (`BlockingExecutor`):
el event-loop nunca se bloquea y la transacción y la caché corren completas en el mismo hilo.

**Transacciones y concurrencia.**
- Aprobar/rechazar es una transacción: carga → valida la transición → calcula condiciones → guarda.
- `@Version` en `credits`: si dos analistas deciden el mismo crédito a la vez, el segundo recibe **409** y se revierte todo
  (verificado con peticiones simultáneas contra PostgreSQL).
- Solicitar un crédito bloquea la fila del usuario (`SELECT … FOR UPDATE`) para que el límite de pendientes no se supere
  con peticiones paralelas (verificado: 5 simultáneas → 1 aceptada).

**Caché (EhCache 3 vía JCache).** Decoradores `@Primary` de los puertos de salida:

| Caché | Qué guarda | TTL | Se invalida |
|---|---|---|---|
| `creditById` | estado/detalle de un crédito | 10 min | al guardar el crédito |
| `creditsByUser` | "mis créditos" por usuario y filtro | 5 min | al guardar un crédito del usuario |
| `activeRateTiers` | tramos de tasa activos | 30 min | al modificar tramos |

La invalidación ocurre **después del commit** (evita recachear el valor viejo y no invalida si hay rollback).
El log `DEBUG` muestra "Caché sin dato: consultando…" solo cuando se va a la base de datos; `/actuator/caches` lista las cachés.

**Seguridad.** JWT (HS384) con rol y permisos en los claims. Dos barreras: `/api/admin/**` exige rol ADMIN y cada endpoint
exige su permiso con `@PreAuthorize` (p. ej. `CREDIT_APPROVE`). 401/403 también responden con `ApiResponse`.
En el front, `authGuard` / `roleGuard` (por rol o permiso) y un interceptor que adjunta el token y cierra sesión ante un 401.

**Errores HTTP.** Todas las respuestas usan `{ status, message, error, data }`:
400 validación (con el error de cada campo) · 401 sin sesión · 403 sin permiso · 404 no existe (o no es tuyo) ·
409 estado inválido, concurrencia o duplicado · 422 regla de negocio.

**Front.** Estado con servicios + signals (stores por módulo), Reactive Forms con validación en vivo, lazy loading por ruta,
diseño en blanco y negro con PrimeNG (tema Aura personalizado) y Tailwind.

---

## 7. Pruebas

```bash
cd backend && mvn test
```

**135 pruebas** (JUnit 5, Mockito, WebTestClient): cálculo financiero, reglas del dominio, servicios, controladores
(seguridad por rol/permiso y códigos HTTP) y caché con el `ehcache.xml` real.

```bash
cd frontend && npm test            # Karma + Jasmine (Chrome)
cd frontend && npm run format:check  # Prettier
```

---

## 8. Endpoints principales

| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/auth/login` | público |
| GET | `/api/auth/me` | autenticado |
| GET | `/api/interest-rates` | autenticado |
| POST | `/api/credits` | `CREDIT_REQUEST` |
| POST | `/api/credits/simulate` | `CREDIT_SIMULATE` |
| GET | `/api/credits/me?status=` | `CREDIT_VIEW_OWN` |
| GET | `/api/credits/{id}` | dueño o `CREDIT_VIEW_ALL` |
| PATCH | `/api/credits/{id}/cancel` | `CREDIT_CANCEL_OWN` |
| GET | `/api/admin/credits?status=&q=&page=&size=` · `/summary` | `CREDIT_VIEW_ALL` |
| PATCH | `/api/admin/credits/{id}/approve` `{annualRate}` | `CREDIT_APPROVE` |
| PATCH | `/api/admin/credits/{id}/reject` `{reason}` | `CREDIT_REJECT` |
| CRUD | `/api/admin/interest-rates` | `RATE_MANAGE` |
| CRUD | `/api/admin/users` · PATCH `/{id}/status` · GET `/api/admin/roles` | `USER_MANAGE` |

Ejemplos listos para ejecutar en [`backend/http/data_credits.http`](backend/http/data_credits.http).
