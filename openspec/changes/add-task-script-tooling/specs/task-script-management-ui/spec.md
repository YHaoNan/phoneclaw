## ADDED Requirements

### Requirement: Script center navigation entry
The UI SHALL provide a script management entry in the main page sidebar.

#### Scenario: Sidebar access
- **WHEN** user opens the main application view
- **THEN** a sidebar item for script management is visible
- **AND** user can navigate to script center by selecting it

### Requirement: Script list and detail visibility
The UI SHALL allow users to browse scripts, inspect metadata, and open read-only script code detail.

#### Scenario: Script list fields
- **WHEN** user enters script center
- **THEN** the list shows script name, summary, and created time for each item

#### Scenario: Read-only code detail
- **WHEN** user opens a script detail page
- **THEN** the script code is displayed in read-only mode

### Requirement: Script CRUD operations
The UI SHALL support create, update, and delete operations for scripts.

#### Scenario: Create or edit from script center
- **WHEN** user submits create or edit action in script center
- **THEN** the system persists the script changes and refreshes list state

#### Scenario: Delete from script center
- **WHEN** user confirms delete action
- **THEN** the script is removed and no longer appears in list view
