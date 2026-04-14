## Context

`EmuFacade` is currently exposed directly to Lua scripts. Because Lua interacts naturally with tables and direct field access, the Java-style object model (e.g., `List.size()`, `List.get(i)`, getter-centric objects) creates a high-friction interface for agent-generated scripts. This has already caused scripting mistakes in automation scenarios, especially when iterating lists and reading `UIWindow`/`UITree` payloads.

Per the PhoneClaw layering design, this change must keep responsibilities clear: emulator domain/integration logic remains in `emu`, agent orchestration remains in `llm`, and UI is not involved.

## Goals / Non-Goals

**Goals:**
- Provide a Lua-friendly proxy surface that forwards `EmuFacade` calls without removing existing `EmuFacade`.
- Normalize return values into Lua-idiomatic structures (tables, scalar values, nested maps/arrays) for script-side access.
- Keep conversion deterministic so agent prompts can rely on stable field names and data shapes.
- Keep compatibility for existing non-Lua code paths.

**Non-Goals:**
- Rewriting all emulator internals or changing underlying accessibility/screen-reading behavior.
- Introducing breaking changes to existing Java/Kotlin `EmuFacade` callers.
- Designing a generic serializer for arbitrary object graphs outside emulator-facing data.

## Decisions

1. Add `LuaFriendlyEmuFacadeProxy` in `emu` module as a composition wrapper around `EmuFacade`.
- Rationale: preserves existing facade and keeps Lua adaptation logic isolated.
- Alternative considered: patch `EmuFacade` methods directly to return Lua-friendly values. Rejected to avoid coupling Java callers to Lua concerns.

2. Add a dedicated converter component (`LuaValueConverter`) responsible for mapping return values.
- Conversion rules:
  - Java/Kotlin primitive wrappers and strings: return as-is.
  - Collections/arrays: convert to ordered Lua array-like tables (1-based index semantics in emitted Lua object).
  - Maps/POJOs/domain objects: convert to key-value tables with stable snake_case or existing field names (prefer current domain naming consistency).
  - `null`: expose as Lua `nil` equivalent via bridge contract.
- Rationale: central place for consistency and testability.
- Alternative considered: ad-hoc conversion inside each proxy method. Rejected due to duplication and drift risk.

3. Scope conversion for core emulator domain objects first (`AppInfo`, `UIWindow`, `UITree`, nested node structures), with explicit field contracts.
- Rationale: these are high-frequency objects in agent scripts and the main source of hallucinated API usage.

4. Bind Lua runtime to proxy object instead of raw `EmuFacade` during tool setup in agent execution path.
- Rationale: behavior change is required where Lua scripts are prepared, while maintaining broader architecture boundaries.

## Risks / Trade-offs

- [Risk] Conversion schema drift when domain objects evolve.
  - Mitigation: add unit tests that assert exact converted keys for key objects (`AppInfo`, `UIWindow`, `UITree`).
- [Risk] Extra conversion overhead for large UI trees.
  - Mitigation: keep conversion iterative and avoid unnecessary reflection where possible; benchmark on representative trees.
- [Risk] Incomplete proxy coverage can leave methods unadapted.
  - Mitigation: enforce method parity checks/tests between proxy and facade for script-exposed APIs.

## Migration Plan

1. Implement proxy and converter in `emu` layer.
2. Update Lua tool binding path to inject proxy.
3. Add compatibility tests and regression tests for representative Lua scripts.
4. Keep fallback toggle (if available in runtime wiring) for quick rollback to raw facade during early validation.

Rollback: switch Lua binding back to original `EmuFacade` and disable proxy wiring if runtime issues are detected.

## Open Questions

- Should converted field naming preserve existing camelCase exactly, or normalize to a Lua style (for example snake_case)?
- Should we expose both raw and friendly representations for debugging (`raw`, `friendly`) in early rollout?
- Do we need a size cap or truncated mode for very large UI trees passed to Lua?
