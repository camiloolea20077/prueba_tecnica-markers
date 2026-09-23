# AGENT.md — Sistema de Gestión de Préstamos Bancarios

> **Fuente única de verdad del proyecto.** Todo agente/sesión lee este archivo antes de trabajar.
> No volver a explorar los proyectos de referencia: sus convenciones ya están resumidas aquí (§7).
> Si una decisión cambia, se actualiza aquí primero.

---

## 1. Objetivo

API REST (Spring Boot) + SPA (Angular) para:
1. **Usuario**: solicitar préstamo (monto + plazo) y consultar el estado de sus préstamos.
2. **Admin**: listar todas las solicitudes y aprobar/rechazar.

Credenciales semilla (password `123`, guardada con BCrypt):
| Rol | Email |
|---|---|
| USER | `usuario@test.com` |
| ADMIN | `admin@test.com` |

---

## 2. Estructura del repositorio

```
prueba_tecnica-markers/
├── AGENT.md              ← este archivo (reglas + plan)
├── CLAUDE.md             ← solo importa AGENT.md
├── docker-compose.yml    ← PostgreSQL 16
├── backend/              ← data_credits: Spring Boot (Maven), artifact `com.markers:data_credits`
└── frontend/             ← Angular 20
```
`.claude/` está en `.gitignore` (skills, agentes y settings locales no se versionan).

---

## 3. Stack

| Capa | Tecnología |
|---|---|
| Lenguaje backend | Java 21, Maven 3.9 |
| Framework | Spring Boot 3.5.x |
| Web | **Spring WebFlux** (controladores reactivos `Mono`/`Flux`) |
| Persistencia | **Spring Data JPA + Hibernate**, PostgreSQL 16, Flyway (migraciones) |
| Validación | Hibernate Validator (`jakarta.validation`) |
| Seguridad | Spring Security reactivo + JWT (jjwt 0.12.x), BCrypt |
| Caché | **EhCache 3** vía JCache (`spring-boot-starter-cache` + `org.ehcache:ehcache` + `javax.cache`) |
| Mapeo | MapStruct + Lombok |
| Tests | spring-boot-starter-test, JUnit 5, Mockito, WebTestClient, reactor-test |
| Frontend | Angular 20 (standalone, signals), PrimeNG 20 + `@primeng/themes` (Aura), Tailwind CSS 3 + `tailwindcss-primeui`, Reactive Forms |
| Infra local | Docker Compose (Postgres) |

### 3.1 WebFlux + JPA (decisión clave)
JPA es bloqueante. Regla: **ningún llamado JPA en el event-loop**.
- Los casos de uso (application) son **síncronos** y llevan `@Transactional` y `@Cacheable`/`@CacheEvict`.
- Los controladores WebFlux los envuelven:
  `Mono.fromCallable(() -> useCase.execute(cmd)).subscribeOn(Schedulers.boundedElastic())`.
- Así la transacción y la caché se ejecutan en un mismo hilo (boundedElastic) y el servidor sigue siendo no bloqueante.
- Utilidad compartida: `infrastructure/adapter/in/web/support/BlockingExecutor` (`Mono<T> run(Callable<T>)`).

---

## 4. Backend — Arquitectura hexagonal

Paquete raíz: `com.markers.data_credits`

