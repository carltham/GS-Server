# Implementation Rules: Quality Gates

**Extracted from:** IMPLEMENTATION_RULES.md  
**Last Updated:** 2026-07-24

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
