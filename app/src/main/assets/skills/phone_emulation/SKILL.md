---
name: phone_emulation
description: "Use Lua scripts to automate Android phone operations via accessibility service - get screen content, wait for events, perform clicks/swipes, and control apps like a human user."
---

# Phone Emulation Skill

Control Android phones programmatically through Lua scripts. The `emu` object is automatically injected into the Lua context, providing APIs for screen reading, gesture execution, and app control.

## ⚠️ CRITICAL: Always Check Return Values

**Every API method returns a value indicating success or failure. You MUST check these values before proceeding.**

| Method | Return Value | Failure Condition |
|--------|-------------|-------------------|
| `openApp(pkg)` | `Boolean` | Returns `false` if app not found or service unavailable |
| `clickById(id)` | `Boolean` | Returns `false` if element not found or click failed |
| `clickByPos(x, y)` | `Boolean` | Returns `false` if gesture failed or service unavailable |
| `inputTextById(id, text)` | `Boolean` | Returns `false` if element not found or not editable |
| `waitWindowOpened(...)` | `AccessibilityWindowInfo` or `nil` | Returns `nil` on timeout or service unavailable |
| `getCurrentWindowByAccessibilityService(...)` | `UIWindow` or `nil` | Returns `nil` if service unavailable or no content |
| `getInstalledApps(filter)` | `List<AppInfo>` or `nil` | Returns `nil` if service unavailable |
| `back()`, `home()` | `Boolean` | Returns `false` if action failed |

**Example of proper error handling:**
```lua
-- WRONG: No error checking
emu:openApp("com.tencent.mm")
emu:clickById("com.tencent.mm:id/button")

-- CORRECT: Check return values
local success = emu:openApp("com.tencent.mm")
if not success then
    return "Failed to open app"
end

local clicked = emu:clickById("com.tencent.mm:id/button")
if not clicked then
    return "Failed to click button"
end
```

---

## Tool: PhoneEmulationTool

Execute Lua scripts on a connected Android phone running PhoneClaw.

**Prerequisites:**
1. PhoneClaw app installed and running on Android device
2. Accessibility service enabled
3. Accessibility service enabled (for screen scanning and gestures)

**Single Tool Method:**

| Method | Description |
|--------|-------------|
| `executeScript(script)` | Execute any Lua script. The `emu` object is automatically available in the script context with all phone automation APIs. |

**Usage Example:**
```
// Call the tool with a Lua script
executeScript("emu:openApp('com.tencent.mm')\nemu:waitMS(2000)\nemu:clickById('com.tencent.mm:id/login_button')")
```

All phone operations are performed through the `emu` object in Lua scripts. See the API Reference below for available methods.

---

## API Reference

### Navigation & Waiting

#### `emu:openApp(packageName)`

Opens an application by package name.

**Returns:** `Boolean` - `true` if successful, `false` if app not found or service unavailable.

```lua
local success = emu:openApp("com.tencent.mm")
if not success then
    print("Failed to open app")
    return "failed"
end
```

#### `emu:waitWindowOpened(packageName, activityName, timeoutMS)`

Blocks until a specific window opens.

| Param | Required | Description |
|-------|:--------:|-------------|
| packageName | Yes | Target app package name |
| activityName | No | Specific activity (experimental) |
| timeoutMS | Yes | Max wait time in milliseconds |

**Returns:** `AccessibilityWindowInfo` or `nil` on timeout.

```lua
local window = emu:waitWindowOpened("com.tencent.mm", nil, 5000)
if window == nil then
    print("Timeout waiting for window")
    return "failed: timeout"
end
```

#### `emu:waitMS(milliseconds)`

Blocks for specified duration.

```lua
emu:waitMS(2000)  -- Wait 2 seconds
```

#### `emu:back()`

Presses the Back button.

**Returns:** `Boolean` - `true` if successful, `false` if failed.

#### `emu:home()`

Presses the Home button.

**Returns:** `Boolean` - `true` if successful, `false` if failed.

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

**Returns:** `UIWindow` or `nil` if service unavailable.

```lua
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, nil)
if ui == nil then
    print("Failed to get UI")
    return "failed"
end

-- When using filter, check matchedNodes
local ui = emu:getCurrentWindowByAccessibilityService(
    50, nil, "登录", true, false, false, false, false
)

if ui ~= nil then
    local nodes = ui:getMatchedNodes()
    if nodes == nil or nodes:size() == 0 then
        print("No matching nodes found")
        return "not_found"
    end
end
```

---

### App Management

#### `emu:getInstalledApps(filterPattern)`

Gets list of installed applications with optional regex filtering.

| Param | Required | Description |
|-------|:--------:|-------------|
| filterPattern | No | Regex to filter by package name or app name (case-insensitive) |

**Returns:** `List<AppInfo>` or `nil` if service unavailable.

```lua
-- Get all apps
local apps = emu:getInstalledApps(nil)
if apps == nil then
    print("Failed to get app list")
    return "failed"
end

for i = 0, apps:size() - 1 do
    local app = apps:get(i)
    print(app:getAppName() .. " - " .. app:getPackageName())
end

-- Filter by keyword
local wechatApps = emu:getInstalledApps("tencent")
if wechatApps ~= nil then
    print("Found " .. wechatApps:size() .. " Tencent apps")
end
```

