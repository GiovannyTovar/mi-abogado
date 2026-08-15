# Arquitectura — Mi Abogado (backend)

SaaS multi-tenant para bufetes laboralistas. Monolito modular Spring Boot,
paquetes por dominio, PostgreSQL con Flyway, auth Firebase + JWT propio.

---

## 1. Modelo de datos

### 1.1 Lo que existe hoy (Fases 0 y 1, migración `V1`)

```
                    ┌──────────────┐
                    │    tenant    │  la firma / bufete
                    │──────────────│
                    │ id           │
                    │ slug (uq)    │  → subdominio del portal white-label
                    │ status       │  TRIAL | ACTIVE | SUSPENDED | CANCELLED
                    └──────┬───────┘
                           │ 1
                           │
                           │ N
                    ┌──────┴────────────────┐
                    │       app_user        │  identidad de plataforma
                    │───────────────────────│
                    │ id                    │
                    │ tenant_id (FK, null   │  null ⇔ role = SUPER_ADMIN
                    │   solo si SUPER_ADMIN)│
                    │ firebase_uid (uq)     │  null mientras está invitado
                    │ email                 │  uq por tenant
                    │ role                  │  SUPER_ADMIN | FIRM_OWNER |
                    │                       │  LAWYER | ASSISTANT | CLIENT
                    │ status                │  PENDING | ACTIVE | DISABLED
                    └───┬───────────────┬───┘
                        │ 1             │ 1
                        │               │
                        │ N             │ 0..1
              ┌─────────┴──────┐   ┌────┴──────────────────┐
              │ refresh_token  │   │       lawyer          │  perfil profesional
              │────────────────│   │───────────────────────│
              │ token_hash(uq) │   │ tenant_id  ← @TenantId│
              │ expires_at     │   │ user_id (uq)          │
              │ revoked_at     │   │ license_number        │  tarjeta profesional
              └────────────────┘   │ city, bio, hourly_rate│
                                   │ public_slug, published│  → directorio Fase 8
                                   │ rating_avg, count     │
                                   └───────────┬───────────┘
                                               │ N
                                               │
                                     ┌─────────┴──────────┐
                                     │ lawyer_practice_   │
                                     │      area          │
                                     └─────────┬──────────┘
                                               │ N
                                     ┌─────────┴──────────┐
                                     │   practice_area    │  catálogo global,
                                     │────────────────────│  NO multi-tenant
                                     │ code (uq), name    │
                                     └────────────────────┘
```

**Decisiones que conviene entender antes de seguir:**

| Decisión | Por qué |
|---|---|
| `app_user` separado de `lawyer` | Una persona es primero identidad (email, rol, login) y después, si aplica, un profesional con tarjeta y ficha pública. El asistente y el cliente final reutilizan `app_user` sin arrastrar campos vacíos. |
| `practice_area` no lleva `tenant_id` | Si cada firma tuviera su propio catálogo, el directorio público de la Fase 8 no podría filtrar "laboralistas de Medellín en acoso laboral" entre firmas distintas. |
| `tenant_id` en `lawyer`, no en `app_user`/`tenant` | Ver §4: `app_user` se consulta en el login, **antes** de saber a qué firma pertenece la persona. |
| Sin auto-registro | Entrar con Google prueba **quién** eres, no que tengas acceso. La fila en `app_user` la crea quien invita. Así ningún usuario queda huérfano, sin tenant ni rol. |
| `refresh_token` guarda SHA-256 | Una filtración de la BD no permite suplantar sesiones. |
| Todos los ids son `UUID` | Van en URLs públicas (`/api/v1/lawyers/{id}`); un `bigserial` filtra cuántos clientes tiene la plataforma. |

### 1.2 Fase 2 — planes y suscripciones (migraciones `V3`, `V4`)

