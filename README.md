# payadmin-bff

Backend-for-frontend for `payadmin-front`.

The service adapts `merchants-core` responses to the fixed SPA contract in
`../contracts/payadmin-bff/openapi.json`.

## API

Implemented endpoint:

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/merchants` | Merchant list for the admin SPA. |

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
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8080/realms/payadmin` | Keycloak issuer |
| `PAYADMIN_REQUIRED_AUTHORITY` | empty | Optional authority required for `/api/**` |
| `MERCHANTS_CORE_BASE_URL` | `http://localhost:8082` | Upstream base URL |
| `MERCHANTS_CORE_INTERNAL_ADMIN_HEADER` | `X-Internal-Admin-Key` | Internal credential header |
| `MERCHANTS_CORE_INTERNAL_ADMIN_API_KEY` | empty | Internal credential value |
| `MERCHANTS_CORE_CONNECT_TIMEOUT` | `2s` | Upstream connect timeout |
| `MERCHANTS_CORE_READ_TIMEOUT` | `5s` | Upstream read timeout |
| `PAYADMIN_UNKNOWN_MCC` | `0000` | Fallback MCC |

Config Server and Vault imports are optional by default for local development.
Production should inject secrets through Vault/config, not through source files.

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

## Tests

```powershell
mvn test
mvn clean verify
```

