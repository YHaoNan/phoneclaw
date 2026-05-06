## 1. Script Domain and Persistence

- [x] 1.1 Define task script domain model and repository interfaces (id, name, summary, createdTime, codeContent).
- [x] 1.2 Implement persistent storage and mapping layer for script CRUD operations.
- [x] 1.3 Add validation rules for required fields and immutable createdTime behavior.

## 2. Tooling for Script Discovery and Execution

- [x] 2.1 Implement script listing tool contract and adapter for agent consumption.
- [x] 2.2 Implement script execution tool contract with ordered step replay.
- [x] 2.3 Add fail-fast runtime checks (preconditions, action failures, deterministic stop with explicit error output).

## 3. Agent Orchestration Integration

- [x] 3.1 Register new script tools in agent executor tool set.
- [x] 3.2 Extend run-time decision flow to prefer matched scripts before exploration.
- [x] 3.3 Implement fallback/error policy between script path and exploration path, including callback propagation.

## 4. Script Management UI

- [x] 4.1 Add sidebar navigation entry for script center.
- [x] 4.2 Build script list page showing name, summary, and created time.
- [x] 4.3 Build read-only script detail/code view.
- [x] 4.4 Implement create/edit/delete interactions and list refresh behavior.

## 5. Quality and Verification

- [x] 5.1 Add unit tests for repository CRUD and validation behavior.
- [x] 5.2 Add integration tests for list/execute tools and fail-fast execution semantics.
- [x] 5.3 Add UI tests for sidebar entry, list rendering, and detail read-only behavior.
- [x] 5.4 Add end-to-end scenario test for repeat task flow: first exploration then script reuse.
