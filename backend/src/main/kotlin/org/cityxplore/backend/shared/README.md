# shared module

Cross-cutting infrastructure and utilities that are domain-agnostic.

Design rules:

- shared DOES NOT import domain packages (user, poi, achievements, discoveries, storage, social).
- contains security configuration, global exception handling, common web utilities, minor system endpoints.
- domain modules may depend on shared, but not the other way around.

Examples:

- `shared.security.SecurityConfig`, `shared.security.JwtUtils`
- `shared.exception.GlobalExceptionHandler`

Operational notes:

- Keep endpoints that operate on domain data (e.g., admin stats, resets) OUT of shared (they belong to `system` or a
  specific domain).