```
backend/src/main/java/com/markers/data_credits/
├── DataCreditsApplication.java
├── domain/                         ← NÚCLEO. Sin Spring, sin JPA, sin Lombok de persistencia
│   ├── model/                      User, Role (code, name, permissions), RoleCodes, Permissions (constantes),
│   │                               AuthToken, AuthSession ✔
│   │                               Credit, InterestRateTier, CreditStatus (PENDING|APPROVED|REJECTED|CANCELLED), CreditPolicy, CreditApplicant, RateCatalog,
│   │                               CreditQuote (resultado de cálculo), AmortizationRow ✔
│   ├── service/                    InterestCalculator ✔ (Java puro: EA→mensual, cuota francesa, amortización)
│   ├── exception/                  DomainException (abstracta), InvalidCredentialsException, InactiveUserException,
│   │                               UserNotFoundException, CreditNotFoundException, InvalidCreditStateException, CreditRuleException ✔ ·
│   │                               EmailAlreadyExistsException
│   └── port/
│       ├── in/                     Casos de uso (interfaces). Los Commands son records anidados en el puerto
│       │                           (ej. AuthenticateUseCase.LoginCommand) y devuelven modelos de dominio.
│       │                           AuthenticateUseCase, GetCurrentUserUseCase, RequestCreditUseCase, SimulateCreditUseCase,
│       │                           QueryCreditUseCase, CancelCreditUseCase, QueryInterestRatesUseCase ✔ · DecideCreditUseCase, ManageUserUseCase
│       └── out/                    UserRepositoryPort (+findByIdForUpdate), PasswordEncoderPort, TokenProviderPort, CreditRepositoryPort, InterestRateTierRepositoryPort ✔
├── application/
│   └── service/                    Implementan puertos in: AuthService, CreditService, InterestRateService ✔ · AdminCreditService, UserService
│                                   (@Transactional, @Cacheable, @CacheEvict aquí)
└── infrastructure/
    ├── adapter/
    │   ├── in/web/
    │   │   ├── controller/         AuthController, CreditController, InterestRateController ✔ · AdminCreditController, UserController
    │   │   ├── dto/in|out/         Request/Response records con validaciones (@NotNull, @Positive, @Min...)
    │   │   ├── mapper/             MapStruct dominio → DTO web (AuthWebMapper, CreditWebMapper ✔)
    │   │   └── support/            BlockingExecutor
    │   └── out/persistence/
    │       ├── entity/             UserEntity, RoleEntity, PermissionEntity, CreditEntity (@Version), InterestRateTierEntity ✔
    │       ├── repository/         UserJpaRepository (@EntityGraph, @Lock), CreditJpaRepository, InterestRateTierJpaRepository ✔
    │       ├── mapper/             entity → domain (UserPersistenceMapper, CreditPersistenceMapper ✔)
    │       └── UserPersistenceAdapter, CreditPersistenceAdapter, InterestRateTierPersistenceAdapter ✔
    ├── security/                   ✔ SecurityConfig, SecurityProperties, JwtTokenProvider, JwtAuthenticationManager,
    │                               JwtSecurityContextRepository, SecurityErrorHandler, AuthenticatedUser,
    │                               BCryptPasswordEncoderAdapter
    ├── config/                     CacheConfig (CORS vive en SecurityConfig)
    └── exception/                  ApiResponse<T>, GlobalException, GlobalHandlerException
backend/src/main/resources/
├── application.yml                 perfiles: default (local) y test
├── ehcache.xml
└── db/migration/                   V1__security_schema, V2__security_seed, V3__credits_schema, V4__interest_rate_tiers_seed ✔
```
✔ = implementado. Tests en `backend/src/test/java/...` espejando el paquete; datos de prueba en `support/TestUsers`.

**Regla de dependencias:** `infrastructure → application → domain`. El dominio nunca importa nada de fuera.

### 4.1 Modelo de datos

`roles`: id, code UNIQUE (USER|ADMIN), name. ✔
`permissions`: id, code UNIQUE, description. ✔
`role_permissions`: role_id, permission_id (PK compuesta). ✔
`users`: id BIGSERIAL, full_name, email (índice único `lower(email)`), password (bcrypt), active, role_id FK, created_at, updated_at. ✔

| Permiso | USER | ADMIN |
|---|:-:|:-:|
| CREDIT_REQUEST, CREDIT_VIEW_OWN, CREDIT_CANCEL_OWN | ✔ | |
| CREDIT_SIMULATE | ✔ | ✔ |
| CREDIT_VIEW_ALL, CREDIT_APPROVE, CREDIT_REJECT, USER_MANAGE, RATE_MANAGE | | ✔ |

