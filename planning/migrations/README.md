# GS-Server Migration Plans (Present → Target Architecture)

**Updated:** 2026-07-24  
**Timeline:** 28 weeks (6-7 months) to full production readiness  
**Critical Path:** 6-8 weeks (Phase 1 + Phase 2)

---

## Overview

This folder contains Gherkin-based migration plans to evolve GS-Server from its present state (28% complete, scaffolded components) to full production-ready target state (aligned with organizational standards).

**Each migration plan includes:**
- ✅ Detailed scenarios in Gherkin format (BDD-friendly)
- ✅ Step-by-step implementation guidance
- ✅ Code examples and patterns
- ✅ Test coverage and verification steps
- ✅ Security checklists and compliance checks
- ✅ Production deployment procedures
- ✅ Rollback procedures

---

## Migration Plans (Organized by Focus Area)

### Phase 1: Foundation & Defence (2-3 weeks)
**Goal:** Production-ready hardening automation with audit trail and secrets protection

Organized into 4 focused feature files:

1. **PHASE_1_01_SECRETS_REDACTION.feature** (3 days)
   - Identify error message leakage points
   - Create ErrorRedactor utility class
   - Wire redaction into ApiExceptionHandler
   - Test all error paths for secrets exposure
   - Team training and pre-commit hooks

2. **PHASE_1_02_AUDIT_LOGGING.feature** (5 days)
   - Design audit logging schema and repository
   - Wire audit logging into HardeningService
   - Wire audit logging into other services (proxy, users, auth, terminal)
   - Implement audit log export API
   - Verify audit logging completeness
   - Configure retention policy

3. **PHASE_1_03_STRUCTURED_ERRORS.feature** (3 days)
   - Define ErrorResponse schema with all required fields
   - Create comprehensive ErrorCatalog
   - Replace generic error handler with structured format
   - Generate error documentation
   - Test all error scenarios for structure
   - Verify client integration

4. **PHASE_1_04_INTEGRATION_VERIFICATION.feature**
   - End-to-end hardening operation with all Phase 1 components
   - Cross-component integration verification
   - Security audit Phase 1
   - Production deployment Phase 1
   - Post-deployment verification
   - Rollback procedure
   - Phase 1 completion verification

**Critical blockers resolved:**
- ✅ Secrets no longer leak in error messages
- ✅ Audit trail with actor and timestamp available
- ✅ Structured errors with error codes for programmatic handling

---

### Phase 2: Gateway Proxy Wiring (3-4 weeks)
**Goal:** Multi-service architecture via REST + Correlation IDs + Cross-tenant testing

Organized into 4 focused feature files:

1. **PHASE_2_01_JAR_BACKEND_WIRING.feature** (1 week)
   - Analyze current direct injection architecture
   - Move service implementations to jar module
   - Create REST controllers in jar (Handlers)
   - Wire UI to call jar via REST HTTP
   - Test jar-to-UI communication
   - Deploy jar module to production

2. **PHASE_2_02_CORRELATION_IDS.feature** (2 days)
   - Design correlation ID flow across services
   - Implement CorrelationIdFilter in jar
   - Thread correlationId through service layer (via MDC)
   - Update error responses to include correlationId
   - Add correlationId to UI service calls
   - Test correlation ID flow end-to-end

3. **PHASE_2_03_CROSS_TENANT_TESTS.feature** (1 week)
   - Implement cross-tenant test fixtures
   - Write hardening cross-tenant denial tests
   - Write proxy cross-tenant denial tests
   - Write user management cross-tenant denial tests
   - Run complete cross-tenant test suite
   - Verify test isolation

4. **PHASE_2_04_IMPROVEMENTS_DEPLOYMENT.feature** (2-3 days)
   - Enhance error responses with full structured format
   - Configure HTTPS/TLS for production
   - Generate OpenAPI specification
   - Create Phase 2 deployment runbook
   - Integration test Phase 2 end-to-end
   - Security audit Phase 2
   - Production deployment Phase 2
   - Post-deployment verification
   - Phase 2 completion verification

