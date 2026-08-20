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

### 1.3 Fase 3 — clientes, expedientes y leads (migración `V5`)

```
┌──────────────┐        ┌────────────────────────┐       ┌──────────────┐
│    client    │        │      legal_case        │       │    lawyer    │
│──────────────│ 1    N │────────────────────────│ N   1 │  (Fase 1)    │
│ client_type  ├───────►│ client_id              │◄──────┤              │
│ document_*   │        │ assigned_lawyer_id     │       └──────────────┘
│   (uq/firma) │        │ practice_area_id       │
│ name         │        │ case_number (uq/firma) │──┐    ┌──────────────┐
│ email, phone │        │ radicado (uq si existe)│  │    │practice_area │
│ status       │        │ case_type   LITIGIO /  │  │    │  (Fase 1)    │
└──────┬───────┘        │             ASESORIA   │  │    └──────────────┘
       │                │ status, outcome        │  │
       │                │ priority, court        │  │  ┌──────────────────────┐
       │                │ opposing_party         │  └─►│ case_number_sequence │
       │                │ claim_amount           │     │ (tenant_id, year) PK │
       │                └───┬────────────────┬───┘     │ last_number          │
       │                    │ 1              │ 1       └──────────────────────┘
       │                    │ N              │ N
       │            ┌───────┴──────┐  ┌──────┴─────────────┐
       │            │  case_event  │  │   case_deadline    │
       │            │──────────────│  │────────────────────│
       │            │ event_type   │  │ deadline_type      │
       │            │ title, descr │  │ due_date  (DATE)   │
       │            │ occurred_at  │  │ notify_days_before │
       │            │ created_by   │  │ status PENDING /   │
       │            │ solo-append  │  │  COMPLETED/MISSED  │
       │            └──────────────┘  └────────────────────┘
       │ 0..1
       │  ┌────────────────────────┐
       └──┤          lead          │
converted │────────────────────────│
_client_id│ name, email, phone     │
          │ source  MARKETPLACE /  │
          │   CALCULADORA / ...    │
          │ status  NEW→CONTACTED→ │
          │  QUALIFIED→CONVERTED   │
          │            ↘ LOST      │
          └────────────────────────┘
```

| Decisión | Por qué |
|---|---|
| Paquete `legalcase`, clase `LegalCase`, tabla `legal_case` | `case` es palabra reservada **en Java y en SQL**: `package ...domain.case` no compila. El prompt pedía un dominio `case`; este es el nombre más cercano que el lenguaje permite. |
| `case_number` lo asigna el sistema, con tabla de secuencia | `max(case_number) + 1` se pisa cuando dos personas crean un caso a la vez. El `INSERT … ON CONFLICT DO UPDATE … RETURNING` es atómico en Postgres: no hace falta bloquear nada. Formato `2026-0001`, reiniciado por año. |
| `due_date` es `LocalDate`, no `Instant` | Los términos vencen "el 15 de marzo", no "el 15 de marzo a las 17:00 UTC". Guardarlos con hora invita a errores de zona horaria justo donde más caro sale. |
| `case_event` solo se agrega | Es el historial de lo que pasó. Poder reescribirlo destruye su valor frente al cliente — y en la Fase 4 es exactamente lo que el cliente verá en su portal. |
| Cerrar exige desenlace | Un caso "cerrado sin resultado" no informa nada. Lo respalda `ck_case_closed` en la BD, no solo el service. |
| El job `MISSED` usa SQL nativo | Es tarea de plataforma: cruza todas las firmas. Una consulta JPQL sobre `CaseDeadline` la limitaría a un solo tenant por el filtro de Hibernate. |
| El lead exige correo **o** teléfono | Sin ninguno de los dos no hay a quién contactar, y un lead incontactable ensucia el pipeline. |

**Contra el N+1, dos patrones que se repiten en esta fase:** el listado de clientes
trae `openCases` con una subconsulta correlacionada, y el de expedientes trae
`nextDueDate` igual. Son las columnas que más se miran; resolverlas por fila
serían N consultas extra en cada pantalla.

