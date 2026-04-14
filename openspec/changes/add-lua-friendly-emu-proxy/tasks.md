## 1. Proxy and Converter Skeleton

- [x] 1.1 Add `LuaFriendlyEmuFacadeProxy` in the `emu` layer that composes `EmuFacade` and defines script-exposed forwarding methods.
- [x] 1.2 Add `LuaValueConverter` in the `emu` layer to centralize conversion of primitives, collections, maps, and domain objects.
- [x] 1.3 Define and document stable converted field contracts for `AppInfo`, `UIWindow`, and `UITree` structures.

## 2. Return Value Conversion Implementation

- [x] 2.1 Implement deterministic conversion for `List`/array outputs to Lua array-like table representations.
- [x] 2.2 Implement object-to-table conversion for emulator domain entities, including nested children for UI tree nodes.
- [x] 2.3 Implement null-to-nil compatible handling for optional return values and optional fields.

## 3. Lua Runtime Integration

- [x] 3.1 Update Lua tool runtime/binding path in agent execution to inject `LuaFriendlyEmuFacadeProxy` as `emu`.
- [x] 3.2 Preserve existing operation semantics by keeping underlying execution delegated to `EmuFacade`.
- [x] 3.3 Ensure non-Lua code paths still use existing `EmuFacade` behavior unchanged.

## 4. Validation and Regression Coverage

- [x] 4.1 Add unit tests for `LuaValueConverter` covering primitives, lists, nulls, and nested UI object conversion.
- [x] 4.2 Add method-coverage test(s) to verify script-exposed proxy operations remain aligned with expected facade behavior.
- [x] 4.3 Add regression test(s) for representative Lua scripts (app listing, app open, UI query) validating Lua-friendly access patterns.

## 5. Rollout Safety

- [x] 5.1 Add lightweight diagnostics/logging for conversion failures during Lua execution.
- [x] 5.2 Document rollback procedure to revert Lua binding from proxy back to raw `EmuFacade` if needed.
