## Why

Agent workflows frequently encounter ambiguity that requires explicit human decisions. We need a first-class way for the agent to pause, ask a focused question, and continue safely based on a confirmed user answer.

## What Changes

- Add a new `AskUser` tool contract so the agent can ask a question with predefined options and a free-text `Other` path.
- Add interaction rules that require explicit confirmation before a selection is committed.
- Enforce option-count limits for consistent UX and predictable tool behavior.
- Integrate `AskUser` into the existing tool-calling flow so execution continues with structured confirmed answer data.

## Capabilities

### New Capabilities
- `ask-user-question`: Define requirements for asking users structured questions with selectable options, optional free-text fallback, and confirmation semantics.

### Modified Capabilities
- `agent-execution`: Extend execution requirements so `AskUser` is handled as a normal tool call and returns selected/entered confirmed answer data.

## Impact

- Agent tool invocation and validation layer (new tool schema and guardrails).
- Execution orchestration/state machine (pause/resume behavior around human input).
- Chat/UI interaction surfaces (question modal, confirm action, option + other input).
- Telemetry/logging for user-decision checkpoints.