### 1.4 Fase 4 — portal del cliente (migración `V6`)

```
   app_user ◄──── client.user_id        el cliente entra con rol CLIENT
   (rol CLIENT)   (uq, nullable)        y su ficha del CRM queda enlazada

   case_event + visible_to_client       lo interno sigue interno;
                (default FALSE)         publicar es una decisión de la firma

┌──────────────────────┐   ┌────────────────────┐   ┌──────────────────────┐
│      document        │   │   case_message     │   │     appointment      │
│──────────────────────│   │────────────────────│   │──────────────────────│
│ legal_case_id        │   │ legal_case_id      │   │ client_id     (req.) │
│ storage_key (uq)     │   │ sender_id          │   │ legal_case_id (opc.) │
│ name, content_type   │   │ body               │   │ lawyer_id            │
│ size_bytes           │   │ read_at            │   │ mode  PRESENCIAL /   │
│ visibility INTERNAL /│   │                    │   │   VIRTUAL/TELEFONICA │
│   SHARED_WITH_CLIENT │   │ un hilo por caso   │   │ starts_at < ends_at  │
│ source  FIRM/CLIENT  │   │ inmutable salvo    │   │ status SCHEDULED /   │
│ extracted_text ──► v2│   │   read_at          │   │  CONFIRMED/COMPLETED │
└──────────────────────┘   └────────────────────┘   │  CANCELLED/NO_SHOW   │
   binario en disco                                 └──────────────────────┘
   (volumen del VPS)
```

**El aislamiento del cliente es un problema distinto al del tenant.** El filtro de
`@TenantId` separa una firma de otra, pero **no** separa a un cliente de los demás
clientes de su misma firma. Ese segundo aislamiento hay que escribirlo. Por eso el
portal es un módulo aparte y no un rol más en los endpoints de la firma:

| Decisión | Por qué |
|---|---|
| `/api/v1/portal/**` separado, solo rol `CLIENT` | Una sola puerta de entrada (`ClientPortalService.requireOwnCase`) por la que pasa **toda** operación del portal. Auditar un archivo es factible; auditar `if (esCliente)` repartidos por seis servicios, no. |
| Ninguna ruta del portal lleva el id del cliente | Sale del token. Lo que no viaja por la URL no se puede manipular. |
| Un caso ajeno responde **404**, no 403 | Un 403 confirmaría que el caso existe. Con 404 el cliente no puede sondear qué expedientes tiene la firma. |
| `visibility` por documento y `visible_to_client` por actuación, ambos **falsos por defecto** | El borrador de una estrategia y una nota sobre la solvencia de la contraparte no son para el cliente. Compartir es un acto explícito. |
| Lo que sube el cliente nace `SHARED_WITH_CLIENT` y no se le puede ocultar | Ocultarle su propio documento no tiene sentido y parecería una manipulación del expediente. |
| La descarga del portal comprueba **dos** cosas | Que el documento sea de un caso suyo **y** que esté compartido. Solo lo primero dejaría ver documentos internos de su propio caso a quien adivine un id. |
| `storage_key` se genera con UUID, sin el nombre original | Un nombre como `../../etc/passwd` no puede escapar del directorio, y dos archivos homónimos no se pisan. `DocumentStorage.resolve` comprueba además que la ruta final siga dentro del almacén. |
| Descarga siempre como `attachment` + `nosniff` | Un HTML o un SVG subido por un cliente se ejecutaría con el dominio de la aplicación si el navegador lo renderizara. |
| Lista blanca de `content-type` | Sin ella el almacén se convierte en un vector de distribución de malware. |
| `PortalCaseRepository` en el paquete `portal` | La dependencia apunta en una sola dirección: el portal conoce el expediente, el expediente no conoce el portal. Y mantiene junto todo lo que el cliente puede leer, que es lo que se revisa al auditar. Extiende `Repository`, no `JpaRepository`: desde el portal no se guarda ni se borra. |
| Choques de agenda rechazados | Una agenda que permite dos citas del mismo abogado a la misma hora no es una agenda. |