**Critical blockers resolved:**
- ✅ UI calls jar via REST HTTP (services can scale independently)
- ✅ Correlation IDs enable request tracing across services
- ✅ Cross-tenant access is tested and denied
- ✅ Structured errors fully compliant with API standards

---

## Phase 3-6 (Not Yet Detailed)

Future migration plans for remaining phases:

**Phase 3: Firewall Control (6-8 weeks)**
- Multi-platform firewall automation (Linux iptables, Windows Defender, AWS security groups)
- Cross-platform rule parity testing
- Rollback testing

**Phase 4: Application Management (8-10 weeks)**
- Zero-downtime application deployment
- Docker, Systemd, Kubernetes adapters
- Health checks and auto-recovery

**Phase 5: Resource Management (6-8 weeks)**
- CPU/memory quotas and auto-scaling
- cgroups (Linux), Job objects (Windows)
- Resource monitoring

**Phase 6: Observability (10-12 weeks)**
- Structured logging with tenant/actor/correlationId
- Metrics collection and dashboards
- Distributed tracing
- Compliance audit export
- Database upgrade (file-based → SQLite → PostgreSQL)
- OAuth2 authentication
- Rate limiting

---

## How to Use These Migration Plans

### For Project Managers
1. Read this README to understand timeline and scope
2. Review Phase 1 scenarios to understand complexity
3. Use Gherkin scenarios as acceptance criteria for sprint planning
4. Track completion by scenario, not just line count

### For Developers
1. Start with Phase 1 FOUNDATION_MIGRATION.feature
2. Read each scenario to understand implementation steps
3. Use code examples in scenarios as implementation templates
4. Write tests before code (Gherkin scenarios define test cases)
5. Reference linked architecture documents (TARGET_ARCHITECTURE_PRINCIPLES.md, etc.)

### For QA/Test Engineers
1. Use Gherkin scenarios as test specifications
2. Convert scenarios to automated test cases
3. Verify security checklist steps are met
4. Run cross-tenant denial tests to verify isolation
5. Verify secrets redaction before deployment

### For DevOps/SRE
1. Review deployment scenarios (last scenario in each phase)
2. Prepare monitoring and rollback procedures
3. Verify production checklist steps before deployment
4. Monitor error rates and audit log volume post-deployment

---

## Migration Checklist (By Phase)

### Phase 1 Complete When:
- [ ] Secrets Redaction scenarios implemented and passing
- [ ] Audit Logging Framework wired and tested
- [ ] Structured Error Model deployed to production
- [ ] Security audit passed
- [ ] Team trained on new error format
- [ ] Documentation updated
- [ ] Production deployment successful

### Phase 2 Complete When:
- [ ] Jar module wired and REST communication working
- [ ] Correlation IDs flow through all layers
- [ ] Cross-tenant denial tests 100% passing
- [ ] Enhanced error model in production
- [ ] HTTPS/TLS enforced
- [ ] OpenAPI specification auto-generated
- [ ] Security audit passed
- [ ] Production deployment successful

---

## Risk Mitigation

### Phase 1 Risks
- **Risk:** Error redaction too aggressive, hides debugging info
  - **Mitigation:** Maintain full error context in logs (behind authentication), redact only in API responses

- **Risk:** Audit logging too verbose, overwhelms storage
  - **Mitigation:** Log only write operations, compress/archive after 30 days, retention 1+ year

- **Risk:** Correlation ID not unique, causes tracing confusion
  - **Mitigation:** Use UUID v4 (128-bit), verify uniqueness in tests

### Phase 2 Risks
- **Risk:** REST call latency adds 50-100ms per request
  - **Mitigation:** Use HTTP/2, connection pooling, monitor latency in Phase 2

- **Risk:** Jar service down, UI calls fail
  - **Mitigation:** Implement circuit breaker, fallback to cached responses, alerts on jar health

- **Risk:** Cross-tenant test false-positive (bad setup)
  - **Mitigation:** Use test fixtures, verify fixture isolation, manual spot-check

---

## Success Metrics