`interest_rate_tiers`: id, name, min_term_months, max_term_months, annual_effective_rate NUMERIC(7,4), active.
`credits`: id BIGSERIAL, user_id FK, amount NUMERIC(15,2), term_months INT,
suggested_annual_rate NUMERIC(7,4) (EA sugerida al solicitar), annual_effective_rate NUMERIC(7,4) NULL (EA aplicada por el admin),
monthly_rate NUMERIC(9,6) NULL, monthly_payment NUMERIC(15,2) NULL, total_interest NUMERIC(15,2) NULL, total_payable NUMERIC(15,2) NULL,
status VARCHAR, rejection_reason, decided_by FK users NULL, decided_at, created_at, updated_at, version BIGINT.
Las tasas se guardan como **porcentaje** (ej. `24.5000` = 24,5 % EA).

### 4.2 Reglas de negocio (en el dominio: `Credit` + `InterestCalculator`)
1. Monto: 1.000.000 ≤ amount ≤ 200.000.000 (COP).
2. Plazo: 6 ≤ termMonths ≤ 84.
3. Un préstamo nace en `PENDING`. Al solicitarlo se busca el **tramo de tasa** (`interest_rate_tiers`) cuyo rango de plazo lo contiene y se guarda como `suggested_annual_rate`; el usuario ve una **cuota estimada** con esa tasa.
4. Solo `PENDING → APPROVED | REJECTED | CANCELLED` (cancelar: solo el dueño). Cualquier otra transición → `InvalidCreditStateException` (409).
   `Credit` es un record inmutable: cada transición devuelve una instancia nueva. Mapeo entidad→dominio manual (MapStruct confunde `cancel(Long)` con setter fluido).
5. **Aprobar exige la Tasa Efectiva Anual (EA)** que aplica el admin (por defecto se precarga la sugerida). Límites: `data-credits.rates.min-annual` (10 %) ≤ EA ≤ `data-credits.rates.max-annual` (tasa de usura, configurable, 28 %). Fuera de rango → 422.
6. Al aprobar se calculan y persisten (en la misma transacción):
   - Tasa mensual vencida equivalente: `i = (1 + EA/100)^(1/12) − 1`
   - Cuota fija (sistema francés): `C = P · i / (1 − (1 + i)^−n)`
   - `total_payable = C · n`, `total_interest = total_payable − P`
   - `BigDecimal`, `MathContext.DECIMAL128`, dinero redondeado a 2 decimales `HALF_UP`.
7. Endpoint de **simulación** (sin persistir): dado monto, plazo y EA devuelve tasa mensual, cuota, intereses totales y tabla de amortización. Lo usan el formulario del usuario y el diálogo de aprobación del admin.
8. Rechazar exige `reason` (no vacío).
9. Un usuario no puede tener más de 3 préstamos `PENDING` a la vez (422).
10. El usuario solo ve sus préstamos; el admin ve todos.
11. Concurrencia: `@Version` (bloqueo optimista) → dos admins decidiendo a la vez = 409.
12. Tramos semilla: 6–12 meses 18 % EA · 13–36 meses 22 % EA · 37–84 meses 25 % EA. El admin puede editarlos (CRUD) sin superponer rangos.

