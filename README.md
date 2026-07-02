# LibraTrack

![CI](https://github.com/yeab-samuel/libratrack/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)

**University Library Loan, Reservation & Fine Management System**

LibraTrack is a full-stack library management platform built for a university setting with four distinct user roles — students, faculty, librarians, and administrators. It manages the complete lifecycle of a library transaction: catalogue search, self-service and counter-issued loans, a fairness-aware reservation queue, automatic fine calculation, book ratings, and administrative reporting — all secured behind JWT-based authentication with role-based access control.

The system was built as the capstone project for **SECT-4221 — Enterprise Application Development**, Spring 2025/2026.

> **Live demo:** [libratrack.up.railway.app](https://libratrack.up.railway.app) (primary) · [libratrack-ui.onrender.com](https://libratrack-ui.onrender.com) (backup, may take 30–60s to wake up)
> **Recorded walkthrough:** [go.screenpal.com/watch/cO1vjUnUini](https://go.screenpal.com/watch/cO1vjUnUini)

---

## Table of Contents

- [Group Members](#group-members)
- [Overview](#overview)
- [Key Features](#key-features)
- [Roles & Permissions](#roles--permissions)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Running the Backend](#running-the-backend)
- [Running the Frontend](#running-the-frontend)
- [API Reference](#api-reference)
- [Testing & Coverage](#testing--coverage)
- [Deployment](#deployment)
- [AI Tool Disclosure](#ai-tool-disclosure)

---

## Group Members

| # | Full Name | Student Number | Email | GitHub |
|---|---|---|---|---|
| 1 | Yeabsira Samuel | ATE/9305/14 | yeabsamuelz25@gmail.com | [yeab-samuel](https://github.com/yeab-samuel) |
| 2 | Kassahun Belachew | ATE/8400/14 | Etelethiopia@gmail.com | [Kase2228](https://github.com/Kase2228) |
| 3 | Natnael Nigatu | ATE/7495/14 | natnaelnigatu23@gmail.com | [natiworks](https://github.com/natiworks) |

---

## Overview

University libraries need to track which physical copies of which books are currently on loan, who is waiting for a copy that isn't available, and who owes fines for overdue returns — all while enforcing different borrowing rules for different kinds of members (a faculty member, for instance, can borrow for longer and gets priority in the reservation queue over a student).

LibraTrack models this directly. Every account — student, faculty, librarian, or admin — logs in with a university-issued ID rather than an email address, mirroring how real institutional systems authenticate their members. Student and faculty accounts are self-registered but cross-checked against a campus registry table before being accepted, so only people the university has actually issued an ID to can create an account. Librarian and admin accounts, by contrast, can only be created by an existing administrator, who assigns them a staff ID at creation time.

From there, the system supports the full operational loop of a circulation desk: searching and filtering the catalogue, borrowing a copy (either by a member directly or issued by a librarian at the counter), returning a copy (with automatic fine calculation if it's late), reserving a book that's fully checked out and being notified when a copy frees up, paying or waiving a fine, and rating a book after it's been returned. Administrators and librarians get dedicated reporting views — overdue loans, fine totals over a date range — and admins can manage user accounts and the campus registry directly.

## Key Features

**Authentication & Access Control**
JWT-based login (HS256), with logout implemented via a server-side token blacklist so a token can be invalidated before its natural expiry. Every account logs in with a university or staff ID rather than email; email is retained for registration and notification purposes only. Self-registration for students and faculty is validated against a campus registry table, so identity fraud at signup is structurally prevented rather than merely discouraged.

**Catalogue Management**
Full-text search across title and author, with filters for category, publication year, and copy availability. Librarians and admins can add new titles and manage individual physical copies (each with its own condition and status).

**Loans**
Students and faculty can borrow available copies themselves; librarians can also issue loans on a member's behalf at the counter. Borrow limits differ by role (faculty get a higher cap and a longer default loan period), and the system enforces that a member with unpaid fines cannot borrow further. Faculty members can extend the due date on their own active loans.

**Reservations**
When a book has no available copies, members can join a waitlist. The system tracks each member's position in the queue and gives faculty priority over students when notifying the next person in line. A notified member has a fixed collection window before their reservation expires and the slot passes to the next person.

**Fines**
Fines are calculated automatically on a tiered daily rate when an overdue book is returned: a 2-day grace period applies first, then $0.50/day for the first 14 days overdue, doubling to $1.00/day beyond that. Librarians can mark a fine as paid; only admins can waive a fine, and a waiver requires a documented reason.

**Ratings**
Members can rate a book out of five stars once they've returned it, and the catalogue displays the running average and total rating count per title.

**Reporting & Administration**
Librarians and admins can pull a list of all currently overdue loans (with the borrower's university ID for quick lookup) and generate a fines-collected summary over an arbitrary date range. Admins can list, filter, activate, and deactivate user accounts, and manage entries in the campus registry that self-registration checks against.

**Email Notifications**
Automated transactional emails are sent via Resend HTTP API (not SMTP): members are notified when their reserved book becomes available, when a reservation expires, and when an overdue fine is applied to their account.

## Roles & Permissions

| Role | Identifier format | Account creation | Loan duration | Reservation priority |
|---|---|---|---|---|
| Student | `UGR/XXXX/YY` | Self-registers (registry-checked) | Up to 14 days | Standard |
| Extension Student | `ATE/XXXX/YY` | Self-registers (registry-checked) | Up to 14 days | Standard |
| Faculty | `FAC/XXXX/YY` | Self-registers (registry-checked) | Up to 30 days, extendable | Priority |
| Librarian | `LIB/XXXX/YY` | Created by an Admin | — (staff) | — |
| Admin | `ADM/XXX/YY` | Created by an Admin | — (staff) | — |

Librarians can issue and process loans, manage reservations, mark fines as paid, and view reports. Only admins can waive fines, manage user accounts, and edit the campus registry.

## Tech Stack

**Backend**
Java 21 · Spring Boot 3.3.5 · Spring Security 6 · Spring Data JPA · PostgreSQL 15 · Flyway (versioned migrations V1–V16) · JWT via jjwt 0.12.6 (HS256) · BCryptPasswordEncoder (strength 12) · Lombok · JaCoCo for coverage · JUnit 5 + Mockito + Testcontainers for integration testing

**Frontend**
React 19 · Vite

**Infrastructure**
Docker & Docker Compose · Railway (primary deployment) · Render.com (backup deployment)

## Architecture

The backend follows a conventional layered architecture: REST controllers handle HTTP concerns and delegate to services, services hold business logic and transaction boundaries, and Spring Data JPA repositories handle persistence. Authentication is enforced via a JWT filter ahead of Spring Security's method-level `@PreAuthorize` annotations, so role checks live next to the endpoints they protect rather than in a separate, easy-to-forget configuration file. Database schema changes are version-controlled through Flyway migrations (V1–V16) rather than applied ad hoc, so the schema's history is auditable and reproducible across environments.

The frontend is a single-page React application that talks to the backend exclusively over its REST API, with the JWT stored client-side and attached to every authenticated request.

## Project Structure

```
libratrack/
├── src/main/java/com/libratrack/
│   ├── controller/        # REST endpoints
│   ├── service/           # Business logic
│   ├── repository/        # Spring Data JPA repositories
│   ├── specification/     # Dynamic query specifications (search/filtering)
│   ├── dto/
│   │   ├── request/       # Inbound request bodies
│   │   └── response/      # Outbound response DTOs
│   ├── entity/             # JPA entities
│   ├── enums/              # Role, status, and category enums
│   ├── security/           # JWT filter, UserDetailsService, JwtUtils
│   ├── exception/          # Custom exceptions + global handler
│   └── scheduler/          # Scheduled jobs (overdue fines, reservation expiry)
├── src/main/resources/
│   └── db/migration/      # Flyway migrations (V1–V16)
├── src/test/java/com/libratrack/
│   ├── controller/        # @WebMvcTest slice tests
│   ├── service/           # Mockito unit tests
│   ├── repository/        # @DataJpaTest tests (H2)
│   └── security/          # Full-stack Testcontainers integration tests
└── libratrack-ui/
    └── src/               # React frontend (Vite)
```

## Getting Started

### Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Docker & Docker Compose (for running PostgreSQL and the full integration test suite)
- Node.js 18+ and npm (for the frontend)

### Clone the repository

```bash
git clone https://github.com/yeab-samuel/libratrack.git
cd libratrack
```

## Environment Variables

The application reads all secrets from environment variables. For local development with Docker Compose, set at minimum:

| Variable | Description |
|---|---|
| `JWT_SECRET` | At least 32 characters, used to sign and verify JWTs |
| `DB_PASS` | PostgreSQL password |
| `DB_URL` | JDBC connection URL |
| `DB_USER` | PostgreSQL username |
| `RESEND_API_KEY` | API key for Resend email service (optional — set `MAIL_ENABLED=false` to disable) |
| `FINE_DAILY_RATE` | Fine per day (default: `0.50`) |
| `FINE_GRACE_DAYS` | Days before fines start accruing (default: `2`) |
| `FINE_ESCALATION_DAYS` | Days before rate doubles (default: `14`) |
| `MAX_LOANS_STUDENT` | Max active loans for students (default: `3`) |
| `MAX_LOANS_FACULTY` | Max active loans for faculty (default: `5`) |
| `CRON_SECRET` | Static secret for the external scheduler endpoint |

## Running the Backend

### Build

```bash
mvn clean package -DskipTests
```

### Run with Docker Compose (recommended for local development)

```bash
docker-compose up --build
```

This starts both the PostgreSQL database and the Spring Boot application together, running Flyway migrations V1–V16 automatically on startup.

- API base URL: `http://localhost:9090` (default port from `server.port=${PORT:9090}`)
- Swagger UI: `http://localhost:9090/swagger-ui.html` (enabled in all profiles)
- Health check: `http://localhost:9090/actuator/health`

> **Local docker-compose note:** The docker-compose.yml maps host port 8080 to container port 8080, but the app listens on `${PORT:9090}` by default. To use port 8080 locally, add `PORT: 8080` to the app service environment block in docker-compose.yml, or access the app on port 9090.

## Running the Frontend

```bash
cd libratrack-ui
npm install
npm run dev
```

Set the backend URL by creating `libratrack-ui/.env.local`:

```
VITE_API_URL=http://localhost:9090
```

## API Reference

A representative subset of the 45 available endpoints (full documentation at `/swagger-ui.html`):

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Self-register a student or faculty account (registry-checked) |
| POST | `/api/auth/login` | Public | Log in with university/staff ID, receive a JWT |
| POST | `/api/auth/logout` | Any authenticated | Blacklist the current token |
| POST | `/api/admin/staff` | Admin | Create a librarian or admin account with a staff ID |
| GET | `/api/books/search` | Public | Search the catalogue with filters |
| POST | `/api/books` | Admin, Librarian | Add a new title to the catalogue |
| GET | `/api/books/{bookId}/copies` | Admin, Librarian | List physical copies of a title |
| GET | `/api/books/{bookId}/copies/available` | Authenticated | Available copies only (self-service borrow modal) |
| POST | `/api/books/{bookId}/ratings` | Student, Faculty | Rate a book (1–5 stars, post-return only) |
| GET | `/api/books/{bookId}/ratings/reviews` | Public | List written reviews for a book |
| POST | `/api/loans/borrow` | Student, Faculty | Self-service borrow |
| POST | `/api/loans` | Librarian, Admin | Issue a loan at the counter |
| PATCH | `/api/loans/{id}/return` | Librarian, Admin | Process a return; triggers reservation queue |
| PATCH | `/api/loans/{id}/extend` | Faculty | Extend the due date on an owned active loan |
| GET | `/api/loans/mine` | Student, Faculty | Own loan history, filterable by status |
| GET | `/api/loans/{id}` | Any authenticated | Single loan (object-level access control) |
| POST | `/api/reservations` | Student, Faculty | Reserve an unavailable book |
| GET | `/api/fines/mine` | Student, Faculty | Own fine history |
| PATCH | `/api/fines/{id}/pay` | Librarian, Admin | Mark a fine as paid |
| PATCH | `/api/fines/{id}/waive` | Admin | Waive a fine with a documented reason |
| GET | `/api/reports/overdue` | Admin, Librarian | All currently overdue loans |
| GET | `/api/reports/fines-summary` | Admin | Fine totals collected within a date range |
| GET | `/api/admin/users` | Admin | List all users, with role/status filters |
| PATCH | `/api/admin/users/{id}/deactivate` | Admin | Deactivate a user account |
| POST | `/api/admin/registry` | Admin | Add an entry to the campus registry |
| GET | `/api/admin/registry` | Admin, Librarian | List campus registry entries |
| POST | `/api/cron/overdue-fines` | Static secret | External scheduler endpoint (cron-job.org) |

## Testing & Coverage

Run the full test suite:

```bash
mvn test
```

The suite includes controller-layer slice tests (`@WebMvcTest`), service-layer unit tests (Mockito), repository-layer tests against an in-memory H2 database (`@DataJpaTest`), and full-stack integration tests that spin up a real PostgreSQL container via Testcontainers — the latter are automatically skipped on machines without Docker available, and run normally in any CI environment that provides it.

Current results: **220 tests, 0 failures, 0 errors, 7 skipped** (Testcontainers). **75% line coverage** — above the 70% project minimum.

Generate a JaCoCo coverage report:

```bash
mvn test
```

Then open `target/site/jacoco/index.html` in a browser. The JaCoCo plugin runs automatically with `mvn test`.

## Deployment

> If the primary link below is unresponsive (e.g. during a Railway infrastructure incident), please use the Render backup link or the recorded walkthrough — both linked at the top of this README and below.

### Primary deployment — Railway (no cold-start)

| Component | URL |
|---|---|
| **Frontend (React/Vite)** | https://libratrack.up.railway.app |
| **Backend API (Spring Boot)** | https://libratrack-production-4275.up.railway.app |
| **Swagger UI** | https://libratrack-production-4275.up.railway.app/swagger-ui.html |
| **Health check** | https://libratrack-production-4275.up.railway.app/actuator/health |

Railway keeps the container running continuously — no cold-start delays.

### Backup deployment — Render (free tier)

| Component | URL |
|---|---|
| **Frontend (React/Vite)** | https://libratrack-ui.onrender.com |
| **Backend API (Spring Boot)** | https://libratrack-1ua8.onrender.com |
| **Health check** | https://libratrack-1ua8.onrender.com/actuator/health |

> **Note:** Render's free tier spins down after 15 minutes of inactivity. The first request after a cold start may take 30–60 seconds — this is expected behaviour, not a bug.

Both deployments share the same PostgreSQL database (hosted on Render). The nightly overdue-fine scheduler is triggered daily via cron-job.org as a workaround for Render's sleep behaviour.

### Recorded walkthrough

A full recorded demo of the system in action is available here, in case a live grading session coincides with a deployment issue on either platform:

**https://go.screenpal.com/watch/cO1vjUnUini**

### Demo credentials

| Role | University ID | Password |
|---|---|---|
| Admin | `ADM/001/00` | `Admin1234!` |
| Demo Student | `DEMO/0001/26` | `Demo1234!` |

## AI Tool Disclosure

In accordance with the academic integrity policy for SECT-4221, the following AI tools were used during this project:

| Tool | How it was used |
|---|---|
| **Claude (Anthropic)** | Debugging assistance, boilerplate generation for DTOs and test scaffolding, CSS/frontend styling suggestions, deployment troubleshooting (CORS, Render/Railway configuration), and Part C report generation |
| **GitHub Copilot** | Inline code completion suggestions within IntelliJ IDEA during development |

All AI-generated or AI-suggested code was reviewed, understood, tested, and modified by group members before being committed. Every group member is able to explain any section of the codebase. No AI tool was used to generate test cases that the team did not understand, nor to write the PDF report narrative.