**El módulo `document` y la IA de v2:** los binarios solo los toca `DocumentStorage`,
y `extracted_text` está reservado para cuando entre OCR o un LLM. Hoy siempre es
`NULL` y **nadie fuera del módulo lo lee**, así que llenarlo más adelante no obliga
a tocar ningún otro dominio.

### 1.5 Modelo objetivo (fases siguientes — **no** se crean tablas todavía)

Se documenta para que las decisiones de hoy no bloqueen mañana. Cada tabla llega
en la migración de su fase.

```
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
com.miabogado
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
    ├── client/         controller, service, dto, mapper, repository, entity
    ├── legalcase/      controller (casos + agenda), service (caso, bitácora,
    │                   términos, consecutivo, job), dto, mapper, repository, entity
    ├── lead/           controller, service, dto, mapper, repository, entity
    ├── document/       controller, service (+ DocumentStorage), dto, mapper,
    │                   repository, entity
    ├── message/        controller, service, dto, repository, entity
    ├── appointment/    controller, service, dto, mapper, repository, entity
    ├── portal/         controller, service, dto, repository  ← sin entidades:
    │                   es un modelo de lectura sobre los dominios de la firma
    ├── billing/        ┐  fases siguientes,
    ├── marketplace/    │  misma estructura interna
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
├── V4__seed_subscription_plan.sql   Freemium / Profesional / Firma
├── V5__client_case_lead.sql         client, legal_case, case_number_sequence,
│                                    case_event, case_deadline, lead
└── V6__client_portal.sql            client.user_id, case_event.visible_to_client,
                                     document, case_message, appointment
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

## 5. Endpoints (Fases 0 a 4)

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

**Clientes y expedientes** (todo el equipo de la firma)

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/v1/clients` | CRM paginado, con casos abiertos por cliente |
| GET · POST | `/api/v1/clients` · `/{id}` | Detalle y alta |
| PATCH | `/api/v1/clients/{id}` | Edición parcial (el documento no se edita) |
| GET | `/api/v1/cases` | Listado con filtros y próximo vencimiento |
| GET · POST | `/api/v1/cases/{id}` · `/api/v1/cases` | Detalle y apertura (consecutivo automático) |
| PATCH | `/api/v1/cases/{id}` | Edición parcial |
| POST | `/api/v1/cases/{id}/close` | Cierre — exige desenlace |
| POST | `/api/v1/cases/{id}/reopen` | Reapertura (FIRM_OWNER, LAWYER) |
| GET · POST | `/api/v1/cases/{id}/events` | Bitácora del expediente |
| GET · POST | `/api/v1/cases/{id}/deadlines` | Términos del expediente |
| GET | `/api/v1/deadlines/upcoming?withinDays=7` | **Agenda de la firma**: qué vence |
| POST | `/api/v1/deadlines/{id}/complete` | Marca el término cumplido |

