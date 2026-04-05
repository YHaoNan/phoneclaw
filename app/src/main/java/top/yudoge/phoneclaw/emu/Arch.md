# Emu Package Architecture

## Overview

The `emu` package provides a comprehensive UI automation framework for Android applications. It abstracts the complexity of Android's Accessibility Service into a clean, Lua-scriptable API for automated testing, RPA (Robotic Process Automation), and accessibility-driven automation tasks.

## Design Philosophy

### 1. Single Service Integration

The architecture follows a **unified service model** where `EmuAccessibilityService` serves as the central provider for all automation capabilities:

- **UI Reading**: Tree traversal and node analysis
- **Gesture Execution**: Touch simulation via `dispatchGesture()` API
- **Window Monitoring**: Active polling for window state changes
- **Global Actions**: Back, Home, Recents, etc.

**Rationale**: Android's Accessibility Service APIs (especially `dispatchGesture()`) are tightly coupled with the service lifecycle. Separating these into multiple services would introduce unnecessary complexity and inter-service communication overhead without meaningful benefits.

### 2. Blocking API Design

All methods in `EmuApi` are **synchronous and blocking**:

```java
// Blocks until window appears or timeout
AccessibilityWindowInfo window = emu.waitWindowOpened("com.example.app", null, 5000);

// Blocks for specified duration
emu.waitMS(2000);
```

**Rationale**: 
- Lua scripts execute in a single-threaded context
- Blocking semantics are intuitive for sequential automation tasks
- The script server already handles request dispatch on worker threads
- No need for complex async/callback patterns in Lua

### 3. Layered Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Lua Script                           │
│              emu:openApp("com.example.app")             │
└─────────────────────┬───────────────────────────────────┘
                      │ LuaJ Coercion
┌─────────────────────▼───────────────────────────────────┐
│                     EmuApi                              │
│          Unified API Entry Point                        │
│  - Parameter validation                                 │
│  - Service coordination                                 │
│  - Result conversion                                    │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│              EmuAccessibilityService                    │
│          Core Service Implementation                    │
│  - AccessibilityNodeInfo traversal                      │
│  - GestureDescription dispatch                          │
│  - Window polling                                       │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│            Android Accessibility Framework              │
└─────────────────────────────────────────────────────────┘
```

### 4. Thin Wrapper Pattern

`EmuGestureService` and `EmuVLMService` are designed as **thin wrappers**:

```java
// EmuGestureService simply delegates to EmuAccessibilityService
public boolean click(int x, int y) {
    EmuAccessibilityService service = EmuAccessibilityService.getInstance();
    if (service == null) return false;
    return service.performGestureClick(x, y, 100);
}
```

**Rationale**:
- Maintains semantic separation for future extensibility
- `EmuVLMService` can be swapped with actual VLM implementation without API changes
- `EmuGestureService` provides a focused interface for gesture-only use cases
- Zero runtime overhead (simple method delegation)

## Component Details

### EmuApi

**Responsibility**: High-level API orchestration
- Parameter validation and normalization
- Service instance retrieval and null-checking
- Result object construction
- Cross-cutting concerns (logging, error handling)

**Design Decisions**:
- All methods return `Boolean` (wrapper type) instead of `boolean` for Lua compatibility
- `null` returns indicate service unavailability or not-found conditions
- Throws `IllegalArgumentException` for invalid parameters (fail-fast)

### EmuAccessibilityService

**Responsibility**: Core accessibility integration
- Singleton pattern for global access
- UI tree traversal with depth control
- Node search by ID, text, or pattern
- Gesture synthesis and dispatch
- Window enumeration and matching

**Design Decisions**:
- Active polling (vs event listeners) for `waitWindowOpened`:
  - Event listeners can miss events if not registered at the right time
  - Polling is more predictable and easier to reason about
  - 1000ms poll interval balances responsiveness vs battery

- Node recycling strategy:
  - Callers are responsible for recycling returned nodes
  - Internal methods recycle intermediate nodes
  - Build methods return ownership to caller

### UITree / UIWindow

**Responsibility**: Data transfer objects for UI structure
- Simple POJOs with getters/setters
- `toString()` implementations for debugging
- No dependency on Android Accessibility types (for potential serialization)

**Design Decisions**:
- Use `Rect` directly (Android type) for bounds to avoid allocation overhead
- Store `List<UITree>` for children (vs array) for easier manipulation
- `matchedNodes` list in `UIWindow` enables flat search results

### EmuVLMService

**Responsibility**: Visual Language Model integration (stub)
- Currently throws `UnsupportedOperationException`
- Designed for future VLM-based screen analysis

### EmuGestureService

**Responsibility**: Gesture operation facade
- Provides focused API for touch operations
- Delegates all operations to `EmuAccessibilityService`

## Thread Model

```
HTTP Request Thread (ScriptServer worker)
    │
    ▼
Lua Script Execution (blocking)
    │
    ├── emu:waitWindowOpened()  ──► Blocks with polling loop
    ├── emu:waitMS()            ──► Blocks with Thread.sleep()
    └── emu:clickByPos()        ──► Non-blocking (gesture dispatch)
    │
    ▼
EvalListener.onFinished() callback
    │
    ▼
HTTP Response sent
```

## Error Handling Strategy

1. **Service Unavailable**: Return `null` or `false`
   - Accessibility service not enabled or crashed
   - Check `EmuAccessibilityService.getInstance() != null`

2. **Node Not Found**: Return `null` or `false`
   - Invalid resource ID
   - Node not in current view hierarchy

3. **Invalid Parameters**: Throw `IllegalArgumentException`
   - Negative timeout values
   - Required parameters null/empty

4. **Operation Failed**: Return `false`
   - Gesture dispatch failed
   - Click action rejected by system

## Future Extensibility

### Planned Features

1. **EmuVLMService Implementation**
   - Integration with on-device VLM (e.g., Gemini Nano)
   - Natural language screen queries
   - Fallback to accessibility-based analysis

2. **Additional Gesture Types**
   - Pinch gestures
   - Multi-touch support
   - Custom gesture paths

3. **Enhanced Node Search**
   - XPath-like queries
   - Ancestor/descendant navigation
   - Property-based matching

4. **Event-Driven Mode**
   - Optional event listener registration
   - Non-blocking callbacks for real-time monitoring

### Extension Points

- `InjectionProvider` interface in ScriptServer allows dynamic object injection
- `EmuApi` can be extended with new methods without breaking existing scripts
- `UITree` can be subclassed for specialized node types

## Performance Considerations

1. **UI Tree Traversal**: Use `maxDepth` to limit traversal scope
2. **Pattern Matching**: Pre-compiled `Pattern` objects for repeated use
3. **Node Recycling**: Always recycle `AccessibilityNodeInfo` objects to prevent memory leaks
4. **Polling Interval**: 1000ms default, adjustable based on use case

## Security Model

- Requires `BIND_ACCESSIBILITY_SERVICE` permission (system-granted)
- User must explicitly enable the accessibility service
- No access to secure settings or privileged operations
- Gestures execute with user-level permissions
