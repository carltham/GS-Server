# Roadmap: From Present to Target

**Project:** GS-Server  
**Version:** 1.0  
**Last Updated:** 2026-07-24  
**Timeline:** 28 weeks (6-7 months) to full production readiness

---

## Executive Summary

**Present State:** 28% complete (78 components, 4-tier architecture, hardening 95%, proxy 50%)  
**Target State:** Production-ready with audit logging, secrets protection, structured errors, multi-service architecture  
**Critical Path:** 6-8 weeks minimum (Phase 1 + Phase 2 blocking issues)  
**Full Timeline:** 28 weeks (all 6 phases)

---

## Gap Analysis

### Critical Gaps (Block Production)

| Gap | Impact | Effort | Priority | Target Phase |
|---|---|---|---|---|
| **Secrets redaction** | Credentials leak in errors | 3 days | P1 | Phase 1 |
| **Audit logging** | No compliance trail | 5 days | P1 | Phase 1 |
| **Jar wiring** | UI not REST-based | 1 week | P1 | Phase 2 |

### High-Priority Gaps (Block Phase 3)

| Gap | Impact | Effort | Priority | Target Phase |
|---|---|---|---|---|
| **Structured error model** | Debugging hard, no details | 3 days | P2 | Phase 2 |
| **Correlation IDs** | Can't trace across services | 2 days | P2 | Phase 2 |
| **Cross-tenant tests** | Security not verified | 1 week | P2 | Phase 2 |

### Medium-Priority Gaps (Improve Quality)

| Gap | Impact | Effort | Priority | Target Phase |
|---|---|---|---|---|
| **TLS enforcement** | Dev-only HTTP, prod needs HTTPS | 2 days | P3 | Phase 2 |
| **Structured logging** | No tenant/correlation context | 3 days | P3 | Phase 6 |
| **API specification** | No OpenAPI/Swagger | 2 days | P3 | Phase 2 |

---

## Phase-by-Phase Roadmap

### Phase 1: Foundation & Defence (2-3 weeks)

**Goal:** Production-ready hardening automation with audit trail

**Critical tasks:**

| Task | Gap | Effort | Status |
|---|---|---|---|
| Secrets redaction in errors | Critical | 3 days | 🔴 Not started |
| Audit logging framework | Critical | 5 days | 🔴 Not started |
| Structured error model | High | 3 days | 🔴 Not started |
| Wire hardening to production path | Current | 2 days | 🟡 In progress |
| Integration test on real server | Current | 1 week | 🔴 Not started |

**Definition of Done:**
- [ ] All hardening operations logged with actor/timestamp
- [ ] Error messages don't expose credentials
- [ ] Structured error model with errorId, code, details
- [ ] Real server integration testing complete
- [ ] Production deployment documented

**Timeline:** 2-3 sprints

---

### Phase 2: Gateway Proxy Controller (3-4 weeks)

**Goal:** Production-ready automated proxy control with jar wiring

**Critical tasks:**

| Task | Gap | Effort | Status |
|---|---|---|---|
| **Jar backend wiring (REST API)** | **Critical** | **1 week** | 🔴 Not started |
| Correlation ID propagation | High | 2 days | 🔴 Not started |
| Cross-tenant denial tests | High | 1 week | 🔴 Not started |
| TLS enforcement for HTTPS | Medium | 2 days | 🔴 Not started |
| API documentation (OpenAPI) | Medium | 2 days | 🔴 Not started |

**Definition of Done:**
- [ ] UI calls jar backend via REST HTTP (not direct injection)
- [ ] All operations return structured errors
- [ ] Correlation IDs trace requests across services
- [ ] HTTPS/TLS required for proxy configs
- [ ] OpenAPI spec generated and current
- [ ] Cross-tenant denial tests 100% passing

**Timeline:** 3-4 sprints

---

### Phase 3: Firewall Control (6-8 weeks)

**Goal:** Multi-platform firewall automation with parity validation

**Key tasks:**
- Firewall controller/service
- Linux adapter (iptables/firewalld)
- Windows adapter (Windows Defender)
- Cloud adapter (AWS security groups)
- Cross-platform rule parity tests

**Timeline:** 6-8 sprints

---

### Phase 4: Application Management (8-10 weeks)

**Goal:** Zero-downtime application deployment

**Key tasks:**
- App deployment controller/service
- Docker adapter
- Systemd adapter
- Kubernetes adapter
- Health checks and auto-recovery

**Timeline:** 8-10 sprints

---

### Phase 5: Resource Management (6-8 weeks)

**Goal:** CPU/memory quotas and auto-scaling

**Key tasks:**
- Resource quota controller/service
- cgroups adapter (Linux)
- Job objects adapter (Windows)
- Auto-scaling policies
- Resource monitoring

**Timeline:** 6-8 sprints

---

### Phase 6: Monitoring & Audit (10-12 weeks)

**Goal:** Full observability and compliance audit trail

