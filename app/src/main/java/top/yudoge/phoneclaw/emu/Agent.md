# Emu API Reference for AI Agents

This document provides a comprehensive guide for using the `emu` Lua API in automation scripts. All examples are written in Lua and can be executed via the PhoneClaw script server.

## Quick Start

```lua
-- Open an app and wait for it to load
emu:openApp("com.tencent.mm")
emu:waitWindowOpened("com.tencent.mm", nil, 5000)
emu:waitMS(2000)

-- Get screen content and print it
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, nil)
print(ui)
```

## API Methods

### Navigation & Waiting

#### `emu:openApp(packageName)`

Opens an application by package name.

```lua
-- Examples
emu:openApp("com.tencent.mm")      -- WeChat
emu:openApp("com.xingin.xhs")      -- Xiaohongshu
emu:openApp("com.ss.android.ugc.aweme")  -- Douyin
```

#### `emu:waitWindowOpened(packageName, activityName, timeoutMS)`

Blocks until a specific window opens. Returns the window object or `nil` on timeout.

```lua
-- Wait for app to open (package only)
local window = emu:waitWindowOpened("com.tencent.mm", nil, 5000)

-- Wait for specific activity
local window = emu:waitWindowOpened("com.tencent.mm", "LauncherUI", 5000)

-- Check result
if window == nil then
    print("Timeout waiting for window")
    return "failed"
end
print("Window opened successfully")
```

#### `emu:waitMS(milliseconds)`

Blocks for specified duration.

```lua
-- Wait 2 seconds for page load
emu:waitMS(2000)

-- Short delay between operations
emu:clickByPos(100, 200)
emu:waitMS(300)
emu:clickByPos(300, 400)
```

#### `emu:back()`

Presses the Back button.

```lua
emu:back()
emu:waitMS(500)  -- Wait for animation
```

#### `emu:home()`

Presses the Home button.

```lua
emu:home()
```

---

### Screen Analysis

#### `emu:getCurrentWindowByAccessibilityService(maxDepth, windowPackageName, filterPattern)`

Gets the current screen UI structure. Returns a `UIWindow` object.

```lua
-- Get complete UI tree
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, nil)
print(ui)

-- Get nodes matching text pattern
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, "登录")
if ui ~= nil then
    local nodes = ui:getMatchedNodes()
    print("Found " .. nodes:size() .. " matching nodes")
end
```

#### `emu:getCurrentWindowByAccessibilityService(maxDepth, windowPackageName, filterPattern, requireClickable, requireLongClickable, requireScrollable, requireEditable, requireCheckable)`

Extended version with interaction capability filters.

```lua
-- Find clickable "提交" buttons
local ui = emu:getCurrentWindowByAccessibilityService(
    50,              -- maxDepth: traversal depth
    nil,             -- windowPackageName: nil = current window
    "提交",          -- filterPattern: regex pattern
    true,            -- requireClickable: must be clickable
    false,           -- requireLongClickable
    false,           -- requireScrollable
    false,           -- requireEditable
    false            -- requireCheckable
)

if ui ~= nil then
    local nodes = ui:getMatchedNodes()
    if nodes ~= nil and nodes:size() > 0 then
        print("Found " .. nodes:size() .. " clickable '提交' buttons")
        for i = 0, nodes:size() - 1 do
            local node = nodes:get(i)
            print(node)
        end
    end
end
```

---

### Click Operations

#### `emu:clickById(id)`

Clicks a node by its resource ID.

```lua
-- Click login button
local success = emu:clickById("com.tencent.mm:id/login_button")
if success then
    print("Clicked successfully")
else
    print("Node not found or click failed")
end
```

#### `emu:clickByPos(x, y)`

Taps at screen coordinates.

```lua
-- Tap at screen position
emu:clickByPos(540.0, 960.0)

-- Tap at node center
local bounds = node:getBounds()
local centerX = bounds.left + (bounds.right - bounds.left) / 2
local centerY = bounds.top + (bounds.bottom - bounds.top) / 2
emu:clickByPos(centerX, centerY)
```

