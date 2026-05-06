## Why

PhoneClaw currently relies on repeated exploratory interaction for recurring mobile tasks, which increases token usage and completion time. We need a script-based execution path so successful exploration can be reused safely and efficiently for repeat tasks.

## What Changes

- Introduce a two-phase task flow: exploratory execution first, then reusable script execution for repeated requests.
- Add tool-level script operations so the model can list available scripts and execute a selected script without prompt-level script list injection.
- Add a script management entry in the main sidebar with list and detail views.
- Support script metadata management (name, summary, created time) and CRUD operations from the script center.
- Enforce production-script safety requirements: deterministic steps, explicit failure detection, and fail-fast behavior (never continue with unsafe random clicks).

## Capabilities

### New Capabilities
- `task-script-registry`: Manage task automation scripts and metadata with list/query/create/update/delete operations.
- `task-script-execution`: Execute saved scripts through dedicated tools with robust runtime validation and safe failure handling.
- `task-script-management-ui`: Provide a sidebar entry and read-only code detail view for browsing and operating scripts.

### Modified Capabilities
- `agent-execution`: Extend execution decision flow to choose between exploratory execution and script execution when scripts are available.

## Impact

- Affected systems: agent runtime orchestration, tool layer, script storage layer, and frontend navigation/UI.
- Affected APIs: internal tool contracts for script listing/loading/execution and script CRUD endpoints.
- Operational impact: lower repeated-task token cost and faster execution, with stronger automation safety constraints.
