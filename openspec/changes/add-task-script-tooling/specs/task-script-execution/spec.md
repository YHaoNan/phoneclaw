## ADDED Requirements

### Requirement: Tool-driven script discovery
The system SHALL expose a tool that returns available scripts for model decision-making without prompt list injection.

#### Scenario: List scripts via tool
- **WHEN** the agent calls the script listing tool
- **THEN** the tool returns script identifiers and metadata needed for script selection

### Requirement: Safe script execution
The system SHALL execute saved task scripts through a dedicated execution tool with strict validation and fail-fast behavior.

#### Scenario: Execute matched script successfully
- **WHEN** the agent calls the execute-script tool with a valid script identifier
- **THEN** the system runs script steps in order
- **AND** returns successful execution result

#### Scenario: Precondition mismatch
- **WHEN** any script step precondition fails during execution
- **THEN** the system terminates execution immediately
- **AND** returns an explicit error result

#### Scenario: Runtime action failure
- **WHEN** a script action fails at runtime
- **THEN** the system stops remaining steps
- **AND** does not perform heuristic random clicks
