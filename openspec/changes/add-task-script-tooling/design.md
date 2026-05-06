## Context

PhoneClaw currently executes user requests primarily through live UI exploration via PhoneEmulationTool. This is flexible but expensive for repeated tasks and can be slow. The new requirement introduces a reusable script layer: once a task path is validated, the system should store and replay it through dedicated tools. At the same time, mobile UI volatility requires stricter script-runtime safety than exploratory automation.

Stakeholders include end users (faster repeated execution), agent/runtime developers (tool orchestration), and product/frontend teams (script management entry and operations).

## Goals / Non-Goals

**Goals:**
- Add a script registry that stores script metadata and executable content for reusable task automation.
- Add explicit tools for script listing and script execution, so model decisions happen through tool calls rather than prompt-injected script lists.
- Extend agent orchestration to choose script execution when appropriate and fall back safely.
- Add script management UI entry and pages for list/detail/read-only code view plus CRUD operations.
- Define reliability constraints for script execution: deterministic behavior, error detection, and fail-fast stopping.

**Non-Goals:**
- Full script marketplace, sharing, or multi-tenant distribution.
- Automatic script self-healing for broken selectors/flows.
- Rich in-browser script editor (initial detail view remains read-only code inspection).

## Decisions

1. Introduce dedicated script tools instead of prompt injection.
- Decision: Add `listTaskScripts` and `executeTaskScript` tool interfaces exposed to the agent.
- Rationale: Keeps script availability dynamic and auditable, avoids token-heavy prompt stuffing, and matches tool-driven architecture.
- Alternative considered: Inject script catalog into system prompt each turn. Rejected due to token cost, stale context risk, and weaker execution governance.

2. Separate script registry capability from execution engine.
- Decision: Keep metadata/content lifecycle (CRUD, indexing, querying) separate from runtime execution and validation.
- Rationale: Simplifies maintenance and allows independent hardening of runtime safety.
- Alternative considered: Single module handling storage and execution. Rejected due to coupling and harder testing.

3. Fail-fast runtime policy for scripts.
- Decision: On any critical mismatch or action precondition failure, terminate script execution and return explicit error; no unsafe blind clicks.
- Rationale: Production scripts are expected to be safer than exploratory mode.
- Alternative considered: Continue with heuristic retries/click guessing. Rejected because it can create destructive UI side effects.

4. UI integration via sidebar script center.
- Decision: Add a dedicated sidebar entry with list view, metadata display, and detail/code read-only page.
- Rationale: Improves discoverability and operational control while preserving current navigation model.
- Alternative considered: Hide script functions in settings/debug pages. Rejected due to poor task discoverability.

## Risks / Trade-offs

- [UI drift breaks scripts] -> Mitigation: strict precondition checks, explicit failures, and operator-visible error messages.
- [Model chooses wrong script] -> Mitigation: richer metadata (name, summary, created time), tool-level filtering, and fallback strategy.
- [Complexity in dual-path execution] -> Mitigation: clear policy ordering (script first when matched, explore fallback by policy) and isolated tests per path.
- [Increased storage consistency requirements] -> Mitigation: validate schema on create/update and enforce immutable created-time semantics.

## Migration Plan

1. Add storage schema/repository for task scripts and metadata.
2. Implement tool endpoints for listing and executing scripts.
3. Integrate execution path selection in agent executor.
4. Deliver sidebar entry and script list/detail pages.
5. Roll out behind feature flag if needed, monitor error rates, and disable script execution path quickly on regressions.

Rollback: disable script-tool registration and hide script UI entry while preserving stored data for later re-enable.

## Open Questions

- Should script matching be model-decided only, or include deterministic pre-filtering by task intent tags?
- Should script execution emit replay telemetry for future auto-quality scoring?
- Do we need per-script versioning at initial release or post-MVP?