**Critical tasks:**
- Structured logging (tenant, actor, correlation ID)
- Metrics collection (operation success rate, latency)
- Distributed tracing (request flow visualization)
- Compliance audit export
- Database persistence upgrade (SQLite → PostgreSQL)
- OAuth2 authentication (replace HTTP Basic)
- Rate limiting API endpoints
- Backup encryption

**Timeline:** 10-12 sprints

---

## Dependency Graph

```
Phase 1: Foundation & Defence
  ├─ Secrets redaction
  ├─ Audit logging framework
  └─ Structured error model

Phase 2: Gateway Proxy Wiring
  ├─ Depends on: Phase 1 (secrets, audit)
  ├─ Jar backend wiring (REST API)
  ├─ Correlation ID propagation
  └─ Cross-tenant denial tests

Phase 3: Firewall Control
  ├─ Depends on: Phase 2 (jar, structured errors)
  └─ Multi-platform adapters

Phase 4: App Management
  ├─ Depends on: Phase 3 (audit, errors)
  └─ Docker/Systemd/K8s

Phase 5: Resource Management
  ├─ Depends on: Phase 4 (deployment)
  └─ Quotas and auto-scaling

Phase 6: Observability
  ├─ Depends on: All phases (audit trail)
  └─ Full observability (metrics, traces, logs)
```

---

## Critical Path (Minimum to Ship)

**Production-ready hardening + proxy in 8 weeks:**

1. **Week 1-2:** Secrets redaction + audit logging (Phase 1)
2. **Week 3-4:** Jar backend wiring (Phase 2)
3. **Week 5-6:** Structured errors + correlation IDs (Phase 2)
4. **Week 7-8:** Cross-tenant denial tests (Phase 2-3)

**Checkpoint:** End of week 8 → Ready for production hardening + proxy

---

## Resource Estimate

| Phase | Effort | Team Size | Timeline |
|---|---|---|---|
| 1 | 20 days | 2 devs | 2-3 weeks |
| 2 | 30 days | 2 devs | 3-4 weeks |
| 3 | 40 days | 2-3 devs | 6-8 weeks |
| 4 | 50 days | 2-3 devs | 8-10 weeks |
| 5 | 40 days | 2 devs | 6-8 weeks |
| 6 | 45 days | 2-3 devs | 10-12 weeks |
| **Total** | **225 days** | **2-3 devs** | **28 weeks (6-7 months)** |

---

## Risk Mitigation

### Risk 1: Database Persistence Needed Mid-Phase

**Risk:** File-based JSON not sufficient for large datasets

**Mitigation:**
- Monitor operation count in Phase 2-3
- If > 100k operations, switch to SQLite (Phase 3)
- Target: 1M operations per server (file-based sufficient)

### Risk 2: OAuth2 Complexity

**Risk:** OAuth2 implementation takes longer than estimated

**Mitigation:**
- Keep HTTP Basic for Phase 1-2 (dev/staging)
- Delay OAuth2 to Phase 6
- Use JWT tokens as bridge (can add later)

### Risk 3: Cloud Adapters Complexity

**Risk:** AWS/GCP/Azure security group APIs have subtle differences

**Mitigation:**
- Start with Linux (iptables) in Phase 3
- Add cloud after Linux/Windows tested
- Invest in adapter test matrix

---

## Go/No-Go Checklist

**Before Phase X ships:**

- [ ] All tasks in phase marked "Done"
- [ ] Integration tests on real servers passing
- [ ] Security audit passed
- [ ] Cross-tenant denial tests passing
- [ ] Documentation updated
- [ ] Performance baseline established
- [ ] Operational runbook written
- [ ] Team trained

---

## Success Metrics

### Phase 1
- ✅ No secrets in error messages (automated scan passes)
- ✅ 100% operation audit logging
- ✅ Structured errors with details
- ✅ Real server integration tested

### Phase 2
- ✅ UI calls jar via REST HTTP
- ✅ All operations traced with correlation ID
- ✅ TLS enforced for HTTPS proxy configs
- ✅ OpenAPI spec generated

### Phase 3
- ✅ Cross-platform firewall rules parity-validated
- ✅ Rollback tested on all platforms
- ✅ Cross-tenant denial tests 100% passing

### Phase 6
- ✅ 100% operation audit trail (1+ year retention)
- ✅ Metrics dashboard (success rate, latency)
- ✅ Distributed traces (request flow)
- ✅ Compliance audit export ready

---

## Summary

**Roadmap to production (28 weeks):**
- **Blocking critical path:** Phase 1 (2-3 weeks) + Phase 2 (3-4 weeks) = 6-8 weeks minimum
- **Full target:** All 6 phases = 28 weeks (6-7 months)
- **Critical gaps:** Secrets redaction, audit logging, jar wiring, cross-tenant tests

**Next steps:** See [[TARGET_ARCHITECTURE_PRINCIPLES.md]] for architectural vision, [[TARGET_SECURITY_POLICY.md]] for security requirements.

