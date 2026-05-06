## ADDED Requirements

### Requirement: Script metadata and content lifecycle
The system SHALL provide persistent CRUD operations for task scripts, including name, summary, creation time, and executable code content.

#### Scenario: Create script
- **WHEN** a new task script is saved after successful exploration
- **THEN** the system stores script name, summary, created time, and code content as one record

#### Scenario: List scripts
- **WHEN** script list is requested
- **THEN** the system returns script records with name, summary, and created time sorted by most recent first

#### Scenario: Update script metadata
- **WHEN** user updates script name or summary
- **THEN** the system persists the new values
- **AND** the script created time remains unchanged

#### Scenario: Delete script
- **WHEN** user deletes an existing script
- **THEN** the system removes the script record from registry