**Pipeline de leads** (todo el equipo de la firma)

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/v1/leads` | Listado con filtros por etapa, origen y abogado |
| GET | `/api/v1/leads/pipeline` | Conteo por etapa (tablero) |
| GET · POST | `/api/v1/leads/{id}` · `/api/v1/leads` | Detalle y alta manual |
| PATCH | `/api/v1/leads/{id}` | Edición parcial |
| POST | `/api/v1/leads/{id}/contacted` | Marca contactado |
| POST | `/api/v1/leads/{id}/convert` | **Crea el cliente** y opcionalmente su primer caso |
| POST | `/api/v1/leads/{id}/lost` | Marca perdido con motivo |

**Documentos, mensajería y agenda** (equipo de la firma)

| Método | Ruta | Qué hace |
|---|---|---|
| GET · POST | `/api/v1/cases/{id}/documents` | Archivos del caso · subida (multipart) |
| GET | `/api/v1/documents/{id}/download` | Descarga |
| PATCH | `/api/v1/documents/{id}/visibility` | Comparte u oculta al cliente |
| DELETE | `/api/v1/documents/{id}` | Borra (FIRM_OWNER, LAWYER) |
| PATCH | `/api/v1/cases/{id}/events/{eventId}/visibility` | Publica o retira una actuación |
| GET · POST | `/api/v1/cases/{id}/messages` | Hilo con el cliente · enviar |
| POST | `/api/v1/cases/{id}/messages/read` | Marca leído |
| GET | `/api/v1/appointments?from=&to=` | Agenda en una ventana de fechas |
| POST · PATCH | `/api/v1/appointments` · `/{id}` | Agendar · reprogramar |
| POST | `/api/v1/appointments/{id}/confirm` · `/cancel` · `/complete` | Cambios de estado |
| POST · DELETE | `/api/v1/clients/{id}/portal-access` | Da o revoca acceso al portal |

**Portal del cliente** (solo rol `CLIENT`; el id del cliente sale del token)

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/v1/portal/cases` | Sus casos, con mensajes sin leer |
| GET | `/api/v1/portal/cases/{id}` | Estado del caso: línea de tiempo publicada + documentos compartidos |
| GET · POST | `/api/v1/portal/cases/{id}/documents` | Ver compartidos · subir los suyos |
| GET | `/api/v1/portal/documents/{id}/download` | Descarga (doble comprobación) |
| GET · POST | `/api/v1/portal/cases/{id}/messages` | Hilo con su abogado |
| POST | `/api/v1/portal/cases/{id}/messages/read` | Marca leído |
| GET | `/api/v1/portal/appointments` | Sus próximas citas |
| POST | `/api/v1/portal/appointments/{id}/confirm` · `/cancel` | Confirma o cancela |

Errores en formato RFC 7807 (`application/problem+json`).

**Jobs programados** (`SchedulingConfig`, desactivados en el perfil `test`)

| Cuándo (America/Bogotá) | Qué hace |
|---|---|
| 00:30 diario | `DeadlineOverdueJob`: términos vencidos sin cumplir → `MISSED` |
| 03:00 diario | `TrialExpirationJob`: pruebas vencidas → `PAST_DUE` |

**Reglas que cruzan módulos:**

- Toda alta de miembro (abogado o asistente) pasa por
  `SubscriptionService.ensureCanAddMember(tenantId)` antes de crear nada.
- Desactivar a un miembro revoca sus refresh tokens en el acto. Sin eso seguiría
  renovando sesión; su access token vigente caduca solo en menos de 30 minutos.
- Todo cambio relevante de un expediente (apertura, cambio de estado, asignación,
  término registrado o cumplido, cierre, reapertura) escribe en `case_event`. La
  bitácora no es opcional: es lo que el cliente verá en su portal en la Fase 4.
- Convertir un lead crea cliente y, si se pide, su primer expediente **en una sola
  transacción**. O queda todo, o no queda nada a medias.
- Dar acceso al portal crea un `User` con rol `CLIENT`. **No cuenta** para el límite
  de miembros del plan: son los clientes de la firma, no su plantilla. Revocar el
  acceso desactiva ese usuario y revoca sus sesiones, sin borrar la ficha ni el
  historial.
- Subir o borrar un documento escribe en la bitácora del expediente.
- El binario se borra **después** de confirmar el borrado en BD: si la BD falla, el
  archivo sigue ahí. Al revés quedarían registros apuntando a la nada.

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

**Los documentos van en el volumen `documents`** (`/app/data/documents` en el
contenedor), así que sobreviven al redespliegue. Ese volumen **no está en la copia
de seguridad de la base de datos**: hay que respaldarlo aparte, o un `docker volume
rm` se lleva los expedientes.

### Requisito del entorno

El proyecto compila con **JDK 21**. En esta máquina `JAVA_HOME` apunta a `jdk-17`,
que también está instalado junto a `jdk-21`; hay que apuntarlo a
`C:\Program Files\Java\jdk-21`.

`<fork>true</fork>` en el `maven-compiler-plugin` es obligatorio: con el compilador
embebido de Maven, Lombok no puede acceder a los internos de `jdk.compiler` y no
genera nada (fallan todos los getters con `cannot find symbol`).