```
┌────────────────────────┐         ┌──────────────────────┐
│   subscription_plan    │         │      tenant          │
│────────────────────────│         └──────────┬───────────┘
│ code (uq)              │                    │ 1
│ monthly_price, currency│                    │
│ trial_days             │                    │ 0..1 vigente
│ max_members       NULL │◄────┐   ┌──────────┴───────────┐
│ max_active_cases  NULL │  N  └───┤    subscription      │
│ marketplace_enabled    │         │──────────────────────│
│ white_label_enabled    │         │ status               │  TRIALING | ACTIVE
│ active, sort_order     │         │ started_at           │  PAST_DUE | CANCELLED
└────────────────────────┘         │ trial_ends_at        │
   catálogo global,                │ current_period_end   │
   NO multi-tenant                 │ cancelled_at         │
   NULL = ilimitado                └──────────────────────┘
                                   índice único parcial:
                                   una sola suscripción no
                                   cancelada por firma
```

| Decisión | Por qué |
|---|---|
| Dominio `subscription` propio, separado de `billing` | `billing` (Fase 6) es lo que la **firma le cobra a sus clientes**: honorarios, horas, pagos. `subscription` es lo que la **firma le paga a la plataforma**. Actores, ciclos de vida y permisos distintos; juntarlos sería un módulo con dos razones para cambiar. |
| `subscription.status` **y** `tenant.status` | El primero es la verdad comercial, el segundo es la puerta de acceso que mira el login. Se sincronizan en un solo sitio (`SubscriptionService.syncTenantStatus`) para que autenticar no tenga que unir dos tablas en cada petición. |
| Los clientes finales no cuentan para `max_members` | Son los clientes de la firma, no su plantilla. Cobrar por ellos penalizaría justo a quien más usa la herramienta. |
| Un downgrade por encima del límite se rechaza | Mejor un error claro que desactivar miembros en silencio para que quepan. |
| Sin `assistant` como entidad | Un asistente es un `User` con rol `ASSISTANT` y nada más. Una tabla sin columnas propias solo añade un JOIN. El abogado sí tiene entidad porque tiene datos propios: tarjeta, especialidades, ficha pública. |

**Pendiente de la Fase 2:** no hay pasarela de pago. `PAST_DUE` se alcanza cuando
vence la prueba (`TrialExpirationJob`, diario a las 03:00 Bogotá) y `ACTIVE` se
activa manualmente. El punto de enganche del cobro es
`SubscriptionService.expireFinishedTrials()`.

### 1.3 Modelo objetivo (fases siguientes — **no** se crean tablas todavía)

Se documenta para que las decisiones de hoy no bloqueen mañana. Cada tabla llega
en la migración de su fase.

```
Fase 3  client >── tenant                     CRM de clientes de la firma
        legal_case >── client, lawyer         expediente ("case" es palabra reservada)
        case_event >── legal_case             actuaciones y términos procesales
        lead >── tenant                       pipeline de captación
Fase 4  appointment >── legal_case            agenda
        message >── legal_case                mensajería con el cliente
Fase 5  legal_parameter                       SMLV, auxilio de transporte, por año
Fase 6  fee_agreement, time_entry, invoice, payment
Fase 7  notification, notification_template   WhatsApp
Fase 8  tenant_branding >── tenant            logo/colores del portal white-label
        lawyer_review >── lawyer              alimenta rating_avg
Fase 9  document_template, template_variable
        document >── legal_case               almacenamiento + metadatos
```

**Nota sobre el módulo `document` y la IA (v2):** `document` guardará el archivo y
sus metadatos, y expondrá la generación desde plantilla detrás de una interfaz
propia (`DocumentGenerator`). Hoy la única implementación sustituye variables en
una plantilla; en v2 se añade otra que llama a un LLM. Nada fuera del módulo
`document` necesita enterarse. Esa es la única interfaz "para el futuro" que se
justifica, y solo cuando llegue la Fase 9.

---

## 2. Estructura de paquetes

Por dominio, no por capa. Un cambio en "abogados" toca un solo directorio.

