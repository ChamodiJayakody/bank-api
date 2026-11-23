# Bank Accounts & Transactions API

[![Build and test](https://github.com/ChamodiJayakody/bank-api/actions/workflows/build.yml/badge.svg)](https://github.com/ChamodiJayakody/bank-api/actions/workflows/build.yml)

A Spring Boot REST API for opening bank accounts, depositing, withdrawing and transferring money between accounts, backed by PostgreSQL.
It enforces real money rules (no overdraft, atomic transfers, exact decimal amounts) and is covered by unit, Mockito and MockMvc tests that run on every push with GitHub Actions.

## Tech stack

| Area | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.2 (Web MVC, Data JPA, Validation) |
| Build | Maven (with Maven Wrapper) |
| Database | PostgreSQL (app), H2 in-memory (tests only) |
| Testing | JUnit 5, Mockito, AssertJ, MockMvc |
| CI | GitHub Actions |

## How to run the app

**Prerequisites:** Java 21 and a running PostgreSQL server.

1. Create the database:
   ```sql
   CREATE DATABASE bankdb;
   ```
2. Set the database password as an environment variable (it is never stored in the repository):

   PowerShell:
   ```
   $env:DB_PASSWORD='your-password'
   ```
   Bash:
   ```
   export DB_PASSWORD='your-password'
   ```
   The username defaults to `postgres`; override it with `DB_USERNAME` if needed.
3. Start the app:
   ```
   ./mvnw spring-boot:run
   ```
   The API runs on `http://localhost:8080`. Tables are created automatically on startup.

Connection settings live in `src/main/resources/application.properties`.

## How to run the tests

```
./mvnw test
```

**23 tests**, none of which need PostgreSQL:

| Test class | Type | Tests |
|---|---|---|
| `AccountServiceTest` | Unit test, Mockito fakes the repositories | 11 |
| `TransferServiceTest` | Unit test, Mockito | 4 |
| `AccountControllerTest` | `@WebMvcTest` + MockMvc | 5 |
| `TransferControllerTest` | `@WebMvcTest` + MockMvc | 2 |
| `BankApiApplicationTests` | Spring context loads (H2) | 1 |

The same command runs in CI on every push and pull request.

## API endpoints

| Method and path | Purpose | Success | Errors |
|---|---|---|---|
| `POST /api/accounts` | Create an account | 201 | 400 invalid input, 409 duplicate account number |
| `GET /api/accounts/{id}` | Get one account | 200 | 404 not found |
| `POST /api/accounts/{id}/deposit` | Add money | 200 | 400 invalid amount, 404 |
| `POST /api/accounts/{id}/withdraw` | Take money out | 200 | 400 invalid amount, 404, 422 insufficient funds |
| `POST /api/transfers` | Move money between two accounts | 201 | 400, 404, 422 insufficient funds |
| `GET /api/accounts/{id}/transactions` | List an account's transactions (newest first) | 200 | 404 |

**Example: create an account**

```http
POST /api/accounts
Content-Type: application/json

{"accountNumber": "ACC-001", "ownerName": "Chamodi", "openingBalance": 100.00}
```

**Every error returns the same JSON shape** (no stack traces or internal details):

```json
{
  "timestamp": "2026-11-23T08:30:00Z",
  "status": 422,
  "error": "Unprocessable Content",
  "message": "Insufficient funds in account 1"
}
```

## Business rules

1. Money uses `BigDecimal`, never `double`, with two decimal places.
2. The amount of every deposit, withdrawal and transfer must be greater than zero.
3. A withdrawal or transfer cannot take the balance below zero (no overdraft).
4. The opening balance cannot be negative.
5. A transfer needs two different accounts, and it either fully succeeds or fully fails (`@Transactional`).
6. Every successful operation writes a transaction record.
7. An unknown account returns a clear not-found error.
8. Account numbers are unique (checked in the service and enforced by a database constraint).

## Project structure

```
src/main/java/com/chamodi/bank_api/
  controller/   HTTP layer: AccountController, TransferController
  service/      Business rules: AccountService, TransferService
  repository/   Spring Data JPA repositories
  domain/       JPA entities: Account, Transaction, TransactionType
  dto/          Request and response records with validation
  exception/    Custom exceptions + GlobalExceptionHandler
```

Controllers handle HTTP, services hold the rules, and repositories talk to the database, so each layer can be tested on its own.

## Limitations and next steps

- No authentication or authorization yet (next: Spring Security with JWT).
- No concurrency control: two simultaneous withdrawals from the same account could both pass the balance check. Next: optimistic locking with a `@Version` field.
- The schema is created by Hibernate (`ddl-auto=update`). Next: versioned migrations with Flyway.
- The service tests use mocks, so they prove the rules run before any change, but not the database rollback itself. Next: an integration test with `@SpringBootTest` that proves a failed transfer is rolled back.
- No Docker setup yet. Next: Docker Compose for the app and PostgreSQL.
- No pagination on the transaction history endpoint.
