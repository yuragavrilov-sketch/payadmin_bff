# payadmin-bff

Backend-for-frontend for `payadmin-front`.

The service adapts `merchants-core` responses to the fixed SPA contract in
`../contracts/payadmin-bff/openapi.json`.

## API

Implemented browser-facing endpoints:

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/merchants` | Merchant list for the admin SPA. |
| `GET` | `/api/v1/limits/operation-types` | Operation type catalog for limit administration. |
| `GET` | `/api/v1/limits/rule-dictionaries` | Selector dictionaries for rule forms. |
| `GET` | `/api/v1/limits/rules` | List limit rules. |
| `POST` | `/api/v1/limits/rules` | Create a draft rule. |
| `PATCH` | `/api/v1/limits/rules/{ruleId}` | Patch a draft rule. |
| `POST` | `/api/v1/limits/rules/{ruleId}/activate` | Activate a draft rule. |
| `POST` | `/api/v1/limits/rules/{ruleId}/disable` | Disable an active rule. |
| `POST` | `/api/v1/limits/rules/{ruleId}/new-version` | Create a new draft rule version. |
| `POST` | `/api/v1/limits/rule-manifests` | Compile active rules into the latest valid manifest. |
| `GET` | `/api/v1/limits/rule-manifests/latest` | Read the latest valid manifest. |
| `GET` | `/api/v1/limits/rule-manifests/{manifestId}` | Read a manifest by ID. |
| `GET` | `/api/v1/limits/runtime-manifests` | List runtime manifest lifecycle history. |
| `POST` | `/api/v1/limits/runtime-manifests` | Compile and schedule an engine-facing runtime manifest. |
| `GET` | `/api/v1/limits/runtime-manifests/active` | Read the active runtime manifest at a UTC instant. |
| `POST` | `/api/v1/limits/runtime-manifests/{manifestId}/rollback` | Create a scheduled runtime manifest from an older payload. |
| `DELETE` | `/api/v1/sbp/upstreams/{id}` | Mark an SBP upstream for removal (pass-through to `sbp-router-management`). |
| `DELETE` | `/api/v1/sbp/extraction-rules/{id}` | Mark an SBP extraction rule for removal (pass-through to `sbp-router-management`). |

The response uses the shared envelope:

```json
{
  "data": [],
  "meta": {
    "limit": 100,
    "offset": 0,
    "count": 0,
    "search": null,
    "status": null,
    "sortBy": "id",
    "sortDir": "asc"
  },
  "error": null,
  "timestamp": "2026-05-25T20:00:00Z"
}
```

## Security

`payadmin-front` obtains Keycloak access tokens directly from Keycloak.
`payadmin-bff` does not proxy login, refresh, logout, or password grant calls.

All `/api/**` endpoints validate the incoming `Authorization: Bearer <token>`
as a Keycloak JWT through Spring Security OAuth2 Resource Server.

Runtime behavior:

- missing, malformed, expired, revoked, or unverifiable token returns `401`;
- valid token without configured authority returns `403`;
- browser tokens are not forwarded to `merchants-core`;
- upstream calls use the configured internal admin credential instead.

The security boundary is recorded in
`../docs/adr/0002-payadmin-bff-security-boundary.md`.

## Architecture

The service uses hexagonal architecture by frontend capability, as recorded in
`../docs/adr/0003-payadmin-bff-hexagonal-capability-packaging.md`.

Current package shape:

```text
ru.copperside.payadmin
  common
  security
  merchant
    domain
    application
      port.out
    adapter.in.web
    adapter.out.merchantscore
    config
```

## Mapping

The BFF consumes:

- `GET /api/v1/merchants/configurations/active-line`;
- `GET /api/v1/merchants/{merchantId}/configurations`.

It maps upstream data to the frontend fields:

| Front field | Source | Transform |
|---|---|---|
| `id` | `mercId` | `MRC-` plus zero-padded 5 digit id |
| `name` | `name` | passthrough |
| `status` | active configuration | derived as `active`, `suspended`, or `blocked` |
| `mcc` | config parameter | `MCC`, `MERCHANT_MCC`, or `MERCHANTCATEGORYCODE`; fallback `0000` |
| `createdAt` | active configuration | earliest `dateBegin` |

## Configuration

Main environment variables:

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8083` | HTTP port |
| `PAY_ENVIRONMENT` | profile default: `local`, `test`, or `prod` | Environment id used for Config Server label and Vault paths |
| `CONFIG_SERVER_URL` | `http://pay-payconfig-server:8080` | Config Server URL |
| `CONFIG_SERVER_ENABLED` | `false` locally, `true` in prod profile | Enables Config Server client |
| `CONFIG_SERVER_LABEL` | `${PAY_ENVIRONMENT}` | Config Server git branch/label |
| `VAULT_ENABLED` | `false` locally, `true` in prod profile | Enables Vault config import |
| `VAULT_KV_BACKEND` | `pay` | Vault KV mount for shared environments |
| `VAULT_KV_CONTEXTS` | `${PAY_ENVIRONMENT}/payadmin-bff-merchants-core-internal-admin-key` | Comma-separated exact Vault contexts |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8080/realms/payadmin` | Keycloak issuer |
| `PAYADMIN_REQUIRED_AUTHORITY` | empty | Optional authority required for `/api/**` |
| `MERCHANTS_CORE_BASE_URL` | `http://localhost:8082` | Upstream base URL |
| `MERCHANTS_CORE_INTERNAL_ADMIN_HEADER` | `X-Internal-Admin-Key` | Internal credential header |
| `MERCHANTS_CORE_INTERNAL_ADMIN_API_KEY` | empty | Internal credential value |
| `MERCHANTS_CORE_CONNECT_TIMEOUT` | `2s` | Upstream connect timeout |
| `MERCHANTS_CORE_READ_TIMEOUT` | `5s` | Upstream read timeout |
| `SBP_ROUTER_MANAGEMENT_BASE_URL` | `http://localhost:8087` | `sbp-router-management` upstream base URL (in k8s use the `:8080` Service port) |
| `SBP_ROUTER_MANAGEMENT_INTERNAL_ADMIN_HEADER` | `X-Internal-Admin-Key` | Internal credential header |
| `SBP_ROUTER_MANAGEMENT_INTERNAL_ADMIN_API_KEY` | empty | Internal credential value (defaults to the shared `${sbp-router.admin-key}` Vault secret) |
| `SBP_ROUTER_MANAGEMENT_CONNECT_TIMEOUT` | `2s` | Upstream connect timeout |
| `SBP_ROUTER_MANAGEMENT_READ_TIMEOUT` | `5s` | Upstream read timeout |
| `PAYADMIN_UNKNOWN_MCC` | `0000` | Fallback MCC |

Config Server and Vault imports are optional by default for local development.
Production and shared test deployments should make imports mandatory with:

```text
CONFIG_SERVER_ENABLED=true
VAULT_ENABLED=true
SPRING_CONFIG_IMPORT=configserver:${CONFIG_SERVER_URL},vault://
```

Config Server stores non-secret settings on the branch selected by
`CONFIG_SERVER_LABEL`. Vault stores secrets under the `pay` KV mount, one path
per secret, for example:

```text
pay/prod/payadmin-bff-merchants-core-internal-admin-key
```

The secret value should use the target property key:

```yaml
payadmin-bff.merchants-core.internal-admin-api-key: <secret>
```

## Local Run

```powershell
mvn spring-boot:run
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8083/actuator/health
```

OpenAPI:

```text
http://localhost:8083/v3/api-docs
http://localhost:8083/swagger-ui.html
```

## Docker Compose Run

The full local contour is owned by `../infra/docker-compose.yaml`.

```powershell
cd ..\infra
docker compose up -d --build payadmin-bff
```

The container uses `SPRING_PROFILES_ACTIVE=compose`, Config Server label
`compose`, and Vault secret
`pay/compose/payadmin-bff-merchants-core-internal-admin-key`.

For browser token compatibility, the compose config keeps the issuer as
`http://localhost:8080/realms/payadmin` and sets the JWKS URI to
`http://keycloak:8080/realms/payadmin/protocol/openid-connect/certs`.
Browser bearer tokens are still validated inbound and are not forwarded to
internal services.

## Tests

```powershell
mvn test
mvn clean verify
```
