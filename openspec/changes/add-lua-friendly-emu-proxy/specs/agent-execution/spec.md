## ADDED Requirements

### Requirement: Lua execution binds emulator access through Lua-friendly proxy
The agent execution pipeline SHALL inject a Lua-friendly emulator proxy object for Lua tool scripts instead of exposing raw `EmuFacade` objects.

#### Scenario: Lua tool runtime initialization
- **WHEN** the system initializes the Lua runtime for a phone emulation tool call
- **THEN** the runtime binding for `emu` uses `LuaFriendlyEmuFacadeProxy`
- **AND** scripts can access converted return values without Java collection/getter conventions

### Requirement: Lua scripts remain compatible with existing emu operation intent
The agent execution pipeline SHALL preserve functional intent of existing script calls while improving data access ergonomics.

#### Scenario: Existing script operation still works
- **WHEN** a script performs an emulator action such as opening an app and querying UI content
- **THEN** the action execution behavior matches existing semantics
- **AND** returned data is provided in Lua-friendly structures for follow-up logic