```
com.mi.abogado
├── AbogadoApplication.java
│
├── shared/                          ← infraestructura transversal, sin reglas de negocio
│   ├── config/
│   │   ├── SecurityConfig.java          filter chain, CORS, @EnableMethodSecurity
│   │   ├── FirebaseConfig.java          inicializa el Admin SDK
│   │   ├── FirebaseProperties.java
│   │   ├── JwtProperties.java
│   │   └── JpaConfig.java               @EnableJpaAuditing
│   ├── security/
│   │   ├── JwtService.java              emite y verifica el JWT propio
│   │   ├── JwtAuthenticationFilter.java middleware: autentica + resuelve tenant
│   │   ├── AuthPrincipal.java           record con userId, tenantId, role, email
│   │   └── CurrentUser.java
│   ├── tenant/
│   │   ├── TenantContext.java           ThreadLocal del tenant de la petición
│   │   └── TenantIdentifierResolver.java puente hacia el filtro de Hibernate
│   ├── persistence/
│   │   ├── BaseEntity.java              id UUID + auditoría
│   │   └── TenantScopedEntity.java      + @TenantId
│   └── error/
│       ├── BusinessException.java
│       └── ApiExceptionHandler.java     RFC 7807 (ProblemDetail)
│
└── domain/
    ├── tenant/         controller, service, dto, mapper, repository, entity
    ├── user/           controller (miembros), service, dto, mapper, repository, entity
    ├── auth/           controller, service, dto, entity, repository
    ├── subscription/   controller, service (+ TrialExpirationJob), dto, mapper,
    │                   repository, entity
    ├── lawyer/         ← dominio completo de referencia
    │   ├── controller/LawyerController.java
    │   ├── service/LawyerService.java
    │   ├── dto/        LawyerResponse, LawyerSummary, CreateLawyerRequest,
    │   │               UpdateLawyerRequest, PracticeAreaResponse   (records)
    │   ├── mapper/LawyerMapper.java                                 (MapStruct)
    │   ├── repository/ LawyerRepository, PracticeAreaRepository
    │   └── entity/     Lawyer, PracticeArea
    ├── client/         ┐
    ├── case/           │
    ├── document/       │  fases siguientes,
    ├── billing/        │  misma estructura interna
    ├── marketplace/    │
    └── notification/   ┘
```

### El dominio `lawyer` como plantilla

Cada pieza responde a un principio del proyecto:

- **`LawyerController`** — valida (`@Valid`), delega, traduce a HTTP. Cero lógica.
  El filtro por firma **no aparece**: lo aplica Hibernate con el tenant del token.
- **`LawyerService`** — los casos de uso. Sin interfaz `LawyerService` +
  `LawyerServiceImpl`: hay una sola implementación y no hay motivo para inventar
  una segunda.
- **`LawyerRepository`** — dos formas de leer, según la necesidad:
  - detalle → `@EntityGraph(attributePaths = {"user", "practiceAreas"})`,
    una consulta en lugar de tres (N+1);
  - listado → proyección con `select new ...LawyerSummary(...)`, que no
    materializa entidades ni colecciones. Imposible que haya N+1.
- **`dto/`** — `record`s inmutables, sin lógica. Petición y respuesta separadas:
  `UpdateLawyerRequest` no expone `ratingAvg` porque el rating lo calcula el
  sistema, no el usuario.
- **`LawyerMapper`** — MapStruct. `unmappedTargetPolicy=ERROR` (configurado en el
  `pom.xml`): si mañana se añade un campo a `LawyerResponse` y nadie lo mapea,
  **falla la compilación** en vez de devolver `null` en producción.

---