### 4.3 Endpoints (prefijo `/api`)

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/auth/login` | público | `{email,password}` → `{token, user}` ✔ |
| GET | `/auth/me` | autenticado | usuario del token ✔ |
| POST | `/credits` | `CREDIT_REQUEST` | solicitar `{amount, termMonths}` → 201 PENDING con cuota **estimada** ✔ |
| POST | `/credits/simulate` | `CREDIT_SIMULATE` | `{amount, termMonths, annualRate?, includeSchedule?}` → cuota, tasa mensual, intereses, amortización ✔ |
| GET | `/credits/me?status=` | `CREDIT_VIEW_OWN` | mis créditos (más reciente primero) ✔ |
| GET | `/credits/{id}` | `CREDIT_VIEW_OWN` (dueño) o `CREDIT_VIEW_ALL` | detalle / estado (ajeno → 404) ✔ · caché en módulo 4 |
| PATCH | `/credits/{id}/cancel` | `CREDIT_CANCEL_OWN` (dueño) | PENDING → CANCELLED (no se borra: queda trazabilidad) ✔ |
| GET | `/interest-rates` | autenticado | tramos activos + EA mín/máx ✔ |
| GET | `/admin/credits` | ADMIN + `CREDIT_VIEW_ALL` | todas las solicitudes (filtro `status`, paginado) |
| PATCH | `/admin/credits/{id}/approve` | ADMIN + `CREDIT_APPROVE` | aprobar `{annualRate}` (EA %) — transaccional |
| PATCH | `/admin/credits/{id}/reject` | ADMIN + `CREDIT_REJECT` | rechazar `{reason}` — transaccional |
| POST/PUT/DELETE | `/admin/interest-rates[/{id}]` | ADMIN + `RATE_MANAGE` | CRUD de tramos |
| GET/POST/PUT/DELETE | `/admin/users[/{id}]` | ADMIN + `USER_MANAGE` | CRUD de usuarios |

Respuesta de crédito: `{id, applicant{id,fullName,email}, amount, termMonths, status, statusLabel, estimated, suggestedAnnualRate,
annualRate, monthlyRate, monthlyPayment, totalInterest, totalPayable, rejectionReason, createdAt, decidedAt}`.
`estimated=true` → condiciones calculadas con la tasa sugerida (aún no aprobado). Tasas en %, `monthlyRate` con 6 decimales.
Validación: formato en el DTO (400); rangos de negocio configurables en el dominio (422).

### 4.4 Formato de respuesta y errores (igual que his-ms-financial)
Toda respuesta: `ApiResponse<T> { status:int, message:String, error:boolean, data:T }`.
`GlobalHandlerException` (`@RestControllerAdvice`) mapea:
| Excepción | HTTP |
|---|---|
| `WebExchangeBindException` (validación) | 400 (lista de campos en `data`) |
| `CreditNotFoundException`, `UserNotFoundException` | 404 |
| `InvalidCreditStateException`, `OptimisticLockException`, `EmailAlreadyExists` | 409 |
| `DomainException` (regla de negocio) | 422 |
| `BadCredentialsException` | 401 |
| `AccessDeniedException` | 403 |
| `GlobalException(status, msg)` | status dado |
| cualquier otra | 500 (mensaje genérico, log del stacktrace) |
401/403 del filtro de seguridad también devuelven `ApiResponse` (entry point y access-denied handler propios).

### 4.5 Seguridad
- JWT HMAC (jjwt elige HS384 por el tamaño del secreto), secreto Base64 en `security.jwt.secret` (env `JWT_SECRET`), expiración 60 min.
- Claims: `sub`=email, `uid`, `name`, `role`, `permissions` (lista).
- Authorities en el contexto: `ROLE_<rol>` + cada permiso → sirve `hasRole('ADMIN')` y `hasAuthority('CREDIT_APPROVE')`.
- `SecurityWebFilterChain`: CSRF/basic/form/logout off, CORS `security.cors.allowed-origins`, stateless (`JwtSecurityContextRepository` lee `Authorization: Bearer`).
- Token inválido/alterado/expirado → contexto anónimo → 401 `ApiResponse`. Se rechazan segmentos Base64 no canónicos.
- Público: `/api/auth/login`, `/actuator/health`. `/api/admin/**` → `hasRole('ADMIN')`. Resto autenticado.
- **Segunda barrera:** `@PreAuthorize("hasAuthority('<PERMISO>')")` en cada endpoint sensible.
  ⚠️ Con seguridad reactiva el método anotado **debe devolver `Mono`/`Flux`** (si no → 500 `IllegalStateException`).
- Endpoints implementados: `POST /api/auth/login` → `{token, tokenType, expiresAt, user{id, fullName, email, role, roleName, permissions}}`; `GET /api/auth/me`.

### 4.6 Caché (EhCache 3)
| Cache | Clave | TTL | Se llena en | Se invalida en |
|---|---|---|---|---|
| `creditById` | creditId | 10 min | `QueryCreditUseCase.findById` | approve, reject, cancel |
| `creditsByUser` | userId | 5 min | `QueryCreditUseCase.findByUser` | request, approve, reject, cancel |
| `rateTiers` | `'all'` | 30 min | `InterestRateService.findActive` | CRUD de tramos |
Configurado en `ehcache.xml`; `@EnableCaching` en `CacheConfig`. Los objetos cacheados son records del dominio/app (Serializable).

### 4.7 Transacciones
- `DecideCreditUseCase.approve/reject`: `@Transactional` — carga con lock optimista, valida transición, registra `decidedBy`/`decidedAt`, guarda, evicta caché. Si algo falla, rollback completo.
- `@Transactional(readOnly = true)` en consultas.

### 4.8 Tests (obligatorios para operaciones críticas)
- `domain`: `CreditTest` — reglas de monto/plazo, transiciones. `InterestCalculatorTest` — EA→mensual (24 % EA ≈ 1,8088 % mensual), cuota francesa, suma de amortización = total.
- `application`: `CreditServiceTest` (Mockito) — request, approve, reject, errores 404/409/422.
- `web`: `AdminCreditControllerTest` con `@WebFluxTest` + `WebTestClient` — USER recibe 403 en approve, ADMIN 200, validación 400.
- `security`: `JwtTokenProviderTest`.
- Cache: test de integración que verifica que la segunda lectura no llama al repositorio.
Comando: `cd backend && mvn test`.

---

## 5. Frontend — Angular

Mismo patrón que `his-mf-financial`: **interfaces → repository (HTTP) → service → store (signals) → UI (page/components)**.

```
frontend/src/app/
├── app.config.ts          provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
│                          providePrimeNG({ theme: { preset: Aura } }), MessageService, ConfirmationService
├── app.routes.ts          lazy loading por módulo
├── core/
│   ├── auth/
│   │   ├── interfaces/    LoginRequest, AuthUser, AuthResponse
│   │   ├── services/      auth.repository.ts, auth.service.ts
│   │   ├── store/         auth.store.ts (signals: token, user, isAuthenticated, isAdmin; persistencia en sessionStorage)
│   │   ├── guards/        auth.guard.ts, role.guard.ts (data: { roles: ['ADMIN'] }), guest.guard.ts
│   │   └── interceptors/  auth.interceptor.ts (Bearer), error.interceptor.ts (401 → logout, toasts)
│   ├── models/            ApiResponse<T>, Page<T>
│   └── layout/            shell (topbar + sidebar según rol)
├── modules/
│   ├── auth/UI/page/login/
│   ├── credits/             (USER)
│   │   ├── interfaces/    credit.model.ts, create-credit.dto.ts, credit-status.enum.ts
│   │   ├── services/      repositories/credits.repository.ts, credits.service.ts
│   │   ├── store/         credits.store.ts
│   │   └── UI/
│   │       ├── page/      my-credits (tabla + resumen), request-credit (formulario)
│   │       └── components/credit-status-tag, credit-simulator (cuota estimada en vivo), credit-detail-dialog
│   └── admin/             (ADMIN)
│       ├── credits/         UI/page/admin-credits (tabla filtrable); diálogo de aprobación con EA precargada (sugerida),
│       │                  simulación en vivo (tasa mensual, cuota, intereses, total) y tabla de amortización; rechazo con motivo
│       ├── interest-rates/ CRUD de tramos de tasa EA
│       └── users/         CRUD de usuarios
└── shared/                componentes/pipes reutilizables (currency-cop pipe, empty-state)
environments/              environment.ts { apiUrl: 'http://localhost:8080/api' }
```

Reglas frontend:
- Standalone components, `inject()`, signals (`signal`, `computed`, `.asReadonly()` para estado público del store).
- Reactive Forms tipados con validadores: monto (required, min 1.000.000, max 200.000.000), plazo (required, 6–84), EA (required, min/max de `/interest-rates`), email, motivo de rechazo (required, minLength 10). Mostrar errores bajo el campo.
- UI: PrimeNG (Table, Tag, Dialog, Toast, ConfirmDialog, InputNumber, Select, Card, Skeleton) + Tailwind para layout/espaciado. Modo oscuro opcional, responsive.
- Nunca guardar la password; solo el token.
- Comando: `cd frontend && npm start` (http://localhost:4200).
- **Regla: nunca `template:` en línea.** Todo componente usa `templateUrl: './<nombre>.component.html'` (como el login),
  aunque la plantilla sea de una línea. Estilos propios, si los hay, en `styleUrl` aparte.
- **Formato con Prettier** (`frontend/.prettierrc.json`: ancho 100, comillas simples, **sin punto y coma**, trailing comma,
  parser `angular` para HTML). Tras escribir código: `npm run format` y verificar con `npm run format:check`.
- Tests: `cd frontend && CHROME_BIN="C:/Program Files/Google/Chrome/Application/chrome.exe" npx ng test --watch=false --browsers=ChromeHeadless`.
  En specs que usen guards/servicios agregar `provideHttpClient()` + `provideHttpClientTesting()`.
- Patrón de página: `modules/<módulo>/UI/page/<pagina>/<pagina>.component.{ts,html}`, `ChangeDetectionStrategy.OnPush`,
  errores de campo solo si `touched || dirty`, error del backend en `p-message` usando `err.error.message`.

---

## 6. Plan de trabajo (marcar al completar)

**Fase 0 — Scaffolding**
- [x] `.gitignore`, `AGENT.md`, `CLAUDE.md`, carpetas `backend/` y `frontend/`
- [x] `docker-compose.yml` (Postgres 16, db `data_credits_db`, user/pass `data_credits`/`data_credits`, **puerto host 5433** porque 5432 lo usa un Postgres local)
- [x] Generar proyecto Spring Boot en `backend/` (pom con dependencias de §3)
- [x] Generar proyecto Angular en `frontend/` + PrimeNG + Tailwind

> Se trabaja **un módulo a la vez**, de punta a punta (migración → dominio → app → persistencia → web → tests → prueba manual).

**Backend · Módulo 1 — Autenticación, roles y permisos** ✔
- [x] Flyway V1 (roles, permisos, usuarios) y V2 (semilla: 2 roles, 9 permisos, 2 usuarios)
- [x] Dominio + puertos + AuthService
- [x] Persistencia JPA (User/Role/Permission)
- [x] ApiResponse + GlobalHandlerException
- [x] JWT + SecurityWebFilterChain + CORS + 401/403 en JSON
- [x] `POST /api/auth/login`, `GET /api/auth/me`
- [x] Tests: AuthServiceTest (6), JwtTokenProviderTest (5), AuthControllerTest (12) → 23 en verde
- [x] Probado contra Postgres con curl

**Backend · Módulo 2 — Créditos (solicitud, simulación, tasas EA)** ✔
- [x] V3 (interest_rate_tiers + credits, CHECKs, índices) y V4 (3 tramos semilla)
- [x] `InterestCalculator` (EA→mensual, cuota francesa, amortización; la última cuota absorbe el redondeo) + `Credit` + `CreditPolicy` (bean desde `data-credits.*`)
- [x] `CreditService` / `InterestRateService`; solicitar = `@Transactional` + `SELECT … FOR UPDATE` del usuario (límite de pendientes seguro ante concurrencia)
- [x] Persistencia: CreditEntity (`@Version`), InterestRateTierEntity, adaptadores
- [x] `CreditController`, `InterestRateController`; GlobalHandler: 404 crédito, 409 estado/optimistic lock
- [x] Tests: InterestCalculator (10), Credit (13), CreditService (13), CreditController (13) → **72 en total, verde**
- [x] Probado con curl contra Postgres (incluye 5 solicitudes concurrentes → solo 1 aceptada)
- Tests de controladores usan `@WebLayerTest` (support) para importar seguridad + mappers.

**Backend · Módulo 3 — Administración de créditos**
- [ ] Aprobar (EA) / rechazar con `@Transactional` + `@Version`
- [ ] Listado admin filtrable y paginado
- [ ] CRUD de tramos de tasa

**Backend · Módulo 4 — Caché EhCache**
- [ ] ehcache.xml + CacheConfig + evicciones + test

**Backend · Módulo 5 — CRUD de usuarios (admin)**
- [ ] Endpoints `/api/admin/users` + tests

**Frontend · Módulo 1 — Login y núcleo de autenticación** ✔
- [x] Tema `core/theme/app-preset.ts` (Aura + primario índigo), `ApiResponse<T>`
- [x] `AuthStore` (signals; "Recordar" → localStorage, si no sessionStorage; descarta sesión expirada)
- [x] `AuthRepository` / `AuthService` (login, me, logout, homeUrl)
- [x] Interceptores: `authInterceptor` (Bearer solo a `environment.apiUrl`), `errorInterceptor` (0 / 401 → logout / 403 → toast)
- [x] Guards: `authGuard` (returnUrl), `guestGuard`, `roleGuard` (`data.roles` y/o `data.permissions`)
- [x] Login (`modules/auth/UI/page/login`): split 47,65 % / resto; panel izq. oculto en < lg.
      Botón **AUTENTICACIÓN** = popover con cuentas demo (rellena el formulario). `returnUrl` solo interno (anti open-redirect).
- [x] `shared/components/brand-logo` (isotipo SVG, `tone`, `stacked`)
- [x] `/inicio` temporal (`modules/home`) con usuario, rol y permisos → se reemplaza por el layout con menú
- [x] Tests: auth.store (6), guards (7), login (6) + app (1) → 20 en verde. Probado en Chrome contra el backend.

**Frontend · Módulo 2 — Layout (barra lateral izquierda)** ✔
- [x] `core/layout`: `ShellComponent` (sidebar + topbar + outlet), `SidebarComponent` (host `contents`, colapsable a íconos
      con preferencia en localStorage, panel deslizable en móvil), `TopbarComponent` (título desde `data.label`/`data.section`, menú de usuario)
- [x] `LayoutStore` (collapsed, mobileOpen, `confirmLogout()` con ConfirmDialog)
- [x] Menú declarativo `core/layout/menu/menu.config.ts` + `MenuService` (filtra por rol/permisos). **Mantener sincronizado con `app.routes.ts`.**
- [x] `/inicio` = tablero (saludo, accesos rápidos del menú, permisos). Secciones futuras usan `shared/pages/coming-soon`.
- [x] Toast propio `shared/components/app-toast` (tarjeta negra, ícono por severidad, barra de tiempo, 4,5 s). Usar `MessageService.add` normal.
- [x] Tests: menu.service (5), layout.store (3) → 28 en total.

**Reglas de diseño (preferencia del usuario):** blanco y negro, **sin degradados**. Primario = negro (`AppPreset`: zinc, `primary.color = zinc.950`).
Usar `surface-*` (grises neutros), `bg-surface-950` para fondos oscuros, `text-white`/`text-surface-300/400` sobre negro.
Solo los íconos de estado (toast) llevan un toque de color. Etiquetas: ADMIN `contrast`, USER `secondary`.

**Pruebas del front:** el usuario prueba en el navegador. El agente **no** usa Chrome ni corre `ng test`; verifica con `ng build`.

**Frontend · Módulo 3 — Usuario: solicitar crédito + simulador + mis créditos** ✔ (pendiente de prueba manual del usuario)
- [x] `modules/credits`: interfaces, `CreditsRepository` → `CreditsService` → `CreditsStore` (signals; filtro y estadísticas
      en cliente; catálogo cacheado con `shareReplay`; se reinicia solo si cambia el usuario en sesión)
- [x] Límites de monto/plazo/tasa leídos de `GET /api/interest-rates` (backend los expone desde `CreditPolicy`; nada quemado en el front)
- [x] `/creditos` (resumen, `p-selectbutton` por estado, tabla, detalle en diálogo, cancelar con confirmación)
- [x] `/creditos/solicitar` (Reactive Form, atajos de plazo, tramo sugerido, **simulación en vivo** con debounce + switchMap)
- [x] `/simulador` (tasa del tramo o propia con toggle, resumen + tabla de amortización paginada por años)
- [x] Componentes: `credit-status-tag`, `quote-summary`, `amortization-table`, `credit-detail-dialog`
- [x] Locale `es-CO` + `DEFAULT_CURRENCY_CODE = COP` en `app.config.ts`
- Estados con color funcional en etiquetas: pendiente ámbar, aprobado verde, rechazado rojo, cancelado gris.

**Frontend · Módulo 4 — Admin: créditos (aprobar con EA / rechazar), tramos, usuarios**
- [ ] …

**Frontend · Módulo 5 — Pulido UX (skeletons, empty states, responsive)**
- [ ] …

**Fase 5 — Entrega**
- [ ] README con pasos de ejecución, decisiones y credenciales
- [ ] Colección de requests (`backend/http/data_credits.http`)

---

## 7. Convenciones heredadas de los proyectos de referencia (ya analizados — no re-explorar)

- **his-ms-financial** (WebFlux): controladores devuelven `Mono<ResponseEntity<ApiResponse<T>>>`; inyección por constructor; servicios como interfaz + `impl`; DTOs separados `in/` y `out/`; MapStruct; `GlobalException(HttpStatus, msg)` + `GlobalHandlerException` con manejo de `WebExchangeBindException`; Javadoc en español en métodos públicos.
- **his-ms-backend-security**: JWT con jjwt 0.12 (`Keys.hmacShaKeyFor`, `Jwts.builder().subject().expiration().signWith()`), `JwtAuthenticationFilter` + `JwtAuthenticationEntryPoint`, BCrypt, stateless, URLs públicas en arreglo `PUBLIC_URLS`. Aquí se adapta a la versión **reactiva** (`SecurityWebFilterChain`).
- **his-mf-financial**: Angular 20 + PrimeNG 20 (Aura) + Tailwind 3 con plugin `tailwindcss-primeui`; capas `interfaces / services/repositories / services / store / UI/page / UI/components`; stores con signals privados `_x` y públicos `x = _x.asReadonly()`; repositorios con `HttpClient` y `Observable<ResponseModel<T>>`.

## 8. Reglas para agentes (ahorro de tokens)
1. Leer solo este archivo + los archivos que se van a modificar. No escanear el repo completo.
2. No volver a abrir `D:\Proyectos Sinergia\...` salvo que se pida explícitamente.
3. Código y nombres en inglés; comentarios, Javadoc, mensajes de API y textos de UI en español.
4. Al terminar una tarea: marcar el checkbox en §6 y, si se tomó una decisión nueva, anotarla en la sección correspondiente.
5. Commits con Conventional Commits (`feat(backend): ...`, `feat(frontend): ...`, `test: ...`, `chore: ...`).
