# Implementation Rules: Development Workflow

**Extracted from:** IMPLEMENTATION_RULES.md  
**Last Updated:** 2026-07-24

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

## Rule 16: Iterative Top-Down TDD (Stack-Based Layer-by-Layer)

Build features top-down using contract-driven Test-Driven Development. Use a stack-based approach: push tests when hitting boundaries, pop tests back up to integrate layers.

**Core principle:** Each layer below adapts to the contract defined by the layer above. E2E test is the ultimate authority. No mocks—real integrations throughout.

### Phase 1: Push Down (Writing tests, discovering boundaries)

1. Write E2E test (defines what user sees)
2. Run it → fails when hitting Handler layer (doesn't exist yet)
3. **PAUSE E2E test, PUSH onto stack**
4. Write Handler test (defines what Handler must do)
5. Run it → fails when calling Service (doesn't exist yet)
6. **PAUSE Handler test, PUSH onto stack**
7. Write Service test (defines what Service must do)
8. Run it → fails when calling Adapter (doesn't exist yet)
9. **PAUSE Service test, PUSH onto stack**
10. Write Adapter test (defines what Adapter must do)
11. Run it → can implement External layer (DB/process/file)
12. **Adapter test PASSES** (bottom layer working)

Stack now:
```
E2E test (paused)
Handler test (paused)
Service test (paused)
Adapter test (PASSING)
```

### Phase 2: Pop Up (Integrating layers, fixing upward)

1. **POP Adapter test** → already passing ✅
2. **POP Service test** → runs against REAL working Adapter → if it fails, **FIX ADAPTER** to match Service's contract → Service test passes
3. **POP Handler test** → runs against REAL working Service → if it fails, **FIX SERVICE** to match Handler's contract → Handler test passes
4. **POP E2E test** → runs against full stack → if it fails, **FIX ALL LAYERS** to match E2E's contract → E2E test passes (must fully work as defined)

### Key Rules

**Rule 1: No Mocks**
- Every test calls REAL implementations of downstream layers
- Tests are not isolated by mocks; they integrate immediately
- Real failures reveal real contract mismatches

**Rule 2: Fixes Flow Upward**
- Layer below adapts to the contract of layer above
- Not the other way around
- E2E test defines everything; all layers satisfy it

**Rule 3: Push at Every Boundary**
- When a test fails at a layer boundary, pause it
- Push it onto the stack
- Write a new test for the next layer

**Rule 4: E2E Test is Authority**
- E2E test defines the complete system contract
- All layer implementations must satisfy E2E
- If E2E fails, fix layers until it passes

### Layer Boundaries in GS-Server (4-tier)

- **E2E ➔ Handler:** HTTP API contract
- **Handler ➔ Service:** Request processing contract
- **Service ➔ Adapter:** Business logic execution contract
- **Adapter ➔ External:** System execution contract
