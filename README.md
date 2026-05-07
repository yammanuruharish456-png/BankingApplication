# BankingApplication - UPI-like Payments Modular Monolith

Spring Boot 3.x implementation of a realistic UPI-style payment platform with strict ledger consistency, idempotency, and outbox-based messaging.

## Tech Stack
- Spring Boot 3.x
- MySQL + Flyway migrations
- RabbitMQ (outbox relay publisher + consumer)
- Spring Security 6 + JWT + refresh tokens
- Springdoc OpenAPI + Actuator

## Architecture
Layered modular monolith:
- controllers -> services/use-cases -> repositories
- `auth` module: register/login/refresh, JWT
- `payments` module: transfer, collect requests, UPI PIN verification, outbox, reconciliation
- `users` module: user profile transactions API + VPA ownership

## Core APIs
### Auth
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`

### Payments
- `POST /v1/payments/transfer` (`Idempotency-Key` required)

### Collect
- `POST /v1/collect`
- `POST /v1/collect/{requestId}/approve` (`Idempotency-Key` required)
- `POST /v1/collect/{requestId}/decline`

### User transactions
- `GET /v1/users/me/transactions`

## Transaction Consistency
Transfer/collect approval is executed in one DB transaction:
1. Resolve payer/payee VPA
2. Verify UPI PIN
3. Lock balances in deterministic account ID order (`SELECT ... FOR UPDATE`)
4. Create transaction `PENDING`
5. Insert double-entry ledger rows (`DEBIT`, `CREDIT`)
6. Update balances
7. Insert outbox event
8. Mark transaction `SUCCESS`

Ledger is source of truth. Reconciliation job verifies `balances` against ledger sums.

## Idempotency
`idempotency_keys` table keyed by `(user_id, idempotency_key, endpoint)` with request hash, status, and stored response payload.
Retries with same key + same payload return stored response.

## Runbook (Local)
### 1) Start infra + app
```bash
docker compose up --build
```

### 2) Run app directly
```bash
mvn clean spring-boot:run
```

### 3) OpenAPI + health
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`

## Example API Calls
Register payer:
```bash
curl -X POST http://localhost:8080/auth/register -H 'Content-Type: application/json' -d '{
  "username":"alice","email":"alice@example.com","password":"password123","upiPin":"1234","vpa":"alice@bank"
}'
```

Transfer:
```bash
curl -X POST http://localhost:8080/v1/payments/transfer \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Idempotency-Key: trf-001" \
  -H "Content-Type: application/json" \
  -d '{"payerVpa":"alice@bank","payeeVpa":"bob@bank","amount":10.00,"note":"Lunch","upiPin":"1234","clientRef":"client-001"}'
```

## CI/CD
### CI
`.github/workflows/ci.yml` runs `mvn clean verify`.

### CD (EC2 via SSH + Docker Compose)
`.github/workflows/cd-ec2.yml` deploys on push to `main`/manual dispatch.
Required secrets:
- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY`
- `EC2_APP_DIR`