#### `emu:longClickById(id, durationMs)`

Long-presses a node by ID.

```lua
-- Long press for 500ms
emu:longClickById("com.example.app:id/message", 500)
```

#### `emu:longClickByPos(x, y, durationMs)`

Long-presses at screen coordinates.

```lua
-- Long press at position for 800ms
emu:longClickByPos(540.0, 960.0, 800)
```

---

### Gesture Operations

#### `emu:swipe(fromX, fromY, toX, toY, durationMs)`

Performs a swipe gesture.

```lua
-- Scroll down (swipe up)
emu:swipe(540.0, 1500.0, 540.0, 500.0, 300)

-- Scroll up (swipe down)
emu:swipe(540.0, 500.0, 540.0, 1500.0, 300)

-- Swipe left (next page)
emu:swipe(1000.0, 960.0, 100.0, 960.0, 300)

-- Swipe right (previous page)
emu:swipe(100.0, 960.0, 1000.0, 960.0, 300)
```

---

## Data Structures

### UIWindow

Represents a window and its UI content.

**Methods:**
- `getPackageName()` - Returns package name string
- `getRoot()` - Returns `UITree` root node (when no filter)
- `getMatchedNodes()` - Returns `List<UITree>` of matched nodes (when filter used)
- `toString()` - Returns formatted string representation

```lua
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, nil)
print("Package: " .. ui:getPackageName())

-- Access root tree
local root = ui:getRoot()
if root ~= nil then
    print(root:toStringTree())  -- Print entire tree
end

-- Access matched nodes
local matched = ui:getMatchedNodes()
if matched ~= nil then
    print("Matched: " .. matched:size() .. " nodes")
end
```

### UITree

Represents a UI node in the view hierarchy.

**Property Methods:**
- `getId()` - Resource ID (string or nil)
- `getClassName()` - Widget class name (string or nil)
- `getText()` - Text content (string or nil)
- `getDesc()` - Content description (string or nil)
- `getHintText()` - Hint text (string or nil)
- `getBounds()` - Screen bounds (Rect or nil)
- `isClickable()` - Is clickable (boolean)
- `isLongClickable()` - Is long-clickable (boolean)
- `isScrollable()` - Is scrollable (boolean)
- `isEditable()` - Is editable (boolean)
- `isCheckable()` - Is checkable (boolean)
- `isChecked()` - Is checked (boolean)
- `getChildren()` - Child nodes (List<UITree> or nil)

**Formatting Methods:**
- `toString()` - Single-line representation
- `toStringTree()` - Tree representation with children

```lua
local node = matchedNodes:get(0)

-- Print node info
print("ID: " .. (node:getId() or "none"))
print("Text: " .. (node:getText() or "none"))
print("Clickable: " .. tostring(node:isClickable()))

-- Get bounds
local bounds = node:getBounds()
if bounds ~= nil then
    print(string.format("Position: (%d, %d)", bounds.left, bounds.top))
    print(string.format("Size: %d x %d", bounds.right - bounds.left, bounds.bottom - bounds.top))
end

-- Access children
local children = node:getChildren()
if children ~= nil then
    print("Children: " .. children:size())
end

-- Print tree structure
print(node:toStringTree())
```

### Rect

Android Rect object with bounds properties.

**Properties:**
- `left`, `top` - Top-left corner coordinates
- `right`, `bottom` - Bottom-right corner coordinates

**Computed values in Lua:**
```lua
local width = bounds.right - bounds.left
local height = bounds.bottom - bounds.top
local centerX = bounds.left + width / 2
local centerY = bounds.top + height / 2
```

---

## Common Patterns

### Pattern: Find and Click

```lua
-- Find clickable button with specific text
local ui = emu:getCurrentWindowByAccessibilityService(
    50, nil, "确定", true, false, false, false, false
)

if ui ~= nil then
    local nodes = ui:getMatchedNodes()
    if nodes ~= nil and nodes:size() > 0 then
        -- Click first match
        local node = nodes:get(0)
        local bounds = node:getBounds()
        local x = bounds.left + (bounds.right - bounds.left) / 2
        local y = bounds.top + (bounds.bottom - bounds.top) / 2
        emu:clickByPos(x, y)
        print("Clicked '确定' button")
    else
        print("No '确定' button found")
    end
end
```

