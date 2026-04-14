## Why

PhoneClaw currently injects `EmuFacade` into Lua scripts as raw Java/Kotlin objects, but many returned values and object access patterns are not idiomatic for Lua (for example list indexing/length and Java-style getter usage). This mismatch causes fragile scripts and frequent agent errors, so we need a compatibility layer that preserves existing emulator capabilities while exposing Lua-friendly data and access semantics.

## What Changes

- Introduce a Lua-friendly facade proxy in the `emu` module that forwards all `EmuFacade` operations and normalizes return values for Lua script consumption.
- Add conversion rules for common return shapes (collections, domain objects, nullable values) so scripts can use predictable Lua table-style access patterns.
- Define clear mapping behavior for `UIWindow`, `UITree`, `AppInfo`, and related structures to improve single-turn script robustness.
- Keep existing `EmuFacade` behavior intact for non-Lua callers and route Lua runtime integration through the new proxy to avoid breaking existing integrations.

## Capabilities

### New Capabilities
- `lua-friendly-emu-facade`: Provide a Lua-oriented proxy API over emulator operations so scripts can interact with emulator data using Lua-idiomatic structures and property access.

### Modified Capabilities
- `agent-execution`: Lua-based tool execution must use the proxy surface when interacting with emulator APIs, ensuring consistent script behavior for agent runs.

## Impact

- Affected code: `emu` domain/integration around `EmuFacade`, Lua runtime binding/injection path, and agent execution pipeline that prepares Lua tool context.
- APIs: Lua script-facing emulator API contract becomes proxy-based (backward-compatible at behavior level, improved at usability level).
- Systems: Script generation and execution reliability improves for automation tasks that inspect app lists, windows, and UI trees.