---

### Click Operations

#### `emu:clickById(id)`

Clicks element by resource ID.

**Returns:** `Boolean` - `true` if successful, `false` if element not found or click failed.

```lua
local success = emu:clickById("com.tencent.mm:id/login_button")
if not success then
    print("Click failed or element not found")
end
```

#### `emu:clickByPos(x, y)`

Taps at screen coordinates.

**Returns:** `Boolean` - `true` if successful, `false` if gesture failed.

```lua
local success = emu:clickByPos(540.0, 960.0)
if not success then
    print("Tap failed")
end
```

#### `emu:longClickById(id, durationMs)`

Long-presses element by ID.

**Returns:** `Boolean` - `true` if successful.

```lua
local success = emu:longClickById("com.example.app:id/item", 500)
if not success then
    print("Long click failed")
end
```

#### `emu:longClickByPos(x, y, durationMs)`

Long-presses at coordinates.

**Returns:** `Boolean` - `true` if successful.

```lua
local success = emu:longClickByPos(540.0, 960.0, 800)
```

---

### Gesture Operations

#### `emu:swipe(fromX, fromY, toX, toY, durationMs)`

Performs swipe gesture.

**Returns:** `Boolean` - `true` if successful, `false` if gesture failed.

```lua
local success = emu:swipe(540.0, 1500.0, 540.0, 500.0, 300)
if not success then
    print("Swipe failed")
end
```

---

### Text Input

#### `emu:inputTextById(id, text)`

Inputs text into an editable field by resource ID.

| Param | Type | Required | Description |
|-------|------|:--------:|-------------|
| id | string | Yes | Full resource ID of target editable field |
| text | string | Yes | Text to input |

**Returns:** `Boolean` - `true` if successful, `false` if element not found or not editable.

```lua
local success = emu:inputTextById("com.example.app:id/search_input", "Hello World")
if not success then
    print("Text input failed - element not found or not editable")
end
```

#### `emu:inputTextByPos(x, y, text)`

Inputs text at screen coordinates by clicking to focus then entering text.

**Returns:** `Boolean` - `true` if successful.

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
if ui == nil then
    print("Failed to get window")
    return "failed"
end

ui:getPackageName()      -- Returns package name
ui:getRoot()             -- Returns UITree root node (when no filter)
ui:getMatchedNodes()     -- Returns List<UITree> when filter used
print(ui)                -- Formatted output
```

### UITree

```lua
local nodes = ui:getMatchedNodes()
if nodes == nil or nodes:size() == 0 then
    print("No nodes found")
    return "not_found"
end

local node = nodes:get(0)

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

### AppInfo

```lua
local apps = emu:getInstalledApps(nil)
if apps == nil then
    print("Failed to get apps")
    return "failed"
end

for i = 0, apps:size() - 1 do
    local app = apps:get(i)
    app:getPackageName()  -- Package name (e.g., "com.tencent.mm")
    app:getAppName()      -- Display name (e.g., "微信")
    print(app)            -- Formatted: "微信 (com.tencent.mm)"
end
```

---

## Common Patterns

### Find and Click

```lua
local ui = emu:getCurrentWindowByAccessibilityService(
    50, nil, "确定", true, false, false, false, false
)

if ui == nil then
    return "failed: could not get UI"
end

local nodes = ui:getMatchedNodes()
if nodes == nil or nodes:size() == 0 then
    return "failed: button not found"
end

local node = nodes:get(0)
local bounds = node:getBounds()
local x = bounds.left + (bounds.right - bounds.left) / 2
local y = bounds.top + (bounds.bottom - bounds.top) / 2

local success = emu:clickByPos(x, y)
if not success then
    return "failed: click failed"
end

return "success"
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

local node = scrollToFind("目标文本", 5)
if node == nil then
    return "failed: not found after scrolling"
end
```

### Open App and Wait

```lua
local success = emu:openApp("com.tencent.mm")
if not success then
    return "failed: could not open app"
end

local window = emu:waitWindowOpened("com.tencent.mm", nil, 5000)
if window == nil then
    return "failed: timeout waiting for window"
end
emu:waitMS(2000)  -- Wait for content to load
```

---

## Best Practices

1. **Always check return values** - Every method indicates success/failure
2. **Check for nil before accessing** - Objects, lists, nodes may be nil
3. **Add delays** between operations for animations (300-500ms typical)
4. **Use appropriate maxDepth** - 50 for full, lower for faster search
5. **Prefer clickById** over clickByPos when ID is known and unique
6. **Use interaction filters** to narrow down search results
7. **Print debug info** during development
8. **Progressive approach** - observe, modify, observe, modify

---

## Error Handling

```lua
-- Check service availability
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, "test")
if ui == nil then
    print("Error: Could not get UI tree (service unavailable?)")
    return "failed"
end

-- Check if any results
local nodes = ui:getMatchedNodes()
if nodes == nil or nodes:size() == 0 then
    print("No matching nodes found")
    return "not_found"
end

-- Check operation success
local success = emu:clickById("com.example:id/button")
if not success then
    print("Click operation failed")
    return "failed"
end
```
