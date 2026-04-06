---
name: phone_emulation
description: "Use Lua scripts to automate Android phone operations via accessibility service - get screen content, wait for events, perform clicks/swipes, and control apps like a human user."
---

# Phone Emulation Skill

Control Android phones programmatically through Lua scripts. The `emu` object is automatically injected into the Lua context, providing APIs for screen reading, gesture execution, and app control.

## Tool: PhoneEmulationTool

Execute Lua scripts on a connected Android phone running PhoneClaw.

**Prerequisites:**
1. PhoneClaw app installed and running on Android device
2. Accessibility service enabled
3. Script server started (port 8765)
4. Network connection established (same WiFi or adb forward)

**Setup:**
```bash
# Option A: Same WiFi network
# Get phone IP from PhoneClaw app UI, e.g., 192.168.1.100:8765

# Option B: ADB port forwarding
adb forward tcp:8765 tcp:8765
# Then use localhost:8765
```

**Available Tool Methods:**

| Method | Description |
|--------|-------------|
| `executeScript(script)` | Execute any Lua script |
| `getCurrentScreen(maxDepth)` | Get current UI structure |
| `openApp(packageName)` | Open an app by package name |
| `tap(x, y)` | Tap at coordinates |
| `swipe(fromX, fromY, toX, toY, durationMs)` | Perform swipe gesture |
| `inputText(resourceId, text)` | Input text into a field |
| `findElements(pattern, clickableOnly, maxDepth)` | Find UI elements |
| `pressBack()` | Press back button |
| `pressHome()` | Press home button |

---

## API Reference

### Navigation & Waiting

#### `emu:openApp(packageName)`

Opens an application by package name.

```lua
emu:openApp("com.tencent.mm")      -- WeChat
emu:openApp("com.xingin.xhs")      -- Xiaohongshu
```

#### `emu:waitWindowOpened(packageName, activityName, timeoutMS)`

Blocks until a specific window opens.

| Param | Required | Description |
|-------|:--------:|-------------|
| packageName | Yes | Target app package name |
| activityName | No | Specific activity (experimental) |
| timeoutMS | Yes | Max wait time in milliseconds |

```lua
local window = emu:waitWindowOpened("com.tencent.mm", nil, 5000)
if window == nil then
    print("Timeout waiting for window")
end
```

#### `emu:waitMS(milliseconds)`

Blocks for specified duration.

```lua
emu:waitMS(2000)  -- Wait 2 seconds
```

#### `emu:back()`

Presses the Back button.

#### `emu:home()`

Presses the Home button.

---

### Screen Analysis

#### `emu:getCurrentWindowByAccessibilityService(maxDepth, windowPackageName, filterPattern, ...)`

Gets current screen UI structure with optional filters.

| Param | Required | Description |
|-------|:--------:|-------------|
| maxDepth | Yes | Traversal depth (use 50 for full) |
| windowPackageName | No | Target window, nil for current |
| filterPattern | No | Regex to match text/ID/desc |
| requireClickable | No | Filter for clickable elements |
| requireLongClickable | No | Filter for long-clickable elements |
| requireScrollable | No | Filter for scrollable elements |
| requireEditable | No | Filter for editable elements |
| requireCheckable | No | Filter for checkable elements |

```lua
-- Get full UI tree
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, nil)
print(ui)

-- Find clickable "登录" buttons
local ui = emu:getCurrentWindowByAccessibilityService(
    50, nil, "登录", true, false, false, false, false
)

if ui ~= nil then
    local nodes = ui:getMatchedNodes()
    if nodes ~= nil and nodes:size() > 0 then
        print("Found " .. nodes:size() .. " buttons")
    end
end
```

---

### Click Operations

#### `emu:clickById(id)`

Clicks element by resource ID.

```lua
emu:clickById("com.tencent.mm:id/login_button")
```

#### `emu:clickByPos(x, y)`

Taps at screen coordinates.

```lua
emu:clickByPos(540.0, 960.0)  -- Screen center
```

#### `emu:longClickById(id, durationMs)`

Long-presses element by ID.

```lua
emu:longClickById("com.example.app:id/item", 500)
```

