# Digital Wallet Backend

A Spring Boot application for managing digital wallets and user-to-user money transfers.

The application provides REST APIs for user registration, authentication, adding money, transferring funds, and viewing transaction history. It also includes an admin role for accessing wallet and transaction records across the system.

The main focus of this implementation is maintaining wallet balance consistency, securing user-specific operations, and handling repeated or concurrent requests correctly.


## Application Design

The application follows a layered architecture. Controllers handle incoming requests, services contain the business logic, and repositories communicate with the database.

```text
Client (Postman)
      |
      v
REST Controller
      |
      v
Service Layer
      |
      v
Repository Layer
      |
      v
H2 Database
```

The security layer validates JWT tokens and checks the user's role before allowing access to protected endpoints.

### Package Responsibilities

| Package | Responsibility |
|---|---|
| `controller` | REST endpoints and request handling |
| `service` | Wallet and transaction business logic |
| `repository` | Database access using Spring Data JPA |
| `entity` | JPA entity classes |
| `dto` | Request and response objects |
| `security` | JWT authentication and authorization |
| `config` | Application and security configuration |
| `exception` | Custom exceptions and error handling |


## Domain Model

The application uses three primary tables.

### User


| Field | Description |
|---|---|
| id | Unique user identifier |
| email | User's email address |
| password | Encrypted password |
| role | USER or ADMIN |

### Wallet


| Field | Description |
|---|---|
| id | Unique wallet identifier |
| user_id | Associated user |
| balance | Current wallet balance |
| version | Version used for concurrency control |

A wallet is created automatically when a user registers. Each user is associated with one wallet.

### Transaction

It Stores the record of wallet operations.

| Field | Description |
|---|---|
| id | Transaction identifier |
| from_wallet | Source wallet |
| to_wallet | Destination wallet |
| amount | Transaction amount |
| type | ADD or TRANSFER |
| status | SUCCESS or FAILED |
| idempotency_key | Request identification key |
| created_at | Transaction creation time |

## Transaction Flow

### Adding Money

When an authenticated user sends a request to `/wallet/add`, the application identifies the user's wallet from the authentication context.

The requested amount is validated, the wallet balance is updated, and a transaction record is maintained.

The client/user must provide an `Idempotency-Key` so that a retry of the same request does not result in another credit.(to avoid multiple touch at ADD)

### Transferring Money

A transfer involves two wallets and must be treated as a single business operation.

The general flow is:

1. Identify the sender using the authenticated user.
2. Find the receiver using the receiver email.
3. Validate the transfer amount and wallet details.
4. Check the sender's available balance.
5. Debit the sender's wallet.
6. Credit the receiver's wallet.
7. Record the transaction.

The debit and credit operations must execute within the same database transaction. If the operation fails, the database changes are rolled back.

Wallet locking/versioning is used to protect balance updates when requests overlap.

## Idempotency

The wallet APIs accept an `Idempotency-Key` header for money-related operations.

Consider a situation where a transfer request reaches the server, but the client does not receive the response because of a network timeout. The client may retry the request.

Without idempotency, that retry could process the transfer again.

The system uses the provided key to identify an already-processed operation and return its existing result rather than performing the operation a second time. Database-level uniqueness is also used to protect against duplicate keys.

## Authentication and Authorization

Authentication is handled through Spring Security and JWT.

The registration endpoint creates a user with an encrypted password and automatically creates the associated wallet.

After successful login, the application returns a JWT. The client sends this token in the Authorization header for protected endpoints.

```http
Authorization: Bearer JWT_TOKEN
```

There are two module:

- `USER` – Can access their own wallet and transaction history.
- `ADMIN` – Can access all wallets and transactions.

The sender's wallet is determined from the authenticated identity rather than accepting a wallet ID from the client.

## API Reference

All endpoints use the base URL:

```text
http://localhost:8080
```

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register a user and create a wallet |
| POST | `/auth/login` | Authenticate and receive JWT |

### Wallet Operations

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/wallet/add` | USER | Add money |
| POST | `/wallet/transfer` | USER | Transfer money |
| GET | `/wallet` | USER | Get wallet details |
| GET | `/wallet/transactions` | USER | Get paginated transactions |

### Administration

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/admin/wallets` | ADMIN | Retrieve all wallets |
| GET | `/admin/transactions` | ADMIN | Retrieve all transactions |


### Register a users

`POST /auth/register`

user1:
```json
{
  "email": "shreyash@gmail.com",
  "password": "Shreyash@12345"
}
```

user2:

```json
{
  "email": "sanket@gmail.com",
  "password": "Sanket@12345"
}
```

A wallet is created for each successfully registered user.

### Login

`POST /auth/login`
user1:
```json
{
  "email": "shreyash@gmail.com",
  "password": "Shreyash@12345"
}
```

Use the  JWT token for  authenticated requests.
ADD Authorization Bariar token

### Add money

`POST /wallet/add`

Header:

```http
Idempotency-Key: ADD-shreyash-1
```

Request:

```json
{
  "amount": 1000
}
```

### Transfer money

`POST /wallet/transfer`

Example: Shreyash transfers 300 to Sanket.

Header:

```http
Idempotency-Key: TRANSFER-shreyash-1
```

Request:

```json
{
  "receiverEmail": "sanket@gmail.com",
  "amount": 300
}
```

### Get wallet details

`GET /wallet`

Returns the wallet belonging to the authenticated user.

### Get transaction history

`GET /wallet/transactions?page=0&size=10`

Returns the authenticated user's transactions.

### Admin endpoints

```http
GET /admin/wallets
GET /admin/transactions
```

These endpoints require an ADMIN account.

## Configuration

Application configuration is maintained in:

```text
src/main/resources/application.yml
```

The default admin configuration is:

```yaml
email: ${ADMIN_EMAIL:admin@wallet.local}
password: ${ADMIN_PASSWORD:admin@12345}
```

The application uses H2 as its database. Check the existing `application.yml` for the configured database URL and H2 console settings.


## Testing Scenarios



- Register two users and verify that both wallets are created.
- Log in and access the wallet using the returned JWT.
- Add money and verify the updated balance.
- Transfer money between Shreyash and Sanket.
- Retry a request using the same idempotency key.
- Attempt a transfer with insufficient balance.
- Verify that one user cannot access another user's wallet.
- Access admin endpoints using an ADMIN account.
- Send overlapping wallet update requests and verify balance consistency.

## Repository Structure

```text
digital-wallet-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
├── pom.xml
├── .gitignore
└── README.md
```