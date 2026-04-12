## ADDED Requirements

### Requirement: MCP server provides executeScript tool

The system SHALL provide an MCP tool named `executeScript` that executes Lua scripts on a connected PhoneClaw device.

#### Scenario: Execute script successfully
- **WHEN** the tool is called with a valid Lua script and ScriptServer is running
- **THEN** the script is executed on the device and the result is returned

#### Scenario: Script execution fails
- **WHEN** the script contains syntax errors or runtime errors
- **THEN** the tool returns an error message describing the failure

#### Scenario: ScriptServer unavailable
- **WHEN** ScriptServer is not running or not reachable
- **THEN** the tool returns a connection error message

### Requirement: Tool returns structured response

The executeScript tool SHALL return a structured response containing:
- `success`: boolean indicating if the script executed successfully
- `logs`: array of log lines from script execution
- `result`: the return value of the script (if successful)
- `error`: error message (if failed)

#### Scenario: Successful response structure
- **WHEN** a script executes successfully
- **THEN** the response contains `success: true`, `logs`, and `result` fields

#### Scenario: Failed response structure
- **WHEN** a script execution fails
- **THEN** the response contains `success: false`, `logs`, and `error` fields

### Requirement: Configurable ScriptServer endpoint

The MCP server SHALL allow configuration of the ScriptServer host and port.

#### Scenario: Default configuration
- **WHEN** no configuration is provided
- **THEN** the server connects to `http://localhost:8765` by default

#### Scenario: Custom configuration
- **WHEN** environment variables `SCRIPT_SERVER_HOST` and `SCRIPT_SERVER_PORT` are set
- **THEN** the server uses the configured endpoint
