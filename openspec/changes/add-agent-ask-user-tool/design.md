## Context

Current agent execution can call tools and stream responses, but it does not provide a standardized human-decision checkpoint. In practical workflows (for example, preference selection or risk confirmation), the agent needs a structured way to collect a user decision. The change introduces a structured `AskUser` tool and handles it as a normal tool invocation path.

Constraints:
- User response must be explicit and confirmable (selection is not auto-submitted).
- Option count must remain bounded for UI clarity and predictable payload size.
- AskUser interaction panel must be presented as a bottom sheet.
- Existing agent callback and persistence flow must stay compatible.

## Goals / Non-Goals

**Goals:**
- Provide a domain-level `AskUser` tool contract with question + selectable options + free-text fallback.
- Ensure the execution pipeline can process `AskUser` through the existing tool-calling mechanism with user-confirmed data.
- Define validation and UX rules (max options, required confirmation, typed `Other` handling).
- Preserve observability through existing callback flow and logs.

**Non-Goals:**
- Building multi-turn wizard forms beyond one question per tool invocation.
- Introducing automatic retries/auto-selection heuristics for unanswered questions.
- Replacing existing tool callback interfaces with a completely new event model.

## Decisions

### 1. Introduce `AskUser` as a first-class tool
- Decision: Add a dedicated tool schema (question + options array + optional other-input support) instead of overloading free-form chat text.
- Rationale: Tool schema gives strict validation and predictable UI rendering.
- Alternative considered: Let LLM ask plain text and parse user replies. Rejected due to ambiguity and poor reliability for deterministic resume.

### 2. Keep AskUser in existing tool invocation flow
- Decision: Treat `AskUser` as a normal tool call and return confirmed user input as the tool result, without introducing new executor states.
- Rationale: Reuses existing execution path and minimizes integration complexity.
- Alternative considered: Add dedicated wait/resume state machine transitions. Rejected due to unnecessary complexity for this feature.

### 3. Enforce UI and payload guardrails in validator layer
- Decision: Validate at tool boundary: options count 1..5, non-empty question text, non-empty option labels, and `Other` text required when selected.
- Rationale: Keeps backend behavior stable regardless of LLM output quality.
- Alternative considered: Validate only in UI. Rejected because non-UI clients and future integrations would bypass safeguards.

### 4. Standardize AskUser UI as bottom sheet
- Decision: Render AskUser using a bottom sheet presentation anchored from screen bottom.
- Rationale: Matches requested interaction pattern and keeps a lightweight decision UI without taking over the full screen.
- Alternative considered: centered modal dialog. Rejected because it does not meet the explicit UX constraint.

### 5. Do not extend callback contract
- Decision: Reuse current tool call callbacks and lifecycle events without adding new callback methods for `AskUser`.
- Rationale: Keeps API stable and avoids churn for callback consumers.
- Alternative considered: Add dedicated wait/resume callback events. Rejected because current tool callback granularity is sufficient.

## Risks / Trade-offs

- [Risk] AskUser call can remain pending if user never confirms. -> Mitigation: add timeout/expiry policy and surfaced pending state in session metadata.
- [Risk] LLM may emit malformed options or too many options. -> Mitigation: strict validation + user-visible error + prompt guidance for regeneration.
- [Risk] User-provided free text can be noisy for downstream tool reasoning. -> Mitigation: normalize answer payload and include selected source (`option` vs `other`) in result schema.
- [Trade-off] Keeping existing callback contract reduces event expressiveness for UI observers. -> Mitigation: rely on existing tool start/end callbacks and execution logs.

## Migration Plan

1. Add domain contracts for `AskUser` request/response payloads.
2. Implement tool validator + UI request envelope.
3. Integrate AskUser handling into existing tool execution path and persistence hooks.
4. Add telemetry for user-decision checkpoints through existing callback flow.
5. Roll out behind feature flag; fallback path keeps current behavior when flag is disabled.
6. Rollback strategy: disable flag and bypass `AskUser` tool registration.

## Open Questions

- Should timeout behavior auto-fail the run or keep it resumable until manual cancellation?
- Should `Other` input be capped by length at spec level (for example 200 chars)?
- Do we need internationalized button labels in this change or defer to existing i18n pipeline?
