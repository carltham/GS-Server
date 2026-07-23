# Playwright UI Test Infrastructure

## Purpose
Top-down Angular UI testing infrastructure for GS-Server using Playwright.

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