## 3. Flyway

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate     # el esquema lo manda Flyway, nunca Hibernate
```

```
src/main/resources/db/migration/
├── V1__init.sql                     tenant, app_user, refresh_token,
│                                    practice_area, lawyer, lawyer_practice_area
├── V2__seed_practice_area.sql       catálogo de especialidades laborales
├── V3__subscription.sql             subscription_plan, subscription
└── V4__seed_subscription_plan.sql   Freemium / Profesional / Firma
```

Reglas:

1. **Una migración aplicada no se edita nunca.** Se corrige con una `V3__...` nueva.
2. Una migración por fase/feature; nombre descriptivo (`V3__create_client_and_case.sql`).
3. Las tablas se crean cuando existe el código que las usa, no antes.
4. `ddl-auto: validate` es la red de seguridad: si una entidad y su tabla se
   desincronizan, la app no arranca. `AbogadoApplicationTests` lo comprueba en CI
   levantando un Postgres real con Testcontainers.

---

## 4. Flujo Firebase → JWT propio

### Por qué dos tokens

Firebase certifica **quién** es la persona. El rol y la firma son datos nuestros y
tienen que ser auditables en nuestra base de datos, no en un proveedor externo.
Por eso el ID token de Google se usa **una sola vez**, en el login; a partir de ahí
la app viaja con nuestro JWT, que lleva `tenantId` y `role` firmados.

### 4.1 Login con Google

```
Ionic/Angular        Firebase        AuthController      AuthService        BD
     │                   │                 │                  │              │
     │ signInWithGoogle()│                 │                  │              │
     ├──────────────────>│                 │                  │              │
     │   ID token (JWT   │                 │                  │              │
     │<──────────────────┤                 │                  │              │
     │                   │                 │                  │              │
     │ POST /api/v1/auth/google            │                  │              │
     │ { idToken }       │                 │                  │              │
     ├─────────────────────────────────────>│                 │              │
     │                   │                 │ loginWithGoogle()│              │
     │                   │                 ├─────────────────>│              │
     │                   │                 │                  │              │
     │                   │  verifyIdToken(idToken)            │              │
     │                   │<───────────────────────────────────┤              │
     │                   │  uid, email, name, picture         │              │
     │                   ├───────────────────────────────────>│              │
     │                   │                 │                  │              │
     │                   │       findByFirebaseUid(uid)       │              │
     │                   │                 │                  ├─────────────>│
     │                   │                 │                  │              │
     │      ┌────────────────────────────────────────────────────────────┐   │
     │      │ ¿existe?                                                   │   │
     │      │  SÍ  → es un login normal                                  │   │
     │      │  NO  → buscar invitación por email (firebase_uid is null)  │   │
     │      │        ├─ existe → linkFirebaseAccount(uid): PENDING→ACTIVE│   │
     │      │        └─ no existe → 403 "Esta cuenta no tiene acceso"    │   │
     │      └────────────────────────────────────────────────────────────┘   │
     │                   │                 │                  │              │
     │                   │   validar: usuario no DISABLED,    │              │
     │                   │   firma en TRIAL o ACTIVE          │              │
     │                   │                 │                  │              │
     │                   │   issueAccessToken(user)  ── HS256, 30 min       │
     │                   │     claims: sub, role, tenantId, email           │
     │                   │   issue refresh token     ── 30 días, hash SHA-256│
     │                   │                 │                  ├─────────────>│
     │                   │                 │                  │              │
     │ 200 { accessToken, refreshToken, expiresInSeconds, user }            │
     │<─────────────────────────────────────┤                 │              │
```

### 4.2 Peticiones siguientes — resolución de tenant

Aquí es donde el aislamiento multi-tenant deja de depender de la disciplina del
desarrollador:

```
Cliente          JwtAuthenticationFilter      TenantContext    Hibernate        BD
   │                      │                        │              │             │
   │ GET /api/v1/lawyers  │                        │              │             │
   │ Authorization: Bearer <jwt>                   │              │             │
   ├─────────────────────>│                        │              │             │
   │                      │ verify(jwt)            │              │             │
   │                      │  ├ firma HS256 válida  │              │             │
   │                      │  ├ issuer correcto     │              │             │
   │                      │  └ no expirado         │              │             │
   │                      │                        │              │             │
   │                      │ SecurityContext ← AuthPrincipal       │             │
   │                      │   (userId, tenantId, role)            │             │
   │                      │                        │              │             │
   │                      │ TenantContext.set(tenantId)           │             │
   │                      ├───────────────────────>│              │             │
   │                      │                        │              │             │
   │            ── @PreAuthorize("hasAnyRole(...)") ──            │             │
   │            ── LawyerController → LawyerService ──            │             │
   │                      │                        │              │             │
   │                      │      TenantIdentifierResolver         │             │
   │                      │      .resolveCurrentTenantIdentifier()│             │
   │                      │                        │<─────────────┤             │
   │                      │                        │  tenantId    │             │
   │                      │                        ├─────────────>│             │
   │                      │                        │              │             │
   │                      │   SELECT ... FROM lawyer              │             │
   │                      │   WHERE tenant_id = ?  ← lo añade Hibernate         │
   │                      │                        │              ├────────────>│
   │                      │                        │              │             │
   │ 200 [ ... ]          │                        │              │             │
   │<─────────────────────┤                        │              │             │
   │                      │ finally: TenantContext.clear()        │             │
   │                      │ (el hilo vuelve al pool limpio)       │             │
```

**El punto clave:** `Lawyer` hereda de `TenantScopedEntity`, que lleva
`@TenantId`. Hibernate añade `AND tenant_id = ?` a **toda** consulta de esa
entidad y rellena la columna en cada insert. No hay forma de olvidarse del filtro,
porque no se escribe a mano en ninguna parte.

Si no hay tenant en el contexto (super-admin o endpoint público), el resolver
devuelve el UUID sentinela `00000000-...-0000`, que no existe en ninguna fila: la
consulta devuelve vacío en lugar de devolver datos de otra firma. **Falla cerrado.**

**Plano de plataforma vs plano de firma.** No todas las tablas llevan `@TenantId`,
y la línea no es arbitraria:

- **Plano de firma** (`lawyer`, y más adelante `client`, `legal_case`, `document`,
  `invoice`…): datos de negocio de un bufete. Llevan `@TenantId`. Es el grueso del
  sistema y donde una fuga sería grave.
- **Plano de plataforma** (`tenant`, `app_user`, `subscription`): son
  cross-tenant por naturaleza. El login busca en `app_user` por `firebase_uid`
  **antes** de saber a qué firma pertenece la persona; el super-admin administra
  firmas y suscripciones de todas. Con el filtro activo, ninguna de las dos cosas
  sería posible. Aquí el aislamiento va explícito en el repositorio
  (`findByIdAndTenant_Id(...)`, `findCurrentByTenantId(...)`).

Regla práctica: si el super-admin necesita verlo entre firmas, o si se consulta
antes de resolver el tenant, es plano de plataforma. Todo lo demás lleva
`@TenantId`.

### 4.3 Refresh (rotación)

```
Cliente                    AuthService              RefreshTokenService        BD
   │                            │                          │                   │
   │ POST /api/v1/auth/refresh  │                          │                   │
   │ { refreshToken }           │                          │                   │
   ├───────────────────────────>│                          │                   │
   │                            │ consume(token)           │                   │
   │                            ├─────────────────────────>│                   │
   │                            │        findByTokenHash(sha256(token))        │
   │                            │                          ├──────────────────>│
   │                            │   ¿revocado o expirado? → 401                │
   │                            │   revoke(now)  ← rotación: se quema al usarse│
   │                            │                          ├──────────────────>│
   │                            │ nuevo access + nuevo refresh                 │
   │ 200 { accessToken, refreshToken, ... }                │                   │
   │<───────────────────────────┤                          │                   │
```

Rotación estricta: reutilizar un refresh ya consumido no da sesión. `logout`
revoca el token recibido; desactivar un usuario revoca todas sus sesiones
(`revokeAllByUser`).

### 4.4 Alta de la primera firma (el problema del huevo y la gallina)

Sin auto-registro, alguien tiene que existir antes del primer login:

1. `SuperAdminBootstrap` crea al arrancar el super-admin en estado `PENDING` a
   partir de `SUPER_ADMIN_EMAIL`. Es idempotente.
2. Esa persona entra con Google → se vincula su `firebase_uid` → queda `ACTIVE`.
3. `POST /api/v1/tenants` crea, **en una sola transacción**, la firma, su dueño
   (`FIRM_OWNER`, `PENDING`) y la suscripción al plan elegido. Si algo falla no
   queda una firma a medias sin dueño o sin plan.
4. El dueño entra con Google e invita a su equipo:
   `POST /api/v1/lawyers` (abogados) y `POST /api/v1/members` (asistentes).
5. Cada miembro entra con Google y su invitación se vincula.

El `slug` de la firma se deriva del nombre — *"Ramírez & Asociados S.A.S."* →
`ramirez-asociados-sas` — con sufijo numérico si ya existe. Será el subdominio del
portal white-label, por eso `UpdateTenantRequest` no permite cambiarlo: ya circula
en enlaces.

---

## 5. Endpoints (Fases 0, 1 y 2)

**Auth**

| Método | Ruta | Acceso | Qué hace |
|---|---|---|---|
| POST | `/api/v1/auth/google` | público | Login con ID token de Firebase |
| POST | `/api/v1/auth/refresh` | público | Rota la sesión |
| POST | `/api/v1/auth/logout` | autenticado | Revoca el refresh token |
| GET | `/api/v1/auth/me` | autenticado | Perfil de la sesión actual |

**Firmas y planes**

| Método | Ruta | Acceso | Qué hace |
|---|---|---|---|
| GET | `/api/v1/public/plans` | público | Planes activos (landing y onboarding) |
| POST | `/api/v1/tenants` | SUPER_ADMIN | Alta de bufete: firma + dueño + suscripción |
| GET | `/api/v1/tenants` | SUPER_ADMIN | Listado de firmas con su plan |
| GET | `/api/v1/tenants/{id}` | SUPER_ADMIN | Detalle |
| PATCH | `/api/v1/tenants/{id}/status` | SUPER_ADMIN | Suspensión manual |
| GET | `/api/v1/firm` | equipo de la firma | Su propia firma (id del token, no de la URL) |
| PATCH | `/api/v1/firm` | FIRM_OWNER | Edita sus datos |
| GET | `/api/v1/subscription` | equipo de la firma | Plan vigente + miembros en uso |
| PUT | `/api/v1/subscription/plan` | FIRM_OWNER | Cambio de plan |
| POST | `/api/v1/subscription/cancel` | FIRM_OWNER | Cancela |

**Equipo**

| Método | Ruta | Acceso | Qué hace |
|---|---|---|---|
| GET | `/api/v1/members` | equipo de la firma | Listado del equipo (proyección) |
| POST | `/api/v1/members` | FIRM_OWNER | Invita un asistente |
| PATCH | `/api/v1/members/{id}/status` | FIRM_OWNER | Activa / desactiva |
| GET | `/api/v1/lawyers` | equipo de la firma | Listado paginado (proyección) |
| GET | `/api/v1/lawyers/{id}` | equipo de la firma | Detalle |
| POST | `/api/v1/lawyers` | FIRM_OWNER | Invita al abogado y crea su perfil |
| PATCH | `/api/v1/lawyers/{id}` | FIRM_OWNER, LAWYER | Edición parcial |

Errores en formato RFC 7807 (`application/problem+json`).

**Dos reglas que cruzan módulos:**

- Toda alta de miembro (abogado o asistente) pasa por
  `SubscriptionService.ensureCanAddMember(tenantId)` antes de crear nada.
- Desactivar a un miembro revoca sus refresh tokens en el acto. Sin eso seguiría
  renovando sesión; su access token vigente caduca solo en menos de 30 minutos.

---

## 6. Puesta en marcha

### Local

```bash
# Postgres para desarrollo
docker run -d --name abogado-db -p 5432:5432 \
  -e POSTGRES_DB=abogado -e POSTGRES_USER=abogado -e POSTGRES_PASSWORD=abogado \
  postgres:16-alpine

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway aplica `V1` y `V2` al arrancar.

### VPS (Hostinger)

```bash
cp .env.example .env          # rellenar secretos
mkdir -p secrets && cp <service-account>.json secrets/firebase-service-account.json
docker compose up -d --build
```

`docker-compose.yml` levanta Postgres (sin puertos publicados: solo accesible
desde la red interna), la app y Nginx como terminador TLS y proxy inverso.

### Requisito del entorno

El proyecto compila con **JDK 21**. En esta máquina `JAVA_HOME` apunta a `jdk-17`,
que también está instalado junto a `jdk-21`; hay que apuntarlo a
`C:\Program Files\Java\jdk-21`.

`<fork>true</fork>` en el `maven-compiler-plugin` es obligatorio: con el compilador
embebido de Maven, Lombok no puede acceder a los internos de `jdk.compiler` y no
genera nada (fallan todos los getters con `cannot find symbol`).
