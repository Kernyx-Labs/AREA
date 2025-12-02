# AREA Server

This module contains the Spring Boot backend for AREA.

The backend always runs from the IDE (IntelliJ) and connects to a PostgreSQL database running in Docker. The database is started manually with Docker Compose; the backend never starts or embeds a database.

> Only backend code (endpoints, services, rules, configuration) is committed. **Database data itself (under `db-data/`) is local-only and ignored by Git.**

---

## 1. Environment file

All sensitive values (including the database password) are defined in `.env` and **must not** be hard‑coded in source code or documentation.

1. In the `server` folder, copy the example file:
   ```bash
   cd /home/guilby/Delivery/web/AREA/server
   cp .env.example .env
   ```
2. Open `.env` and choose your own secure values for:
   - `POSTGRES_DB`
   - `POSTGRES_USER`
   - `POSTGRES_PASSWORD`
   - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (used when running the backend in Docker, if needed)
   - `APP_JWT_SECRET`

> The actual password and secrets live only in `.env` on your machine. Do **not** commit this file.

---

## 2. PostgreSQL with Docker (on host port 8088)

Postgres runs in a Docker container defined at the repository root in `docker-compose.yml` under the `postgres` service.

### 2.1. Start only the database

From the repository root:

```bash
cd /home/guilby/Delivery/web/AREA
# start only the postgres service in the background
docker compose up -d postgres
```

This will:
- Start a `postgres` container using the credentials from `server/.env`.
- Expose Postgres on the host at `localhost:8088` (container port 5432).
- Persist data under `server/db-data/`.
- Optionally run any init scripts found in `server/db-init/` (see below).

### 2.2. Check that Postgres is healthy (optional)

```bash
cd /home/guilby/Delivery/web/AREA
docker compose logs -f postgres
```

You should see Postgres start without errors. Once it reports that it is ready for connections, you can start the backend from the IDE.

### 2.3. Initialization and data directories

- `db-data/` – persistent Postgres data (mounted into the container). Do not commit real data.
- `db-init/` – optional `.sql` or `.sh` files that run on first container boot (when the data directory is empty). Use this to create schemas, tables, or seed data.

---

## 3. Running the backend from IntelliJ

The **only supported way** to run the backend is from IntelliJ IDEA using the `ServerApplication` main class. We do **not** run the backend via Docker Compose during normal development.

### 3.1. Pre‑requisite: database running in Docker

Before starting the backend in IntelliJ, ensure Postgres is running:

```bash
cd /home/guilby/Delivery/web/AREA
docker compose up -d postgres
```

If Postgres is not running or not reachable, the backend will fail to start with a database connection error.

### 3.2. IntelliJ run configuration

1. Open IntelliJ in the project root.
2. Create a new **Spring Boot** run configuration:
   - Main class: `com.area.server.ServerApplication`
   - Module: `server`
3. Set the active Spring profile to `dev`:
   - In the run configuration, add `dev` to **Active profiles**, or
   - Add a VM option: `-Dspring.profiles.active=dev`

With the `dev` profile active, Spring Boot will use `application-dev.properties`.

### 3.3. Dev profile database configuration

The file `server/src/main/resources/application-dev.properties` is configured to connect to the Postgres container over `localhost:8088`.

It defines, among other properties:

```properties
spring.datasource.url=jdbc:postgresql://localhost:8088/area
spring.datasource.username=<your DB user from .env>
spring.datasource.password=<your DB password from .env>
```

Adjust these values to match what you put in `.env` (database name, user, password). The password itself is **not** documented here; it is only stored in your local `.env`.

Once this matches your `.env` configuration and the container is running, starting the `ServerApplication` run configuration in IntelliJ will bring up the backend and connect to the Dockerized Postgres instance.

---

## 4. Summary of the workflow

1. Configure secrets and credentials in `server/.env` (created from `.env.example`).
2. Start Postgres using Docker Compose:
   ```bash
   cd /home/guilby/Delivery/web/AREA
   docker compose up -d postgres
   ```
3. In IntelliJ, run `ServerApplication` from the `server` module with the `dev` profile active.
4. The backend connects to the Postgres instance listening on `localhost:8088` using the credentials from your configuration.

No other way of launching the backend (e.g. `docker compose up server`) is used in the normal development workflow.
