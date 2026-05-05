# UPI Banking Application

A production-ready UPI-like banking application built with Spring Boot 3.2.x, Java 17, MySQL, RabbitMQ, and JWT authentication.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        Client                               │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTPS
┌───────────────────────▼─────────────────────────────────────┐
│              Spring Boot Application (Port 8080)            │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │   Auth   │  │ Accounts │  │  VPA     │  │ Payments │   │
│  │ /auth/** │  │/v1/accts │  │ /v1/vpa  │  │/v1/pay.. │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Spring Security + JWT Filter            │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌──────────────────┐    ┌──────────────────────────────┐  │
│  │  Outbox Publisher │    │  Reconciliation Job (hourly) │  │
│  │  (every 5s)       │    │                              │  │
│  └──────────────────┘    └──────────────────────────────┘  │
└────────────────┬──────────────────┬─────────────────────────┘
                 │                  │
        ┌────────▼───┐      ┌───────▼──────┐
        │  MySQL 8.x  │      │  RabbitMQ    │
        │  (Flyway)   │      │  upi.events  │
        └────────────┘      └──────────────┘
```

### Key Design Patterns
- **Transactional Transfer**: All transfer operations execute in a single DB transaction
- **Outbox Pattern**: Events published to RabbitMQ via outbox table (at-least-once delivery)
- **Idempotency**: All payment endpoints accept `Idempotency-Key` header
- **Pessimistic Locking**: Balance rows locked with `SELECT FOR UPDATE` during transfers
- **Optimistic Locking**: `@Version` on Balance entity as secondary protection

## Tech Stack
- Spring Boot 3.2.4 / Java 17
- MySQL 8.x + Flyway migrations
- Spring Security + JWT (jjwt 0.12.3)
- Spring AMQP + RabbitMQ
- SpringDoc OpenAPI 3 (Swagger UI)
- Spring Boot Actuator
- Lombok
- JUnit 5 + Mockito + H2

## Running Locally with Docker Compose

### Prerequisites
- Docker and Docker Compose installed
- Java 17 + Maven (for building)

### Steps

1. **Build the application JAR:**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Start all services:**
   ```bash
   docker-compose up -d
   ```

3. **Check logs:**
   ```bash
   docker-compose logs -f app
   ```

4. **Access the API:**
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Health: http://localhost:8080/actuator/health
   - RabbitMQ Management: http://localhost:15672 (guest/guest)

5. **Stop services:**
   ```bash
   docker-compose down
   ```

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Login (returns JWT tokens) |
| POST | `/auth/refresh` | Refresh access token |

### Accounts
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/accounts` | Link a bank account |
| GET | `/v1/accounts/me` | Get my accounts |

### VPA (Virtual Payment Address)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/vpa` | Create a VPA |
| GET | `/v1/vpa/{handle}` | Resolve a VPA |

### PIN
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/pin/set` | Set or update UPI PIN |

### Payments
| Method | Endpoint | Description | Headers |
|--------|----------|-------------|---------|
| POST | `/v1/payments/transfer` | Execute transfer | `Idempotency-Key` required |
| GET | `/v1/payments/{txnId}` | Get transaction by ID | |

### Collect (Request Money)
| Method | Endpoint | Description | Headers |
|--------|----------|-------------|---------|
| POST | `/v1/collect` | Create collect request | |
| POST | `/v1/collect/{id}/approve` | Approve collect request | `Idempotency-Key` required |
| POST | `/v1/collect/{id}/decline` | Decline collect request | |

### Transaction History
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/users/me/transactions` | Paginated transaction history |

## Transfer Flow

```
POST /v1/payments/transfer
  1. Check idempotency key → return cached if exists
  2. Resolve payer & payee VPAs
  3. Verify UPI PIN (lockout after 3 failures)
  4. SELECT FOR UPDATE on balance rows
  5. Check sufficient funds
  6. Create Transaction (PENDING)
  7. Insert 2 LedgerEntry rows (DEBIT + CREDIT)
  8. Update both Balance rows
  9. Insert OutboxEvent
 10. Update Transaction → SUCCESS
 11. Save IdempotencyKey response
 12. Save AuditLog
```

## AWS EC2 Deployment

### Prerequisites
- EC2 instance (t3.small or larger) with Ubuntu 22.04
- Docker and Docker Compose installed on EC2
- Security group: ports 22 (SSH), 8080 (App), 3306 (MySQL - optional), 5672/15672 (RabbitMQ - optional)

### Manual Deployment

1. **SSH into EC2:**
   ```bash
   ssh -i your-key.pem ubuntu@<EC2_HOST>
   ```

2. **Install Docker:**
   ```bash
   sudo apt update && sudo apt install -y docker.io docker-compose-plugin
   sudo usermod -aG docker ubuntu
   ```

3. **Copy files:**
   ```bash
   mkdir -p ~/banking-app/target
   scp -i your-key.pem target/banking-app.jar ubuntu@<EC2_HOST>:~/banking-app/target/
   scp -i your-key.pem docker-compose.yml ubuntu@<EC2_HOST>:~/banking-app/
   scp -i your-key.pem Dockerfile ubuntu@<EC2_HOST>:~/banking-app/
   ```

4. **Deploy:**
   ```bash
   cd ~/banking-app
   docker compose up -d
   ```

### GitHub Actions Secrets Setup

Configure these secrets in your GitHub repository (`Settings → Secrets and variables → Actions`):

| Secret | Description |
|--------|-------------|
| `EC2_HOST` | EC2 instance public IP or hostname |
| `EC2_USER` | SSH username (e.g., `ubuntu`) |
| `EC2_SSH_KEY` | Private SSH key (PEM format, full content) |

### Environment Variables (Production)

Set these in your deployment environment or docker-compose override:

```bash
DB_URL=jdbc:mysql://mysql:3306/upi_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USER=your_db_user
DB_PASSWORD=your_secure_password
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=your_rabbitmq_user
RABBITMQ_PASS=your_secure_password
JWT_SECRET=your_256bit_hex_secret
SERVER_PORT=8080
```

## Database Schema

The application uses 9 Flyway migration scripts:
- `V1` - users table
- `V2` - accounts + balances tables
- `V3` - vpas table
- `V4` - upi_pins table
- `V5` - transactions + ledger_entries tables
- `V6` - collect_requests table
- `V7` - outbox_events table
- `V8` - idempotency_keys table
- `V9` - audit_log table

## Running Tests

```bash
# Run all tests (unit + integration with H2)
mvn test -Dspring.profiles.active=test

# Run only unit tests
mvn test -Dspring.profiles.active=test -Dtest="AuthServiceTest,PinServiceTest,TransferServiceTest"

# Run integration tests
mvn test -Dspring.profiles.active=test -Dtest="UpiApplicationIntegrationTest"
```

## Security Considerations

- Passwords hashed with BCrypt
- UPI PINs hashed with BCrypt (never stored in plain text)
- PIN lockout after 3 failed attempts (30-minute lockout)
- JWT tokens with configurable expiry (default: 24h access, 7d refresh)
- All endpoints (except `/auth/**`, health, Swagger) require authentication
- Idempotency keys prevent duplicate transactions
- Database transactions with pessimistic locking prevent race conditions
