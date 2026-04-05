# Emu Package Design

Date: 2026-04-05

## Overview

The emu package provides UI automation capabilities for Android, including accessibility-based screen reading, gesture execution, and window monitoring.

## Architecture

### Component Structure

```
emu/
├── EmuApi.java              # Unified API entry point
├── EmuAccessibilityService.java  # Core accessibility service (with gesture)
├── EmuVLMService.java       # VLM interface (stub)
├── EmuGestureService.java   # Gesture thin wrapper
├── UIWindow.java            # Window information object
└── UITree.java              # UI tree node object
```

### Service Architecture

**Single Service Integration Pattern**:
- EmuAccessibilityService contains all core capabilities (UI reading, gestures, window polling)
- EmuGestureService is a thin wrapper that delegates to EmuAccessibilityService
- EmuVLMService is a stub interface for future VLM integration

## Components

### 1. EmuAccessibilityService

**Responsibilities**:
- UI tree traversal and reading
- Gesture execution via dispatchGesture API
- Window polling and monitoring
- Single instance access pattern

**Key Features**:
- Uses active polling (not event-based) for waitWindowOpened
- Supports package name + activity name matching
- Implements keep-alive mechanism

**Implementation Details**:
```java
public class EmuAccessibilityService extends AccessibilityService {
    private static EmuAccessibilityService instance;
    
    public static EmuAccessibilityService getInstance() { ... }
    
    // Window polling
    public AccessibilityWindowInfo findWindow(String packageName, String activityName);
    
    // UI tree traversal
    public UITree buildUITree(AccessibilityNodeInfo node, int maxDepth);
    
    // Gesture execution
    public boolean performGesture(GestureDescription gesture);
}
```

### 2. EmuVLMService

**Responsibilities**:
- Interface definition for visual language model integration
- Currently returns UnsupportedOperationException

**Implementation**:
```java
public class EmuVLMService {
    public UIWindow analyzeScreen(String hintPrompt) {
        throw new UnsupportedOperationException("VLM not implemented");
    }
}
```

### 3. EmuGestureService

**Responsibilities**:
- Expose gesture operations as simple API
- Delegate to EmuAccessibilityService

**Implementation**:
```java
public class EmuGestureService {
    private EmuAccessibilityService accessibilityService;
    
    public boolean click(int x, int y);
    public boolean longClick(int x, int y, long durationMs);
    public boolean swipe(int x1, int y1, int x2, int y2, long durationMs);
}
```

### 4. EmuApi

**Responsibilities**:
- Unified entry point for all operations
- Coordinate between services
- Expose blocking APIs

**API Methods**:

```java
// Wait APIs
public AccessibilityWindowInfo waitWindowOpened(String packageName, String activityName, long timeoutMS);
public void waitMS(long timeoutMS);

// Screen Reading APIs
public UIWindow getCurrentWindowByAccessibilityService(int maxDepth, String windowPackageName, String filterPattern);
public UIWindow getCurrentWindowByVLM(String hintPrompt);

// Operation APIs
public boolean clickById(String viewId);
public boolean clickByPos(double x, double y);
public boolean longClickById(String viewId, long durationMs);
public boolean longClickByPos(double x, double y, long durationMs);
public boolean swipe(double fromX, double fromY, double toX, double toY, long durationMs);
public boolean openApp(String packageName);
public boolean back();
public boolean home();
```

## Data Structures

### UITree

Represents a UI component node in the view hierarchy.

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| id | String | View resource ID |
| className | String | Widget class name |
| text | String | Text content |
| desc | String | Content description |
| hintText | String | Hint text |
| bounds | Rect | Screen coordinates |
| clickable | boolean | Can be clicked |
| longClickable | boolean | Can be long-clicked |
| scrollable | boolean | Can be scrolled |
| editable | boolean | Can be edited |
| checkable | boolean | Can be checked |
| checked | boolean | Current check state |
| children | List<UITree> | Child nodes |

### UIWindow

Represents a window and its UI content.

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| windowInfo | AccessibilityWindowInfo | Window reference |
| packageName | String | Window package name |
| activityName | String | Activity name (if available) |
| root | UITree | UI tree root node |
| matchedNodes | List<UITree> | Matched nodes (when using filterPattern) |

## Wait Implementation

### waitWindowOpened

Uses **active polling** instead of event-based listening to avoid missed events causing permanent suspension.

**Algorithm**:
1. Poll every 200ms
2. Get all windows via `getWindows()`
3. Match by package name AND activity name (if provided)
4. Return matched window or timeout

**Rationale**:
- Event-based approach can miss events if listener not ready
- Active polling is more reliable for automation use cases

## Gesture Implementation

Uses AccessibilityService's `dispatchGesture()` API (requires API 24+).

**Gesture Operations**:
- Click: Single point touch for 100ms
- Long click: Single point touch for configurable duration
- Swipe: Path from point A to B with configurable duration

## AndroidManifest Configuration

### Service Declaration

```xml
<service 
    android:name=".emu.EmuAccessibilityService"
    android:label="PhoneClaw Emulation"
    android:enabled="true"
    android:exported="false"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/emu_accessibility_service_config" />
</service>
```

### Accessibility Service Config

```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagReportViewIds|flagIncludeNotImportantViews|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true"
    android:description="@string/emu_service_description"
    android:notificationTimeout="100"
/>
```

### Required Permissions (already declared)

- `SYSTEM_ALERT_WINDOW` - For overlay features
- `WRITE_SECURE_SETTINGS` - For keep-alive auto-enable

## Migration Plan

### Changes Required

1. **Delete**: `SelectToSpeakService.java`
2. **Update**: `MainActivity.kt` - Change service class reference in:
   - `isAccessibilityServiceEnabled()`
   - `tryAutoEnableAccessibility()`
3. **Update**: `EmuOperationTool.java` - Use EmuApi instead of SelectToSpeakService
4. **Update**: `AndroidManifest.xml` - Remove old service, add new service
5. **Delete**: `res/xml/accessibility_service_config.xml` (replaced by emu version)

## Blocking Behavior

All blocking operations block the caller thread:
- `waitWindowOpened()` - Blocks until window found or timeout
- `waitMS()` - Blocks for specified duration

## Error Handling

- Service not available: Return null/false, log error
- Gesture failure: Return false
- Window not found: Return null after timeout
- VLM: Throw UnsupportedOperationException
