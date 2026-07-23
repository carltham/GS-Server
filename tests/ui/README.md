# Playwright UI Test Infrastructure

## Purpose
Top-down Angular UI testing infrastructure for GS-Server using Playwright.

## UI Delivery Model
- Angular source module: `GSServer-pom/GSServer-UI/frontend`
- Browser delivery module: `GSServer-pom/GSServer-UI`
- Static assets served via Spring Boot static resources in the UI module
- Playwright target: Spring Boot-served UI (default `http://localhost:8080`)

Current status:
- The module skeleton now exists in this repository.
- Until Spring Boot is running on `BASE_URL`, smoke and phase UI tests will fail with connection refused.

## Prerequisites
- Node.js 20+
- npm 10+

## Setup
1. cd tests/ui
2. npm install
3. npm run install:browsers
4. cp .env.example .env (optional)

## Running Tests
- npm test
- npm run test:headed
- npm run test:ui
- npm run test:phase1

## Troubleshooting

### npm not found
Install prerequisites:
1. `sudo apt update`
2. `sudo apt install -y nodejs npm`

### Browser dependencies missing (Linux)
Install Playwright host dependencies:
1. `npx playwright install-deps`

### Connection refused to BASE_URL
If smoke tests fail with connection refused, start Spring Boot UI module first:
1. Start app on configured `BASE_URL` (default `http://localhost:8080`)
2. Re-run `npm test`

If the UI module is not runnable yet:
1. Implement Angular app in `GSServer-pom/GSServer-UI/frontend`
2. Build Angular into Spring Boot static resources
3. Start Spring Boot on `BASE_URL`

## Conventions
- Use Gherkin phase tags in test titles, e.g. `@phase1`.
- Keep clients thin: UI tests validate behavior and API interaction, not backend business logic.
- Follow top-down sequence: UI boundary first, then API/controller/service/adapter verification.

## Folder Layout
- tests/phase1: Phase 1 scenarios and top-down UI checks
- tests/smoke: Basic app and navigation checks
- tests/fixtures: Shared helpers and test data

## CI Notes
- HTML report is generated automatically.
- Traces/screenshots/videos are retained on failures.
