server-control-framework-planning.feature

Feature: Server Control Framework Planning Overview
  As a platform engineering team
  We want an overview of phase-specific Gherkin plans
  So that discovery and implementation are managed incrementally per phase

  Background:
    Given all phase plans are split into dedicated feature files
    And each phase starts with discovery before implementation work
    And project progress is tracked in planning/project-state.md

  # Progress and active execution source:
  # - planning/project-state.md

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
    And planning/project-state.md should provide the current progress overview
    And Phase 2 should focus on server gateway proxy controller delivery for nginx HTTPS control
    And cross-cutting quality gates should apply to all phases
