# LibraTrack — University Library Loan, Reservation & Fine Management System

**Course:** SECT-4221 Enterprise Application Development · Capstone · Spring 2025/2026

## Group Members

| # | Full Name | Student Number | Email | GitHub |
|---|-----------|---------------|-------|--------|
| 1 | Yeabsira Samuel *(Leader)* | ATE/9305/14 | yeabsamuelz25@gmail.com | yeab-samuel |
| 2 | Kassahun Belachew | ATE/8400/14 | Etelethiopia@gmail.com | Kase2228 |
| 3 | Natnael Nigatu | ATE/7495/14 | natnaelnigatu23@gmail.com | natiworks |
| 4 | Tsegaab Alemu | ATE/8814/14 | tsegaabalemu147@gmail.com | Tsegaab-ux |

## Deployed URL

https://

## Tech Stack

- Java 21, Spring Boot 3.3.5, Spring Security 6
- JWT (jjwt 0.12.6 / HS256), BCryptPasswordEncoder(12)
- PostgreSQL 15, Flyway V1–V5
- Docker Compose, JaCoCo ≥ 70% line coverage

## Roles

`ADMIN` · `LIBRARIAN` · `STUDENT` · `FACULTY`

## How to Build

```bash
mvn clean package -DskipTests
```

## How to Run (Docker)

```bash
cp .env.example .env
# Edit .env — set JWT_SECRET (>= 32 chars) and POSTGRES_PASSWORD
docker-compose up --build
```

- API base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html` *(dev profile only — disabled in prod)*
- Health check: `http://localhost:8080/actuator/health`

## How to Run Tests

```bash
mvn test
```

## How to Generate JaCoCo Coverage Report

```bash
mvn test jacoco:report
# Open: target/site/jacoco/index.html
```

## Key API Endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | /api/auth/register | PUBLIC | Register account |
| POST | /api/auth/login | PUBLIC | Login, receive JWT |
| POST | /api/auth/logout | ALL | Blacklist token (logout) |
| GET | /api/books/search | PUBLIC | Search catalogue with filters |
| POST | /api/books | ADMIN, LIBRARIAN | Add book to catalogue |
| GET | /api/books/{bookId}/copies | ADMIN, LIBRARIAN | List physical copies |
| POST | /api/loans | LIBRARIAN | Issue loan |
| PATCH | /api/loans/{id}/return | LIBRARIAN | Process return |
| GET | /api/loans/mine | STUDENT, FACULTY | Own loan history |
| GET | /api/loans/{id} | ALL | Single loan (BOLA enforced) |
| POST | /api/reservations | STUDENT, FACULTY | Reserve unavailable book |
| GET | /api/fines/mine | STUDENT, FACULTY | Own fine history |
| GET | /api/fines/{id} | ALL | Single fine (BOLA enforced) |
| PATCH | /api/fines/{id}/pay | LIBRARIAN | Mark fine as paid |
| PATCH | /api/fines/{id}/waive | ADMIN | Waive fine with reason |
| GET | /api/reports/overdue | ADMIN, LIBRARIAN | All overdue loans |
| GET | /api/reports/fines-summary | ADMIN | Fine totals in date range |
| GET | /api/admin/users | ADMIN | All users with filters |
| PATCH | /api/admin/users/{id}/deactivate | ADMIN | Deactivate user account |

## AI Tool Disclosure

GitHub Copilot and Claude (Anthropic) were used to assist with boilerplate generation and debugging. All code has been reviewed, understood, and can be explained by every group member.