### Pattern: Scroll Until Found

```lua
local function scrollToFind(pattern, maxScrolls)
    for i = 1, maxScrolls do
        local ui = emu:getCurrentWindowByAccessibilityService(50, nil, pattern)
        if ui ~= nil then
            local nodes = ui:getMatchedNodes()
            if nodes ~= nil and nodes:size() > 0 then
                print("Found after " .. i .. " scrolls")
                return nodes:get(0)
            end
        end
        
        -- Scroll down
        emu:swipe(540.0, 1500.0, 540.0, 500.0, 300)
        emu:waitMS(500)
    end
    return nil
end

local node = scrollToFind("加载更多", 5)
if node ~= nil then
    print("Found: " .. node:getText())
end
```

### Pattern: Wait and Retry

```lua
local function waitForElement(pattern, timeout, interval)
    local startTime = os.clock() * 1000
    while (os.clock() * 1000) - startTime < timeout do
        local ui = emu:getCurrentWindowByAccessibilityService(50, nil, pattern)
        if ui ~= nil then
            local nodes = ui:getMatchedNodes()
            if nodes ~= nil and nodes:size() > 0 then
                return nodes:get(0)
            end
        end
        emu:waitMS(interval)
    end
    return nil
end

local element = waitForElement("登录成功", 10000, 500)
if element ~= nil then
    print("Login successful!")
end
```

### Pattern: Input Text (Workaround)

```lua
-- Click input field to focus
emu:clickById("com.example.app:id/edit_text")
emu:waitMS(300)

-- Simulate typing by finding focused field
local ui = emu:getCurrentWindowByAccessibilityService(10, nil, nil)
-- Note: Actual text input requires additional implementation
```

---

## Error Handling

```lua
-- Always check for nil returns
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, "test")
if ui == nil then
    print("Error: Could not get UI tree")
    return "failed"
end

-- Check matched nodes
local nodes = ui:getMatchedNodes()
if nodes == nil or nodes:size() == 0 then
    print("No matching nodes found")
    return "not_found"
end

-- Check operation results
local success = emu:clickById("com.example:id/button")
if not success then
    print("Click failed")
end
```

---

## Debugging Tips

```lua
-- Print entire UI tree
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, nil)
if ui ~= nil then
    print("=== UI Structure ===")
    print(ui)
end

-- Print specific node details
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, "关键词")
if ui ~= nil then
    local nodes = ui:getMatchedNodes()
    if nodes ~= nil then
        for i = 0, nodes:size() - 1 do
            local node = nodes:get(i)
            print("--- Node " .. (i+1) .. " ---")
            print("  ID: " .. (node:getId() or "none"))
            print("  Class: " .. (node:getClassName() or "none"))
            print("  Text: " .. (node:getText() or "none"))
            print("  Clickable: " .. tostring(node:isClickable()))
            if node:getBounds() ~= nil then
                local b = node:getBounds()
                print(string.format("  Bounds: [%d,%d %d,%d]", b.left, b.top, b.right, b.bottom))
            end
        end
    end
end
```

---

## Best Practices

1. **Always check for nil** before accessing object methods
2. **Use appropriate maxDepth** - Start with 50, increase only if needed
3. **Add delays** between operations for animations to complete
4. **Handle timeouts gracefully** - Users may have slow devices
5. **Prefer clickById over clickByPos** when ID is known (more reliable)
6. **Use interaction filters** to narrow down search results
7. **Print debug info** during development, remove in production

---

## Limitations

- **Text Input**: Not directly supported; requires workaround
- **Scroll Detection**: Cannot detect scroll completion automatically
- **WebView Content**: Limited visibility into web content
- **Secure Screens**: Cannot access content on secure screens (banking apps, etc.)
- **System UI**: Some system elements may not be accessible