### Phase 1 Success
- ✅ No secrets in error messages (automated scan: 0 findings)
- ✅ 100% of write operations logged in audit trail
- ✅ All error responses include errorId
- ✅ Production deployment stable for 1 week

### Phase 2 Success
- ✅ UI calls jar backend (0 direct service injection)
- ✅ 100% of requests traced with correlationId
- ✅ Cross-tenant denial tests: 100% passing
- ✅ Error responses: 100% structured
- ✅ Jar and UI can scale independently
- ✅ Production deployment stable for 1 week

---

## Communication & Alignment

### Team Alignment Meeting (Before Phase 1)
- ✅ Explain why each change matters (secrets, audit, errors)
- ✅ Review scenarios together
- ✅ Assign ownership (who implements what)
- ✅ Set sprint goals based on scenarios

### Sprint Planning (Each Phase)
- ✅ Use scenarios as acceptance criteria
- ✅ Estimate effort by scenario (not by method count)
- ✅ Verify Phase N-1 complete before starting Phase N
- ✅ Plan security review before production deployment

### Sprint Review (End of Phase)
- ✅ Demo each completed scenario
- ✅ Verify security checklist passed
- ✅ Review production deployment procedures
- ✅ Capture learnings for next phase

---

## Reference Documents

**Architecture Documents:**
- `/architecture/present/ARCHITECTURE_OVERVIEW.md` — Current state baseline
- `/architecture/target/TARGET_ARCHITECTURE_PRINCIPLES.md` — 8 principles defining target
- `/architecture/target/TARGET_API_STANDARDS.md` — REST API standards (error model, validation)
- `/architecture/target/TARGET_SECURITY_POLICY.md` — Security requirements (audit, secrets, multi-tenant)
- `/architecture/target/TARGET_DEVELOPMENT_RULES.md` — Development standards (4-tier minimum)
- `/architecture/target/ROADMAP_TO_TARGET.md` — Phase-by-phase timeline

**Rule Files:**
- `/architecture/rules/ARCHITECTURE_RULES.md` — 8 core principles enforcement
- `/architecture/rules/API_DESIGN_RULES.md` — API design checklist
- `/architecture/rules/SECURITY_RULES.md` — Security enforcement rules
- `/architecture/rules/TESTING_RULES.md` — Testing requirements
- `/architecture/rules/DEVELOPMENT_RULES.md` — Code development standards

**Project Management:**
- `/planning/project-state.md` — Current project status and progress
- `/planning/migrations/` — This folder with Gherkin migration plans

---

## Next Steps

1. **Review Phase 1 scenarios** with the team
2. **Estimate effort** for Phase 1 tasks
3. **Plan Phase 1 sprint** based on Gherkin scenarios
4. **Implement Phase 1** following scenario steps
5. **Verify Phase 1 completion** using security checklist
6. **Begin Phase 2** after Phase 1 production deployment is stable

---

## FAQ

**Q: Why Gherkin format?**
A: Gherkin is human-readable, testable, and links requirements to test cases. Each scenario becomes an acceptance test.

**Q: Can we run phases in parallel?**
A: No. Phase 2 depends on Phase 1 (audit logging). Phase 3 depends on Phase 2 (jar wiring). Follow the sequence.

**Q: What if Phase X takes longer than planned?**
A: Adjust sprint plan, but don't skip scenarios. Each scenario addresses a critical gap. Defer Phase X+1, don't compress X.

**Q: How do we verify Phase X is complete?**
A: Run the "Phase X completion verification" scenario. All checkboxes must be checked.

**Q: Can we rollback if Phase X deployment fails?**
A: Yes. Each phase includes rollback procedures. Keep previous version available for 1 week post-deployment.

**Q: What about testing Phase X?**
A: Gherkin scenarios ARE the tests. Convert scenarios to automated tests (JUnit, Selenium, etc.) as you implement.

---

## Version History

| Date | Change |
|---|---|
| 2026-07-24 | Initial Phase 1 & Phase 2 migration plans created |
| TBD | Phase 3-6 migration plans added |

**Last Updated:** 2026-07-24

