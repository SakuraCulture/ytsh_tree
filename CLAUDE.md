# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

- This repo is a customized Yudao / RuoYi-Vue-Pro fork.
- Backend: Java 17, Spring Boot 3, Maven multi-module modular monolith.
- Frontend in active use for this checkout: `ytsh-ui-vue3` (Vue 3 + Vite + Element Plus + Pinia + TypeScript).
- `yudao-server` is the runnable backend container. In this checkout it assembles `yudao-module-system`, `yudao-module-infra`, plus custom `yudao-module-business` and `yudao-module-ele`. Many other modules exist in the repo but are commented out in `yudao-server/pom.xml`.

## Common commands

### Backend (run from repo root)

- Full build:
  ```bash
  mvn clean package
  ```
- Run all backend tests:
  ```bash
  mvn test
  ```
- Build only the runnable server and the modules it depends on:
  ```bash
  mvn -pl yudao-server -am package
  ```
- Run the backend locally:
  ```bash
  mvn -pl yudao-server -am spring-boot:run
  ```
- Run tests for one module:
  ```bash
  mvn -pl yudao-module-system test
  ```
- Run a single test class:
  ```bash
  mvn -pl yudao-module-system -Dtest=AdminAuthServiceImplTest test
  ```
- Run a single test method:
  ```bash
  mvn -pl yudao-module-system -Dtest=AdminAuthServiceImplTest#testMethodName test
  ```

Notes:

- There is no Maven wrapper checked in; use the system `mvn` binary.
- Surefire 3.x is configured at the root `pom.xml`, so standard `-Dtest=...` patterns work.

### Frontend (`ytsh-ui-vue3`)

- Install dependencies:
  ```bash
  pnpm install
  ```
- Start frontend against local backend config (`.env.local`):
  ```bash
  pnpm dev
  ```
- Start frontend against the shared dev environment (`.env.dev`):
  ```bash
  pnpm dev-server
  ```
- Type-check:
  ```bash
  pnpm ts:check
  ```
- ESLint autofix:
  ```bash
  pnpm lint:eslint
  ```
- Prettier format:
  ```bash
  pnpm lint:format
  ```
- Stylelint autofix:
  ```bash
  pnpm lint:style
  ```
- Local build:
  ```bash
  pnpm build:local
  ```
- Dev build:
  ```bash
  pnpm build:dev
  ```
- Production build:
  ```bash
  pnpm build:prod
  ```
- Preview a build:
  ```bash
  pnpm preview
  ```

Notes:

- The frontend README explicitly says to use `pnpm`.
- No frontend test script is defined in `ytsh-ui-vue3/package.json`.

### Database helpers

- Database helper scripts are documented in `sql/tools/README.md` and are meant to be run from `sql/tools`.
- Quick MySQL startup for local testing:
  ```bash
  cd sql/tools && docker compose up -d mysql
  ```
- Quick PostgreSQL startup:
  ```bash
  cd sql/tools && docker compose up -d postgres
  ```

## High-level architecture

### Backend structure

- Root `pom.xml` is the Maven aggregator and version anchor.
- `yudao-dependencies` is the BOM / dependency-management module.
- `yudao-framework` contains the shared internal starters (`...-web`, `...-security`, `...-mybatis`, `...-redis`, `...-mq`, `...-job`, `...-test`, etc.).
- `yudao-server` is the Spring Boot boot app and assembly module; it is intentionally mostly a container that pulls feature modules together.
- The boot entrypoint is `yudao-server/src/main/java/cn/iocoder/yudao/server/YudaoServerApplication.java`, which scans both `${yudao.info.base-package}.server` and `${yudao.info.base-package}.module`.

### Backend module layout

The codebase follows a consistent layered module pattern:

- `controller/admin/**` and `controller/app/**` for HTTP APIs
- `service/**` for business logic
- `dal/dataobject/**` and `dal/mysql/**` for persistence
- `api/**` for cross-module APIs
- `mq/**` for message producers/consumers
- `job/**` for scheduled jobs
- `framework/**` for module-specific framework wiring

### Most important modules in this checkout

- `yudao-module-system`: auth, users, RBAC, menus, dictionaries, tenants, OAuth2, mail, SMS, notices, logs.
- `yudao-module-infra`: codegen, config, files, job logging, monitoring, websocket support, API/logging infrastructure.
- `yudao-module-business`: custom business domain for product/category/store management.
- `yudao-module-ele`: custom Ele order integration module. It depends on `yudao-module-business` and packages local SDK jars from `src/main/resources/lib/` via `system` scope.

### Test architecture

- Module tests typically extend shared bases from `yudao-framework/yudao-spring-boot-starter-test`, especially `BaseDbUnitTest` and related classes.
- `BaseDbUnitTest` loads a Spring context with the `unit-test` profile and runs `sql/clean.sql` after each test.
- `application-unit-test.yaml` files use H2 for the database and Redis on `127.0.0.1:16379`.
- The common pattern is: current-module mappers use H2-backed tests, while dependencies on other modules are mocked at the service boundary.

### Frontend structure

- `ytsh-ui-vue3/src/main.ts` boots the app: i18n, Pinia, global components, Element Plus, form-create, router, directives, DOMPurify, and print support.
- Static shell routes live in `ytsh-ui-vue3/src/router/modules/remaining.ts`.
- Dynamic routes are backend-driven:
  1. login via `src/api/login/index.ts`
  2. fetch permission/menu info from `/system/auth/get-permission-info`
  3. cache it in `src/store/modules/user.ts`
  4. turn backend menu data into routes in `src/store/modules/permission.ts`
  5. add those routes during navigation in `src/permission.ts`
- The shared HTTP layer is `ytsh-ui-vue3/src/config/axios/service.ts`.
  - Adds `Authorization` bearer tokens
  - Adds `tenant-id` and `visit-tenant-id`
  - Handles refresh-token replay on 401
  - Sets no-cache headers for GETs
  - Supports optional API encryption/decryption
- Base API URL comes from `ytsh-ui-vue3/src/config/axios/config.ts`: `VITE_BASE_URL + VITE_API_URL`.
  - In local dev, `.env.local` points to `http://localhost:8080/admin-api`.
  - Frontend dev server port is `8081` from `.env`.
- `ytsh-ui-vue3/docs/order-request-analysis.md` is a useful repo-specific walkthrough of the page → API module → axios wrapper → interceptor → backend flow.

## Repo-specific notes

- The backend defaults to the `dev` profile in `yudao-server/src/main/resources/application.yaml`.
- `yudao-server/src/main/resources/application-dev.yaml` in this checkout is not a throwaway localhost sample: it contains environment-specific hosts, credentials, and API keys. Read it before changing runtime config, and avoid casually overwriting it.
- Frontend `.env` / `.env.local` also contain environment-specific defaults such as tenant/login settings and API-encryption configuration.
- This repo contains multiple frontend variants under `yudao-ui/`, but the actively customized frontend in this checkout is `ytsh-ui-vue3`.
- `script/docker/docker-compose.yml` references paths such as `yudao-ui-admin` that do not exist in this checkout; prefer the direct backend/frontend commands above and `sql/tools/README.md` for local database setup.

