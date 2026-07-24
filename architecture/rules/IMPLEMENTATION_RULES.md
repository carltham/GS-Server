# GS-Server Implementation Rules (Index)

**Last Updated:** 2026-07-24

This file is an index to the implementation rules organized by concern. Each area has its own focused document.

---

## 📋 Phase Management

**File:** [IMPL_PHASE_MANAGEMENT.md](IMPL_PHASE_MANAGEMENT.md)

Rules for managing phase dependencies, gap classification, and definition of done:
- **Rule 1:** Phase Progression (blocking order)
- **Rule 2:** Gap Classification (priority framework)
- **Rule 4:** Definition of Done (phase completion)

**When to use:** Before starting a phase, to understand blockers and completion criteria.

---

## ⏰ Timeline & Estimation

**File:** [IMPL_TIMELINE_ESTIMATION.md](IMPL_TIMELINE_ESTIMATION.md)

Rules for estimating effort, tracking velocity, and planning timelines:
- **Rule 3:** Critical Path (minimum to ship in 8 weeks)
- **Rule 6:** Effort Estimation Rules (buffer allocation)
- **Rule 8:** Team Size & Timeline (27-week total roadmap)
- **Rule 9:** Velocity Tracking (sprint health metrics)

**When to use:** During sprint planning, for estimation and tracking team velocity.

---

## 🚀 Development Workflow

**File:** [IMPL_DEVELOPMENT_WORKFLOW.md](IMPL_DEVELOPMENT_WORKFLOW.md)

Rules for day-to-day development, blocker resolution, and Iterative TDD:
- **Rule 5:** Blocker Resolution (decision framework)
- **Rule 7:** Risk Mitigation (known risks and strategies)
- **Rule 10:** Pre-Phase Planning (before/during/after phase)
- **Rule 11:** Scope Decisions (what to add/defer)
- **Rule 14:** Roadmap Review (keep it current)
- **Rule 16:** Iterative TDD (layer-by-layer implementation) — **PRIMARY DEVELOPMENT APPROACH**

**When to use:** Every day, to guide implementation decisions and blocker resolution.

---

## ✅ Quality Gates

**File:** [IMPL_QUALITY_GATES.md](IMPL_QUALITY_GATES.md)

Rules for success metrics and escalation paths:
- **Rule 12:** Success Metrics (how we know it worked, by phase)
- **Rule 13:** Escalation Path (when things go wrong)

**When to use:** End of phase, to verify completion; during incidents, to escalate quickly.

---

## 🔧 Maintenance

**File:** [IMPL_MAINTENANCE.md](IMPL_MAINTENANCE.md)

Rules for ongoing maintenance after all phases complete:
- **Rule 15:** Maintenance Window (per sprint, per quarter, annually)

**When to use:** After Phase 6 completes, for ongoing operations and security.

---

## Quick Navigation

**Looking for something specific?**

| Question | Document | Rule |
|---|---|---|
| Can we start Phase 2? | Phase Management | Rule 1 |
| Is this bug P1 or P3? | Phase Management | Rule 2 |
| What's the critical path to ship? | Timeline | Rule 3 |
| How long should Phase 2 take? | Timeline | Rule 8 |
| How do we build this feature? | Development Workflow | Rule 16 (Iterative TDD) |
| What's blocking us? | Development Workflow | Rule 5 |
| Did we succeed? | Quality Gates | Rule 12 |
| Who do I escalate to? | Quality Gates | Rule 13 |

---

## Key Principle: Iterative TDD

All development follows **Rule 16: Iterative TDD - Layer-by-Layer Implementation**.

See [IMPL_DEVELOPMENT_WORKFLOW.md - Rule 16](IMPL_DEVELOPMENT_WORKFLOW.md#rule-16-iterative-tdd---layer-by-layer-implementation) for complete workflow.

**TL;DR:** Start with E2E Playwright test → fails at layer boundary → write focused integration test → implement → resume → repeat.

---

## Reference

**Related documents:**
- [ARCHITECTURE_RULES.md](ARCHITECTURE_RULES.md) — Core architectural principles
- [TESTING_RULES.md](TESTING_RULES.md) — Testing standards (aligned with Iterative TDD)
- [SECURITY_RULES.md](SECURITY_RULES.md) — Security requirements
- [TARGET_ROADMAP_TO_TARGET.md](../target/ROADMAP_TO_TARGET.md) — Phase timeline and gaps
