# Backend – Developer Documentation

## 1. Overview

The backend system for **CityXplore** is a Kotlin/Spring Boot 3 application designed following the *Clean Architecture*
and *Package by Feature* convention.  
Its primary purposes:

- Manage user authentication and data through **Supabase Auth + Postgres**
- Expose REST API for map exploration, achievements, and social features
- Provide developer and admin endpoints for diagnostics and maintenance

---

## 2. Technology stack

| Layer         | Technology                                                  |
|---------------|-------------------------------------------------------------|
| Language      | Kotlin (Gradle KTS build)                                   |
| Framework     | Spring Boot 3                                               |
| Database      | PostgreSQL (Hosted via Supabase + PostGIS)                  |
| ORM           | Hibernate / Spring Data JPA                                 |
| Auth          | Supabase Auth (HS256 JWT + Spring Security Resource Server) |
| CI/CD         | GitHub Actions                                              |
| Storage       | Supabase Storage (object bucket)                            |
| Documentation | Swagger / Springdoc OpenAPI                                 |

---

## 3. Package structure

```
org.cityxplore.backend
 ├── common/           → utilities shared across modules (JwtUtils, exceptions, config)
 │
 ├── system/           → system-level endpoints (/ping, /environment, /actuator)
 │
 ├── user/             → user profile, account, synchronization with Supabase Auth
 │
 ├── poi/              → points of interest management (admin + map data)
 │
 ├── discoveries/      → POI discovery system (users discovering points)
 │
 ├── achievements/     → achievement engine (definitions + user achievements)
 │
 ├── social/           → friendships and shared POIs
 │
 ├── storage/          → Supabase Storage integration (upload, signed URLs)
 │
 └── security/         → JWT auth configuration, filters, and CORS rules
```

> Each feature‑package includes its own `entity`, `dto`, `repository`, `service`, `controller`.

Example:

```
poi/
├── entity/PointOfInterest.kt
├── dto/PoiAdminResponse.kt
├── repository/PointOfInterestRepository.kt
├── service/PoiService.kt
└── controller/PoiController.kt
```

---

## 4. Authentication

CityXplore backend uses Supabase Auth tokens (`HS256`) validated locally via secret key:

- Secret: stored in ENV → `SUPABASE_JWT_SECRET`
- Verified by `SecurityConfig` using `NimbusJwtDecoder`
- Audience = `authenticated`
- Issuer = `https://<project-id>.supabase.co/auth/v1`

All protected endpoints require header:
`Authorization: Bearer <access_token>`

Public endpoints (`/api/public/**`, `/actuator/**`) are accessible without auth.

---

## 5. Layer responsibilities

| Layer              | Responsibility                                    |
|--------------------|---------------------------------------------------|
| **Controller**     | Handle HTTP requests, validate input, return DTOs |
| **Service**        | Contain business logic, transactions              |
| **Repository**     | Data access via Spring Data JPA                   |
| **Entity**         | ORM model, direct database mapping                |
| **DTO**            | Data Transfer Objects, isolate API from entities  |
| **Mapper / Utils** | Convert entities ↔ DTOs                           |

---

## 6. DTO and exception conventions

- Controllers **never** expose JPA entities directly.
- Every public API uses DTO objects with clear naming:
    - `<FeatureName>Dto` → representation
    - `Create<FeatureName>Dto` | `Update<FeatureName>Dto` → input.
- Validation exceptions → thrown as `ResponseStatusException`.

---

## 7. API endpoint organization

### User

```
GET   /api/users/me               → current profile
PATCH /api/users/me               → update avatar / username
```

### POI & Discoveries

```
GET   /api/pois                   → all POIs (active)
POST  /api/pois/{id}/discover     → discover point
GET   /api/pois/discoveries       → list discoveries of user
```

### Achievements

```
GET   /api/achievements             → all achievements
GET   /api/achievements/mine        → user's achievements
POST  /api/achievements/{id}/grant  → grant manually (debug)
```

### Social

```
POST  /api/friends/{id}/invite
POST  /api/friends/{id}/accept
GET   /api/friends
POST  /api/share/{poiId}
GET   /api/shared
```

### Storage

```
GET    /api/storage/url?bucket=&path=
DELETE /api/storage?bucket=&path=
```

### System / Admin

```
GET  /api/public/ping
GET  /api/public/environment
GET  /api/admin/stats
POST /api/admin/reset
```

---

## 8. Health & Monitoring

Using **Spring Boot Actuator**:

```
/actuator/health      → UP/DOWN state
/actuator/info        → build & version info
/actuator/metrics     → JVM, system metrics
```

Configurable in `application.yml`:

```yaml
management:
  endpoints.web.exposure.include: health,info,metrics
  endpoint.health.show-details: always
```

---

## 9. Environment variables

| Key                             | Description                                       |
|---------------------------------|---------------------------------------------------|
| `SUPABASE_JWT_SECRET`           | Signing secret for HS256 JWT verification         |
| `SUPABASE_SERVICE_KEY`          | Service‑role key for Supabase Storage operations  |
| `SUPABASE_STORAGE_URL`          | Base URL of Supabase project                      |
| `SUPABASE_DB_USER` / `PASSWORD` | DB credentials (via pooler)                       |
| `SPRING_PROFILES_ACTIVE`        | `dev` / `prod`                                    |
| `APP_VERSION`                   | from Gradle build, shown in `/public/environment` |

`.env` file (local only, ignored in Git):

```
SUPABASE_DB_USER=xxxx
SUPABASE_DB_PASSWORD=xxxx
SUPABASE_PROJECT_REF=xxxx
SUPABASE_JWT_ISSUER=xxxx
SUPABASE_DIRECT_USER=xxxx
SUPABASE_PUBLISHABLE_KEY=xxxx
SUPABASE_SECRET_KEY=xxxx
```

---

## 10. Testing conventions

- Unit tests → per feature (poi/PoiServiceTests.kt)
- Integration tests → verify REST (PoiControllerTests.kt, AchievementsControllerTests.kt)
- For secured endpoints, create stub tokens using spring-security-test:

```kotlin
  with(mockJwt().jwt { it.subject("user-id") })
```

---

## 11. Build & Run

### Local build

`./gradlew bootRun --args='--spring.profiles.active=dev'`

--- 

## 12. Security summary

| Level    | Mechanism                      | Example                              |
|----------|--------------------------------|--------------------------------------|
| API      | JWT HS256 verification         | `/api/users/me` requires valid token |
| DB       | Row Level Security in Supabase | `auth.uid() = user_id` policies      |
| Internal | role‑based guards              | `/api/admin/**` → `ROLE_ADMIN`       |

---

## 13. Coding conventions

- Kotlin idiomatic (data classes, immutable DTOs, var‑only mutable JPA fields).
- Naming: PascalCase for classes, camelCase for variables.
- REST nouns, lowercase paths, plural collections (/api/users).
- Services annotated @Transactional.
- Controllers return ResponseEntity<…> or raw DTO.

---

## 14. Adding a new feature module

1. Create a new package under org.cityxplore.backend.<feature>
2. Add:
    - entity, dto, repository, service, controller
3. Add Flyway migration (SQL in resources/db/migration/).
4. Register RLS policies in Supabase.
5. Secure endpoints by role or JWT
6. Add tests and update API README.
