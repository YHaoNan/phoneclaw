# PhoneClaw Harness MCP Server

MCP (Model Context Protocol) server for PhoneClaw ScriptServer. Execute Lua scripts on your Android device from Claude Desktop or other MCP-compatible AI tools.

## Prerequisites

1. PhoneClaw app installed on Android device
2. ScriptServer enabled in PhoneClaw settings (port 8765 by default)
3. Device and computer on the same network (or use ADB port forwarding)

## Setup

### 1. Install dependencies

```bash
cd harness-test
npm install
```

### 2. Build the project

```bash
npm run build
```

### 3. Configure ScriptServer connection (optional)

By default, the MCP server connects to `localhost:8765`. Set environment variables to customize:

```bash
export SCRIPT_SERVER_HOST=192.168.1.100  # Your device's IP
export SCRIPT_SERVER_PORT=8765
```

For ADB port forwarding:
```bash
adb forward tcp:8765 tcp:8765
# Then use default localhost:8765
```

## Claude Desktop Configuration

Add to your Claude Desktop config file:

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`  
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "phoneclaw-harness": {
      "command": "node",
      "args": ["/path/to/phoneclaw/harness-test/dist/index.js"]
    }
  }
}
```

With custom ScriptServer host:
```json
{
  "mcpServers": {
    "phoneclaw-harness": {
      "command": "node",
      "args": ["/path/to/phoneclaw/harness-test/dist/index.js"],
      "env": {
        "SCRIPT_SERVER_HOST": "192.168.1.100",
        "SCRIPT_SERVER_PORT": "8765"
      }
    }
  }
}
```

## Usage

Once configured, Claude Desktop will have access to the `executeScript` tool. You can ask Claude to:

- "Open TikTok and show me the messages"
- "Find all clickable elements on the current screen"
- "Swipe up to scroll the page"

## Example Scripts

### Get installed apps
```lua
local apps = emu:getInstalledApps("微信")
if apps == nil then
    print("Failed to get apps")
    return "error"
end
print("Found " .. apps:size() .. " apps")
for i = 0, apps:size() - 1 do
    local app = apps:get(i)
    print(app:getAppName() .. " - " .. app:getPackageName())
end
```

### Open app and get screen content
```lua
emu:openApp("com.ss.android.ugc.aweme")
emu:waitMS(2000)
local ui = emu:getCurrentWindowByAccessibilityService(50, nil, nil)
if ui ~= nil then
    local root = ui:getRoot()
    if root ~= nil then
        print(root:toStringTree(0))
    end
end
```

### Click element and wait
```lua
emu:clickByPos(540.0, 1200.0)
emu:waitMS(1000)
print("Clicked and waited")
```

## Troubleshooting

### "Connection error" 
- Ensure ScriptServer is running on the device
- Check network connectivity (device and computer on same network)
- Verify IP address and port

### "Request timeout"
- Script execution took too long (>30s)
- Check if device is responsive

### Tool not appearing in Claude Desktop
- Restart Claude Desktop after config changes
- Check Claude Desktop logs for errors
- Verify the path in config is absolute and correct
