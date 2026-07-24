# GS-Server Implementation Rules

**Extracted from:** ROADMAP_TO_TARGET.md  
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

---

## Rule 5: Blocker Resolution (Decision Framework)

**When a phase is blocked:**

1. **Check dependencies** - Is prior phase done?
2. **Classify blocker** - Is it P1 (production) or P3 (nice to have)?
3. **Resolve P1** - In current phase, delay non-critical work
4. **Defer P3** - Push to next phase or roadmap review

**Example resolution:**
```
Blocker: "Can't complete proxy (Phase 2) until jar wiring done"
├─ Check: Phase 1 (secrets/audit) complete? ✅ YES
├─ Classify: Jar wiring is P1 (critical for Phase 2)
├─ Action: Prioritize jar wiring, defer nice-to-have TLS until phase complete
└─ Timeline: Add 1 sprint if needed
```

**Escalation:**
- Phase > 6 weeks for 2 devs → Consider splitting
- Real server testing fails → Extend phase
- Security audit finds issues → Fix before shipping

---

## Rule 6: Effort Estimation Rules

**Estimate by:**
- Historical velocity (past sprints)
- Complexity (single-platform vs multi-platform)
- Testing requirements (cross-tenant tests add 30-50%)
- Integration risk (new adapters are riskier)

**Buffer allocation:**
- Base estimate: 20 days
- Add 30% for unknown unknowns (testing, integration)
- Realistic: 26 days for 2-week sprint

**Track actual vs estimated:**
- Sprint 1 estimate: 3 days → Actual: 4 days (adjust future)
- Sprint 2 estimate: 5 days → Actual: 3 days (confidence building)
- Sprint 3 estimate: 2 days → Actual: 8 days (investigate)

**Adjustment rule:**
- If 3 consecutive sprints beat estimate → Reduce estimates
- If 2 consecutive sprints miss estimate → Add buffer

---

## Rule 7: Risk Mitigation (Known Risks)

### Risk 1: Database Persistence Mid-Phase

**Risk:** File-based JSON not sufficient for large datasets

**Mitigation:**
- Monitor operation count in Phase 2-3
- If > 100k operations, switch to SQLite
- Current target: 1M operations per server (file-based sufficient)

### Risk 2: OAuth2 Complexity

**Risk:** OAuth2 implementation takes longer than estimated

**Mitigation:**
- Keep HTTP Basic for Phase 1-2 (dev/staging only)
- Delay OAuth2 to Phase 6
- Use JWT tokens as bridge (can add later)

### Risk 3: Cloud Adapters Complexity

**Risk:** AWS/GCP/Azure security group APIs have subtle differences

**Mitigation:**
- Start with Linux (iptables) in Phase 3
- Add cloud after Linux/Windows tested
- Invest in adapter test matrix

### Risk 4: Real Server Integration Failures

**Risk:** Tests pass locally but fail on real servers

**Mitigation:**
- Phase includes real server testing (not just docker/sandbox)
- Staging environment mirrors production
- Rollback tested on real servers

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

---

## Rule 10: Pre-Phase Planning (What Happens Before)

**Before phase starts (day 0):**
- ✅ Previous phase review (DoD checklist)
- ✅ Dependencies verified (prior phase complete)
- ✅ Team capacity confirmed (2-3 devs available)
- ✅ Blockers identified and mitigated
- ✅ Real servers provisioned (if needed)
- ✅ Runbook templates prepared

**During phase kickoff (day 1):**
- ✅ Detailed task breakdown
- ✅ Dependencies mapped (task A blocks task B)
- ✅ Definition of Done for phase confirmed
- ✅ Success metrics agreed
- ✅ Risk escalation path clarified

**After phase ships (day N):**
- ✅ Retrospective (what went well, what didn't)
- ✅ Velocity analysis (adjust estimates)
- ✅ Blocker analysis (prevent recurrence)
- ✅ Documentation of lessons learned

---

## Rule 11: Scope Decisions (Stay on Track)

**When to add work to a phase:**
- ✅ Dependency blocking next phase
- ✅ Security finding requires immediate fix
- ✅ 20% capacity buffer still available

**When to defer to next phase:**
- ❌ Nice-to-have feature
- ❌ Performance optimization
- ❌ Code cleanup/refactoring
- ❌ New capability not on roadmap

**Rule:** Never defer P1/P2 gaps. Always defer P3/P4.

---

## Rule 12: Success Metrics (How We Know It Worked)

**Phase 1 metrics:**
- ✅ No secrets in error messages (automated scan passes)
- ✅ 100% operation audit logging (sample logs verified)
- ✅ Structured errors with errorId, code, details
- ✅ Real server integration tested

**Phase 2 metrics:**
- ✅ UI calls jar via REST HTTP (network trace verified)
- ✅ All operations traced with correlation ID
- ✅ TLS enforced for HTTPS proxy configs
- ✅ OpenAPI spec generated and current

**Phase 3 metrics:**
- ✅ Cross-platform firewall rules parity-validated
- ✅ Rollback restores previous rules
- ✅ Cross-tenant denial tests 100% passing

**Phase 6 metrics:**
- ✅ 100% operation audit trail (1+ year retention)
- ✅ Metrics dashboard (success rate, latency)
- ✅ Distributed traces (request flow visualization)
- ✅ Compliance audit export ready

---

## Rule 13: Escalation Path (When Things Go Wrong)

**Daily blocker** (team can't progress):
- Dev → Engineering Lead
- Expected response: < 4 hours
- Decision: unblock or adjust scope

**Phase blocker** (phase can't ship):
- Engineering Lead → Product/Security Lead
- Expected response: < 1 day
- Decision: extend phase, defer gaps, or escalate

**Production incident** (shipped code breaks):
- Team → On-call + Security Lead
- Expected response: < 30 minutes
- Action: rollback or hotfix

---

## Rule 14: Roadmap Review (Keep it Current)

**Per sprint:**
- Review gaps vs target
- Reprioritize if business changes
- Track velocity (adjust estimates)
- Update known risks

**Per phase:**
- Retrospective (what worked, what didn't)
- Adjust estimates for remaining phases
- Plan next phase in detail (task breakdown)

**Quarterly:**
- Full architecture review
- Re-assess phase priorities
- Update target based on business changes
- Adjust timeline if needed

---

## Rule 15: Maintenance Window (Ongoing)

**After all phases complete:**

**Per sprint:**
- Bug fixes (P1 same-day, P2 < 1 week)
- Security patches (immediate)
- Dependency updates (monthly)
- Monitoring & alerting (24/7)

**Per quarter:**
- Compliance audit
- Penetration testing
- Performance optimization
- Capacity planning

**Annually:**
- Architecture review
- Technology refresh (dependencies, frameworks)
- Disaster recovery drill
- Security training

