server-control-framework-planning.feature

Feature: Server Control Framework Planning Overview
  # Canonical Progress File: yes
  # Last Updated: 2026-07-23
  # Update Trigger: Any change to phase status, quality-gate status, or active execution boundary completion.
  # Update Owner: The engineer making the phase-changing commit.
  # Update Rule: Update this file first, then verify no other file contains progress bars.
  # Bar Format: 20 slots where each slot = 5% completion.
  # Rounding Rule: Round to nearest 5% for bar shape, keep exact percentage in text.
  #
  # Progress Dashboard (Delivery Phases)
  # Overall Completion (Phase 1-6): 18%
  # Overall Progress Bar: [###-----------------] 18%
  #
  # Phase 1 - Foundation and Defence: [################----] 80% (In Progress)
  # Phase 2 - Server Gateway Proxy Controller: [####----------------] 25% (Contract tests in progress)
  # Phase 3 - Firewall Control: [--------------------] 0% (Not Started)
  # Phase 4 - Application Management: [--------------------] 0% (Not Started)
  # Phase 5 - Resource Management: [--------------------] 0% (Not Started)
  # Phase 6 - Monitoring and Access Control: [--------------------] 0% (Not Started)
  #
  # Cross-cutting Quality Gates: [#########-----------] 45% (In Progress)
  #
  # Execution Progress (Active Phase)
  # Phase 1 Boundary Completion: [#############-------] 67% (4/6 complete)
  #
  # Next Target
  # Phase 2 Readiness: [####----------------] 25% (Contract tests in progress)
  # Phase 2 Focus: Server gateway proxy controller and nginx HTTPS control
  # Phase 2 Discovery Focus: Requirements, policy, rollback, and audit boundaries
  # Phase 2 Contract Focus: Controller API shape and service policy rules
  # Phase 2 Test Focus: Contract-first controller and API response coverage

  As a platform engineering team
  We want an overview of phase-specific Gherkin plans
  So that discovery and implementation are managed incrementally per phase

  Background:
    Given all phase plans are split into dedicated feature files
    And each phase starts with discovery before implementation work

  # Phase Plan Files
  # - planning/gherkin/phases/phase-1-foundation-and-defence.feature
  # - planning/gherkin/phases/phase-2-server-gateway-proxy-controller.feature
  # - planning/gherkin/phases/phase-3-firewall-control.feature
  # - planning/gherkin/phases/phase-4-application-management.feature
  # - planning/gherkin/phases/phase-5-resource-management.feature
  # - planning/gherkin/phases/phase-6-monitoring-and-access-control.feature
  # - planning/gherkin/phases/cross-cutting-quality-gates.feature

  Scenario: Confirm phase planning files are organized
    Given the phase feature files exist in planning/gherkin/phases
    When the team prepares sprint planning
    Then each phase should be planned from its dedicated file
    And Phase 2 should focus on server gateway proxy controller delivery for nginx HTTPS control
    And cross-cutting quality gates should apply to all phases

  Scenario: Review consolidated phase progress
    Given phase progress is maintained only in planning/gherkin/server-control-framework-planning.feature
    When the team reviews planning/gherkin/server-control-framework-planning.feature
    Then overall and per-phase completion should be visible in one place
    And the dashboard values should be updated whenever a phase status changes

  Scenario: Use overview as single source of progress truth
    Given phase planning and execution details exist in multiple files
    When phase status is updated
    Then planning/gherkin/server-control-framework-planning.feature should be updated first
    And no duplicate progress bars should be maintained elsewhere

  Scenario: Keep canonical progress dashboard updated
    Given implementation work changes phase or boundary completion
    When the engineer finalizes the change set
    Then Last Updated should be refreshed with the current date
    And overall, phase, and execution progress values should be recalculated
    And cross-cutting quality-gate progress should be synchronized

  Scenario: Show the next phase target clearly
    Given Phase 1 is the active boundary stream
    When the team plans the next implementation slice
    Then the overview should show Phase 2 as the active discovery target
    And the Phase 2 focus should remain server gateway proxy controller delivery

  Scenario: Start Phase 2 discovery planning
    Given Phase 1 boundary work is stable
    When the team prepares the next phase backlog
    Then Phase 2 discovery should be the immediate active work item
    And nginx HTTPS proxy control should remain the first managed capability

  Scenario: Capture Phase 2 discovery output
    Given Phase 2 discovery work is underway
    When the team records findings from gateway proxy and nginx analysis
    Then requirements, policy, rollback, and audit boundaries should be documented
    And the next Phase 2 contract sprint should be prepared from those findings

  Scenario: Define Phase 2 contract boundaries
    Given Phase 2 discovery findings are recorded
    When the team shapes the gateway proxy controller and service contracts
    Then the controller API should remain orchestration only
    And the service layer should own validation, policy, and rollback decisions

  Scenario: Plan Phase 2 contract tests
    Given the Phase 2 controller and service contract boundaries are defined
    When the team prepares the first verifiable API boundary
    Then controller orchestration tests should be listed first
    And structured success and error response contract tests should be next
