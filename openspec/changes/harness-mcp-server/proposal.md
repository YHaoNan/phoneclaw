## Why

测试 PhoneClaw 的 Lua 脚本执行能力需要每次在手机上操作，效率低下。需要一个基于电脑的工具，通过 MCP 协议快速调用 ScriptServer，实现脚本的编写、测试和调试。

## What Changes

- 创建独立的 Node.js MCP 服务器项目 `harness-test/`
- 提供 `executeScript` 工具，通过 HTTP 请求调用 ScriptServer 的 `/eval` 端点
- 支持 MCP 协议，可与 Claude Desktop、Cursor 等 AI 工具集成
- 完全独立于现有代码，不影响现有功能

## Capabilities

### New Capabilities

- `harness-mcp-server`: MCP 服务器封装，提供 executeScript 工具，通过网络调用 ScriptServer 执行 Lua 脚本

### Modified Capabilities

(无 - 这是独立的新项目，不修改现有能力)

## Impact

- **新增目录**: `harness-test/` - 独立的 Node.js 项目
- **新增依赖**: 无（独立项目，有自己的 package.json）
- **现有代码**: 不受影响
- **使用方式**: 在电脑上启动 MCP 服务器，连接到手机上的 ScriptServer (端口 8765)
