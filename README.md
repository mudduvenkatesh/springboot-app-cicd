# Spring Boot 4 — CI/CD with Docker

A production-ready REST API with a fully automated CI/CD pipeline: **push to `dev`** → build → test → Docker image → staging deploy → integration tests → auto PR → **merge to `main`** → production deploy.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Developer pushes to dev                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                    GitHub Actions: ci-dev.yml
                           │
         ┌─────────────────▼──────────────────┐
         │  1. Unit Tests (JUnit 5 + Mockito)  │
         └─────────────────┬──────────────────┘
                           │
         ┌─────────────────▼──────────────────┐
         │  2. Build Docker Image             │
         │     Push to GHCR (dev-<sha>)       │
         └─────────────────┬──────────────────┘
                           │
         ┌─────────────────▼──────────────────┐
         │  3. Integration Tests              │
         │     (Testcontainers + PostgreSQL)  │
         └─────────────────┬──────────────────┘
                           │
         ┌─────────────────▼──────────────────┐
         │  4. Deploy to Staging (SSH)        │
         │     docker compose up -d           │
         └─────────────────┬──────────────────┘
                           │
         ┌─────────────────▼──────────────────┐
         │  5. Auto-create PR: dev → main     │
         └─────────────────┬──────────────────┘
                           │
                    Human reviews & merges PR
                           │
                    GitHub Actions: cd-prod.yml
                           │
         ┌─────────────────▼──────────────────┐
         │  Re-tag image: prod-<sha>, latest  │
         │  Deploy to Production (SSH)        │
         │  Health check → GitHub Release     │
         └────────────────────────────────────┘
```

---

## Project Structure

```
springboot-app/
├── .github/
│   └── workflows/
│       ├── ci-dev.yml          # Push to dev: test → build → stage → PR
│       ├── cd-prod.yml         # PR merged to main: production deploy
│       └── pr-validation.yml  # All PRs: build + test gate
├── src/
│   ├── main/java/com/example/app/
│   │   ├── Application.java
│   │   ├── controller/         # REST endpoints
│   │   ├── service/            # Business logic
│   │   ├── model/              # JPA entities
│   │   └── repository/         # Spring Data repos
│   ├── main/resources/
│   │   ├── application.properties        # Dev (H2)
│   │   └── application-prod.properties  # Prod (PostgreSQL)
│   └── test/java/com/example/app/
│       ├── unit/               # Fast unit tests (Mockito)
│       └── integration/        # Integration tests (Testcontainers)
├── Dockerfile                  # Multi-stage layered build
├── docker-compose.yml          # Local dev (builds image locally)
├── docker-compose.prod.yml     # Production (pulls from registry)
├── .env.example                # Template — copy to .env
└── pom.xml
```

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Docker | 24+ |
| Docker Compose | v2+ |
| GitHub account | — |

---

## Local Development

```bash
# Clone and run locally (H2 in-memory)
./mvnw spring-boot:run

# Or with Docker + PostgreSQL
cp .env.example .env          # edit as needed
docker compose up -d --build

# Run unit tests
./mvnw test

# Run integration tests (requires Docker for Testcontainers)
./mvnw verify
```

API: `http://localhost:8080/api/products`
H2 Console: `http://localhost:8080/h2-console`
Actuator Health: `http://localhost:8080/actuator/health`

---

## GitHub Secrets Required

Go to **Settings → Secrets and variables → Actions** and add:

### Staging Secrets
| Secret | Description |
|--------|-------------|
| `STAGING_HOST` | IP or hostname of staging server |
| `STAGING_USER` | SSH user on staging server |
| `STAGING_SSH_KEY` | Private SSH key for staging |
| `STAGING_DB_URL` | `jdbc:postgresql://db:5432/appdb` |
| `STAGING_DB_USERNAME` | DB username |
| `STAGING_DB_PASSWORD` | DB password |

### Production Secrets
| Secret | Description |
|--------|-------------|
| `PROD_HOST` | IP or hostname of prod server |
| `PROD_USER` | SSH user on prod server |
| `PROD_SSH_KEY` | Private SSH key for prod |
| `PROD_DB_URL` | Production DB JDBC URL |
| `PROD_DB_USERNAME` | Production DB username |
| `PROD_DB_PASSWORD` | Production DB password |

> `GITHUB_TOKEN` is provided automatically — no action needed.

---

## Server Setup (Staging & Production)

Run once on each server:

```bash
# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# Create app directory and prod data volume
sudo mkdir -p /opt/springboot-app
sudo chown $USER /opt/springboot-app

# Copy docker-compose.prod.yml to the server
scp docker-compose.prod.yml user@your-server:/opt/springboot-app/

# Create persistent DB volume (prod only)
docker volume create postgres-prod-data
```

---

## GitHub Environments (Optional — Manual Approval Gate)

To require a human to approve production deploys:

1. Go to **Settings → Environments → New environment**
2. Name it `production`
3. Enable **Required reviewers** and add yourself
4. The `cd-prod.yml` workflow will pause for approval before deploying

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/products` | List all (supports `?search=`) |
| `GET` | `/api/products/{id}` | Get by ID |
| `POST` | `/api/products` | Create product |
| `PUT` | `/api/products/{id}` | Update product |
| `DELETE` | `/api/products/{id}` | Delete product |
| `GET` | `/api/health` | App health |
| `GET` | `/actuator/health` | Spring Actuator health |

### Example Request

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","description":"16-inch","price":1299.99}'
```

---

## Image Tags

| Tag | When created | Use |
|-----|-------------|-----|
| `dev-<sha>` | Every push to `dev` | Staging |
| `prod-<sha>` | Every merge to `main` | Production |
| `latest` | Every merge to `main` | Production (floating) |
