# Implementation Rules: Phase Management

**Extracted from:** IMPLEMENTATION_RULES.md  
**Last Updated:** 2026-07-24

---

## Rule 1: Phase Progression (Blocking Order)

**Rule:** Phases have dependencies; don't skip ahead.

**Dependency chain:**
```
Phase 1 (Foundation)
  ├─ Hardening wired
  ├─ Secrets redaction
  └─ Audit logging
      ↓
Phase 2 (Gateway Proxy)
  ├─ Depends on Phase 1
  ├─ Jar wiring (REST API)
  ├─ Structured errors
  └─ Correlation IDs
      ↓
Phase 3 (Firewall Control)
  ├─ Depends on Phase 2
  └─ Multi-platform adapters
      ↓
Phase 4+ (App Mgmt, Resources, Observability)
```

**Rule:** Can't start Phase 2 until Phase 1 secrets/audit are complete. Can't start Phase 3 until Phase 2 jar is wired.

---

## Rule 2: Gap Classification (Priority Framework)

**Classify gaps by impact:**

| Priority | Definition | Action | Timeline |
|---|---|---|---|
| **P1 Critical** | Blocks production | Fix in current phase | ASAP |
| **P2 High** | Blocks next phase | Schedule next sprint | 1-2 sprints |
| **P3 Medium** | Improves quality | Nice to have | 2-3 sprints |
| **P4 Low** | Cosmetic | Defer | Next roadmap |

**Examples:**

| Gap | Priority | Why | Target |
|---|---|---|---|
| Secrets redaction | P1 | Credentials leak in errors | Phase 1 |
| Audit logging | P1 | No compliance trail | Phase 1 |
| Jar backend wiring | P1 | UI not REST-based | Phase 1-2 |
| Structured errors | P2 | Debugging hard | Phase 2 |
| Cross-tenant tests | P2 | Security not verified | Phase 2 |
| TLS enforcement | P3 | Dev-only OK, prod needs | Phase 2 |
| Metrics collection | P4 | No dashboards | Phase 6 |

---

## Rule 4: Definition of Done (Phase Completion)

**Before a phase ships, checklist:**

- [ ] All P1 gaps closed
- [ ] All tasks marked "Done"
- [ ] Integration tests on real servers passing
- [ ] Security audit passed
- [ ] Cross-tenant denial tests passing (if applicable)
- [ ] Documentation updated
- [ ] Performance baseline established
- [ ] Operational runbook written
- [ ] Team trained

**Example - Phase 1 DoD:**
- ✅ All hardening operations logged with actor/timestamp
- ✅ Error messages don't expose credentials (scanning automation done)
- ✅ Structured error model with errorId, code, details
- ✅ Real server integration testing complete
- ✅ Production deployment tested and documented
