## ADDED Requirements

### Requirement: Lua proxy exposes facade operations with Lua-friendly values
The system SHALL provide a `LuaFriendlyEmuFacadeProxy` that forwards script-exposed emulator operations and returns Lua-friendly values instead of raw Java/Kotlin collection or entity objects.

#### Scenario: Forward facade operation
- **WHEN** a Lua script calls a proxy method that maps to an existing `EmuFacade` API
- **THEN** the proxy delegates to the corresponding `EmuFacade` operation
- **AND** the proxy returns the converted result to Lua

### Requirement: Collection and object conversion is deterministic
The system SHALL convert `List`, arrays, and emulator domain objects into deterministic Lua table structures with stable key names.

#### Scenario: App list conversion
- **WHEN** `getInstalledApps(...)` returns one or more `AppInfo` objects
- **THEN** Lua receives an array-like table of app tables
- **AND** each app table contains stable keys for package and app name fields

#### Scenario: UI tree conversion
- **WHEN** a window or node query returns `UIWindow`/`UITree` data
- **THEN** Lua receives nested table structures representing window and node fields
- **AND** child nodes are represented in an ordered array-like table

### Requirement: Null handling maps to Lua nil semantics
The system SHALL map null return values and missing optional fields to Lua nil-compatible semantics in the script runtime.

#### Scenario: Optional value absent
- **WHEN** the underlying facade returns null for an optional value
- **THEN** the Lua script observes nil for that field or return value
