# Implementation Rules: Timeline & Estimation

**Extracted from:** IMPLEMENTATION_RULES.md  
**Last Updated:** 2026-07-24

---

## Rule 3: Critical Path (Minimum to Ship)

**Rule:** Build minimum viable production in weeks 1-8.

**Critical path:**
1. **Week 1-2:** Secrets redaction + audit logging (Phase 1)
2. **Week 3-4:** Jar backend wiring (Phase 2)
3. **Week 5-6:** Structured errors + correlation IDs (Phase 2)
4. **Week 7-8:** Cross-tenant denial tests (Phase 2-3)

**Checkpoint:** End of week 8 → Ready for production hardening + proxy.

**Non-critical work** (defer if behind):
- ✅ Rate limiting (Phase 6)
- ✅ Metrics dashboards (Phase 6)
- ✅ Structured logging (Phase 6)
- ✅ OAuth2 migration (Phase 6)

---

## Rule 6: Effort Estimation Rules (Stack-Based TDD)

**Estimate by:**
- Historical velocity (past sprints)
- Complexity (single-platform vs multi-platform)
- Testing requirements (cross-tenant tests add 30-50%)
- Integration risk (new adapters are riskier)
- **Stack-based TDD overhead** (real implementations, not mocks)

**Stack-Based TDD Effort Buffer:**
- Mock-based TDD: X hours
- Stack-based TDD (real implementations): X × 1.6 hours
- Reason: Setup fixtures, real dependencies, integration testing
- New adapters (with real external): +60% overhead
- Established adapters (reusing fixtures): +30% overhead

**Buffer allocation:**
- Base estimate: 20 days
- Add 30% for unknown unknowns (testing, integration)
- Add 50-75% for stack-based TDD (real implementations, fixtures, discovery)
- Realistic: 35-40 days for 2-week sprint (requires team adjustment or scope reduction)

**Example:**
- Task: Implement hardening adapter (new)
- Base: 3 days
- Testing (cross-tenant): +1 day
- Stack-based TDD (real implementations, fixtures): +2 days
- Total: 6 days (not 3 days)

**Track actual vs estimated:**
- Sprint 1 estimate: 3 days → Actual: 4 days (adjust future)
- Sprint 2 estimate: 5 days → Actual: 3 days (confidence building)
- Sprint 3 estimate: 2 days → Actual: 8 days (investigate)

**Adjustment rule:**
- If 3 consecutive sprints beat estimate → Reduce estimates
- If 2 consecutive sprints miss estimate → Add buffer

---

## Rule 8: Team Size & Timeline

**For 2 developers:**

| Phase | Effort | Timeline |
|---|---|---|
| 1 | 20 days | 2 weeks |
| 2 | 30 days | 3 weeks |
| 3 | 40 days | 6 weeks |
| 4 | 50 days | 8 weeks |
| 5 | 40 days | 6 weeks |
| 6 | 45 days | 7 weeks |
| **Total** | **225 days** | **28 weeks (6-7 months)** |

**For 3 developers:**
- Phases 3-4: Can run in parallel where work is independent
- Timeline: 20-22 weeks (4.5-5 months)

**For 1 developer:**
- 50+ weeks (not recommended for Phase 3+)

---

## Rule 9: Velocity Tracking (Sprint Health)

**Track per sprint:**
- Estimate (story points or days)
- Actual (days completed)
- Velocity (actual / estimate)
- Issues encountered

**Healthy velocity:**
- 0.9 → 1.1 (estimates good)
- 0.7 → 0.9 (underestimating, need buffer)
- 1.2+ (overestimating, can increase work)

**Actions if unhealthy:**
- Velocity < 0.7 for 2 sprints → Add 50% buffer to future estimates
- Velocity > 1.3 for 2 sprints → Reduce estimates, increase work
- Erratic velocity → Identify blockers, unblock sprint
