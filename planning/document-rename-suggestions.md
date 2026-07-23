document-rename-suggestions.md

# Document Rename Suggestions

## Purpose
This file captures suggested filename updates so names reflect actual document intent.

## 1) Rename Only (Same Folder)

1. Current: Dream Architecture Structured, architecture/dream-architecture-structured.md
	Suggested: Target Architecture Spec, architecture/target-architecture-spec.md
	Reason: This is a concrete architecture specification, not just a vision.
2. Current: State of Tomorrow, architecture/state-of-tomorrow.md
	Suggested: Target Architecture Overview, architecture/target-architecture-overview.md
	Reason: This reads as a future-state architecture overview.
4. Current: Wishlist, planning/ideas/wishlist.md
	Suggested: Capability Backlog, planning/ideas/capability-backlog.md
	Reason: This is a backlog of desired capabilities and priorities.
5. Current: Dual Licensing Strategy, planning/ideas/dual-licensing-strategy.md
	Suggested: Licensing Commercialization Strategy, planning/ideas/licensing-commercialization-strategy.md
	Reason: More precise that this is business and legal strategy.
6. Current: Server Control Architecture Questions, requirements/questions/server-control-architecture.md
	Suggested: Discovery Questionnaire, requirements/questions/discovery-questionnaire.md
	Reason: This is a requirements discovery intake questionnaire.
7. Current: Current State Investigation Foundation, planning/discovery/current-state-investigation-foundation.md
	Suggested: Current State Discovery Checklist, planning/discovery/current-state-discovery-checklist.md
	Reason: Shorter and more operationally clear naming.

## 2) Rename Only (Root Architecture Folder)

1. Current: Server Control Architecture Decisions, architecture/server-control-architecture-decisions.md
	Suggested: Architecture Decision Record, architecture/architecture-decision-record.md
	Reason: This is architecture decisions content and fits ADR-style naming better.

## 3) Move and Rename (Planning Documents)

1. Current: Implementation Roadmap, planning/roadmaps/implementation-roadmap.md
	Suggested: Architecture Implementation Roadmap, planning/roadmaps/architecture-implementation-roadmap.md
	Reason: Keeps planning artifacts out of architecture folder while clarifying delivery scope.

## 4) Keep As-Is

1. Name, path: Dream Architecture Vision, architecture/dream-architecture-vision.md
	Reason: Current name already matches content well.

## 5) Suggested Execution Order

1. Apply rename-only items.
2. Apply root-architecture renames.
3. Run a workspace search for old filenames and update links.

## Notes
- Suggestions are content-driven and independent of current folder placement.
- Where folder moves are implied, update internal links after renaming.
- Keep naming style consistent: lowercase kebab-case with intent-first wording.