#### `emu:longClickByPos(x, y, durationMs)`

Long-presses at coordinates.

```lua
emu:longClickByPos(540.0, 960.0, 800)
```

---

### Gesture Operations

#### `emu:swipe(fromX, fromY, toX, toY, durationMs)`

Performs swipe gesture.

```lua
-- Scroll down
emu:swipe(540.0, 1500.0, 540.0, 500.0, 300)

-- Scroll up
emu:swipe(540.0, 500.0, 540.0, 1500.0, 300)

-- Swipe left (next page)
emu:swipe(1000.0, 960.0, 100.0, 960.0, 300)
```

---

### Text Input

#### `emu:inputTextById(id, text)`

Inputs text into an editable field by resource ID.

| Param | Type | Required | Description |
|-------|------|:--------:|-------------|
| id | string | Yes | Full resource ID of target editable field |
| text | string | Yes | Text to input |

**Returns:** `Boolean`

```lua
local success = emu:inputTextById("com.example.app:id/search_input", "Hello World")
if success then
    print("Text input successful")
else
    print("Failed - element not found or not editable")
end
```

#### `emu:inputTextByPos(x, y, text)`

Inputs text at screen coordinates by clicking to focus then entering text.

```lua
local success = emu:inputTextByPos(540.0, 800.0, "Hello World")
if not success then
    print("Failed to input text at position")
end
```

---

## Data Structures

### UIWindow

```lua
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, nil)

ui:getPackageName()      -- Returns package name
ui:getRoot()             -- Returns UITree root node
ui:getMatchedNodes()     -- Returns List<UITree> when filter used
print(ui)                -- Formatted output
```

### UITree

```lua
local node = matchedNodes:get(0)

node:getId()             -- Resource ID
node:getClassName()      -- Widget class
node:getText()           -- Text content
node:getDesc()           -- Content description
node:getBounds()         -- Screen bounds (Rect)
node:isClickable()       -- Is clickable
node:isScrollable()      -- Is scrollable
node:getChildren()       -- Child nodes

print(node)              -- Single-line format
print(node:toStringTree())  -- Tree format with children
```

---

## Common Patterns

### Find and Click

```lua
local ui = emu:getCurrentWindowByAccessibilityService(
    50, nil, "确定", true, false, false, false, false
)

if ui ~= nil then
    local nodes = ui:getMatchedNodes()
    if nodes ~= nil and nodes:size() > 0 then
        local node = nodes:get(0)
        local bounds = node:getBounds()
        local x = bounds.left + (bounds.right - bounds.left) / 2
        local y = bounds.top + (bounds.bottom - bounds.top) / 2
        emu:clickByPos(x, y)
    end
end
```

### Scroll Until Found

```lua
local function scrollToFind(pattern, maxScrolls)
    for i = 1, maxScrolls do
        local ui = emu:getCurrentWindowByAccessibilityService(50, nil, pattern)
        if ui ~= nil then
            local nodes = ui:getMatchedNodes()
            if nodes ~= nil and nodes:size() > 0 then
                return nodes:get(0)
            end
        end
        emu:swipe(540.0, 1500.0, 540.0, 500.0, 300)
        emu:waitMS(500)
    end
    return nil
end
```

### Open App and Wait

```lua
emu:openApp("com.tencent.mm")
local window = emu:waitWindowOpened("com.tencent.mm", nil, 5000)
if window == nil then
    return "failed: timeout"
end
emu:waitMS(2000)  -- Wait for content to load
```

---

## Best Practices

1. **Always check for nil** before accessing object methods
2. **Add delays** between operations for animations (300-500ms typical)
3. **Use appropriate maxDepth** - 50 for full, lower for faster search
4. **Prefer clickById** over clickByPos when ID is known and unique
5. **Use interaction filters** to narrow down search results
6. **Print debug info** during development
7. **Progressive** use, observe, modify, observe, modify

---

## Error Handling

```lua
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, "test")
if ui == nil then
    print("Error: Could not get UI tree")
    return "failed"
end

local nodes = ui:getMatchedNodes()
if nodes == nil or nodes:size() == 0 then
    print("No matching nodes found")
    return "not_found"
end
```
