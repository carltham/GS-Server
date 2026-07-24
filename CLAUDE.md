# GS-Server: AI Rules & Development Guidelines

This document enforces mandatory AI behavior rules and development standards for all AI-assisted work on this project.

---

## 🚫 CRITICAL RULES (Non-Negotiable)

### 1. **NEVER Commit Without Explicit Request**
- **Rule**: AI MUST NEVER commit changes to git unless the user explicitly says "commit these changes", "make a commit", etc.
- **Why**: Prevents accidental commits of incomplete/unreviewed work.
- **Enforcement**: Permission prompt required before any `git commit` command.

### 2. **NEVER Push Without Explicit Request**
- **Rule**: AI MUST NEVER push to remote unless explicitly authorized.
- **Why**: Prevents unintended deployment and remote persistence.
- **Enforcement**: Permission prompt required before any `git push` command.

### 3. **No Secrets in Output**
- **Rule**: Never expose credentials, tokens, personal data in logs or commits.
- **Why**: Security breach prevention.
- **Enforcement**: Redact before logging/committing.

---

## 📋 Project Overview

**GS-Server:** Java Spring Boot backend service with Angular frontend for server hardening, firewall proxy control, and application management.

- **Architecture:** 4-tier current (Controllers → Services → Adapters → Repositories) → 6-tier target (+ REST API + UI layers)
- **Tech Stack:** Java 17, Spring Boot 3.3.2, Angular, Maven, PostgreSQL
- **Status:** Phase 0 (28% complete, 95% hardening, 50% proxy)
- **Timeline:** 28-week migration roadmap (6-8 week critical path to Phase 2)
- **Deployment:** Docker, Kubernetes, or standalone JAR

---

## 📁 Repository Structure

```
GS-Server/
├── CLAUDE.md                        (This file - AI rules & guidance)
├── GSServer-pom/                    (Maven parent)
│   ├── GSServer-db/                 (Database & repository layer)
│   ├── GSServer-jar/                (Backend service, REST API)
│   └── GSServer-UI/                 (Angular frontend)
├── architecture/                    (Design documentation)
│   ├── present/                     (Current-state docs - 7 files)
│   ├── target/                      (Target-state docs - 7 files)
│   └── rules/                       (Enforcement rules - 6 files)
├── planning/                        (Project planning)
│   ├── migrations/                  (Gherkin migration plans - 9 files)
│   └── architecture/                (Architecture design docs)
├── .claude/
│   ├── CLAUDE.md                    (This file)
│   ├── settings.json                (Enforcement rules & hooks)
│   └── settings.local.json          (Personal overrides - gitignored)
└── .gitignore                       (Protects local settings)
```

---

## 🏗️ Architecture Overview

