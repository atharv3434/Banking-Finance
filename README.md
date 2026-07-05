# Banking Finance Microservice

A production-ready REST API microservice for banking operations built with **Spring Boot 3**, **Java 17**, **JPA/Hibernate**, and **H2** (swap for PostgreSQL in production).

---

## Quick Start

```bash
# Build & run
mvn spring-boot:run

# Or build JAR and run
mvn clean package -DskipTests
java -jar target/banking-api-1.0.0.jar

# Run tests
mvn test
```

API is available at: `http://localhost:8080`  
H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:bankingdb`)

---

## Project Structure

```
src/main/java/com/banking/
├── BankingApiApplication.java     # Entry point
├── controller/
│   └── AccountController.java     # REST endpoints
├── service/
│   └── AccountService.java        # Business logic
├── repository/
│   ├── AccountRepository.java
│   └── TransactionRepository.java
├── model/
│   ├── Account.java               # Account entity
│   └── Transaction.java           # Transaction entity
├── dto/
│   ├── AccountDto.java            # Request/response DTOs
│   └── TransactionDto.java
└── exception/
    ├── AccountNotFoundException.java
    ├── BankingException.java
    └── GlobalExceptionHandler.java
```

---

## API Reference

### Accounts

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/accounts` | Create account |
| `GET` | `/api/v1/accounts` | List all accounts |
| `GET` | `/api/v1/accounts?search=Alice` | Search by owner name |
| `GET` | `/api/v1/accounts/{id}` | Get account by ID |
| `GET` | `/api/v1/accounts/number/{num}` | Get by account number |
| `GET` | `/api/v1/accounts/{id}/balance` | Get balance |
| `DELETE` | `/api/v1/accounts/{id}` | Close account |

### Transactions

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/accounts/{id}/deposit` | Deposit funds |
| `POST` | `/api/v1/accounts/{id}/withdraw` | Withdraw funds |
| `POST` | `/api/v1/accounts/{id}/transfer` | Transfer to another account |
| `GET` | `/api/v1/accounts/{id}/transactions` | Paginated transaction history |

---

## Example Requests

### Create Account
```http
POST /api/v1/accounts
Content-Type: application/json

{
  "ownerName": "Jane Doe",
  "accountType": "CHECKING",
  "initialDeposit": 1000.00,
  "currency": "USD"
}
```

### Deposit
```http
POST /api/v1/accounts/1/deposit
Content-Type: application/json

{
  "amount": 500.00,
  "description": "Paycheck"
}
```

### Transfer
```http
POST /api/v1/accounts/1/transfer
Content-Type: application/json

{
  "toAccountNumber": "ACC-002-SAV",
  "amount": 250.00,
  "description": "Rent"
}
```

### Get Transaction History
```http
GET /api/v1/accounts/1/transactions?page=0&size=10
```

---

## Account Types
- `CHECKING` — everyday spending account
- `SAVINGS` — interest-bearing savings account
- `INVESTMENT` — investment/brokerage account

## Account Status
- `ACTIVE` — normal operation
- `SUSPENDED` — temporarily disabled
- `CLOSED` — permanently closed (requires zero balance)

---

## Production Upgrade Checklist

- [ ] Replace H2 with PostgreSQL (`spring.datasource.*` in `application.properties`)
- [ ] Add Spring Security + JWT authentication
- [ ] Add rate limiting (Bucket4j or similar)
- [ ] Add Flyway/Liquibase for DB migrations
- [ ] Enable HTTPS
- [ ] Add Actuator endpoints for health/metrics
- [ ] Wire up Prometheus + Grafana for observability
- [ ] Containerise with Docker (`FROM eclipse-temurin:17-jre`)