### Current State (Phase 0)
- **4-tier pattern:** Controllers → Services → Adapters → Repositories
- **Capabilities:** Hardening (95%), Proxy (50%), Users, Security, Terminal
- **Issues:** Secrets leak in errors, no audit logging, unstructured errors, UI directly calls services (can't scale)
- **Components:** 78 total (28% scaffolded, 28% real, 44% infrastructure)

### Target State (Production-Ready)
- **6-tier pattern:** Handler (REST) → Service → Adapter → Repository → REST API → UI
- **Features:** Secrets redaction, audit logging, structured errors, correlation IDs, cross-tenant testing
- **Timeline:** 28 weeks (Phase 1: 2-3 weeks, Phase 2: 3-4 weeks, Phase 3-6: 18+ weeks)

### Key Development Rules

**From organizational standards** (`/mnt/DATA/WORKSPACE/project-settings/common/`):
1. **4-tier minimum architecture** (current) → **6-tier target** (REST + independent scaling)
2. **Dependency injection** over direct instantiation
3. **Business logic ONLY in Service layer** (not Controllers, Adapters, or Repositories)
4. **Database logic ONLY in Repository layer**
5. **Error model:** Structured with errorId, code, message, timestamp, correlationId, severity, source, details
6. **Security:** Secrets redaction in errors/logs, audit logging for all writes, multi-tenant isolation tested
7. **Testing:** Deterministic, cross-tenant denial tests (MANDATORY), 85%+ coverage

**Local Extensions** (see `/architecture/rules/` for details):
- Java naming conventions (PascalCase classes, camelCase methods)
- Spring Boot patterns (Controllers, Services, Repositories)
- Maven build patterns
- Angular service patterns

---

## 🔨 Common Commands

```bash
# Build
mvn clean install
mvn clean package

# Test
mvn test
mvn test -Dtest=DefaultHardeningServiceTest
mvn jacoco:report

# Run backend (after mvn package)
java -jar GSServer-pom/GSServer-jar/target/GSServer-jar-*.jar

# Run frontend dev server
cd GSServer-pom/GSServer-UI && npm install && npm start
```

---

## 📋 Before Starting Any Task

1. **Read CLAUDE.md** — Understand critical rules above
2. **Read architecture documentation:**
   - `/architecture/present/README.md` — Current state baseline
   - `/architecture/target/TARGET_ARCHITECTURE_PRINCIPLES.md` — Target vision
3. **Review applicable standards:**
   - `/architecture/rules/ARCHITECTURE_RULES.md` — Core principles
   - `/architecture/rules/SECURITY_RULES.md` — Security requirements
   - `/architecture/rules/TESTING_RULES.md` — Testing requirements
4. **Check migration plans** if implementing Phase 1-2 work

---

## ✅ After Changes

- **Report what changed:** Files modified, checks performed
- **DO NOT commit or push** unless user explicitly requests it
- **Verify no secrets** in output/logs
- **Reference CLAUDE.md** rules if applicable

---

## 🔗 Key Resources

**Architecture Documentation**
- `/architecture/present/ARCHITECTURE_OVERVIEW.md` — Capabilities status, current gaps
- `/architecture/present/COMPONENT_MAP.md` — 78-component inventory
- `/architecture/target/TARGET_ARCHITECTURE_PRINCIPLES.md` — 8 core principles
- `/architecture/target/TARGET_SECURITY_POLICY.md` — Security requirements
- `/architecture/rules/` — Enforcement rules (6 files)

**Migration Plans (Gherkin Format)**
- `/planning/migrations/PHASE_1_01_SECRETS_REDACTION.feature`
- `/planning/migrations/PHASE_1_02_AUDIT_LOGGING.feature`
- `/planning/migrations/PHASE_1_03_STRUCTURED_ERRORS.feature`
- `/planning/migrations/PHASE_2_01_JAR_BACKEND_WIRING.feature`
- `/planning/migrations/PHASE_2_02_CORRELATION_IDS.feature`
- `/planning/migrations/PHASE_2_03_CROSS_TENANT_TESTS.feature`
- `/planning/migrations/README.md` — Overview & timeline

**Organizational Standards**
- `/mnt/DATA/WORKSPACE/project-settings/common/originals/` — Read-only baseline standards
- `/mnt/DATA/WORKSPACE/project-settings/CLAUDE.md` — Workspace governance

---

## 🛠️ Critical Blockers (Must Address Phase 1-2)

**P1 - Production Blockers:**
- Secrets leak in error messages (DB URLs, SSH keys, file paths)
- No audit logging for compliance/debugging
- Unstructured error responses (no errorId)
- UI directly injects services (can't scale independently)

**P2 - Security Issues:**
- Cross-tenant denial not tested (assume broken)
- No correlation ID for tracing
- No end-to-end error context

See `/architecture/target/ROADMAP_TO_TARGET.md` for prioritization & timeline.

---

## ✅ Enforcement Status

| Rule | Mechanism | Status |
|------|-----------|--------|
| No unasked commits | Permission prompt + hook | ✅ Active |
| No unasked pushes | Permission prompt + hook | ✅ Active |
| No secrets in output | Code review | ✅ Manual |
| Architecture compliance | Documentation + rules | ✅ Documented |
| Testing requirements | Standards + enforcement | ✅ Documented |

---

**Last Updated:** 2026-07-24  
**Maintained by:** Development & Architecture Team  
**Status:** Active & Enforced
