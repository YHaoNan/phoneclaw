## Context

PhoneClaw 提供了 ScriptServer (端口 8765)，可以通过 HTTP POST `/eval` 端点执行 Lua 脚本。当前测试流程需要手动操作手机，效率低下。

MCP (Model Context Protocol) 是一个标准化的工具协议，允许 AI 模型调用外部工具。通过创建 MCP 服务器，可以将 PhoneClaw 的脚本执行能力暴露给 Claude Desktop、Cursor 等 AI 工具。

## Goals / Non-Goals

**Goals:**
- 创建独立的 Node.js MCP 服务器
- 提供 `executeScript` 工具，调用 ScriptServer 执行 Lua 脚本
- 支持配置 ScriptServer 地址和端口
- 返回脚本执行结果和日志

**Non-Goals:**
- 不修改 PhoneClaw 现有代码
- 不提供脚本编辑功能（由 AI 工具负责）
- 不实现认证或加密（本地开发用途）

## Decisions

### 1. 使用 `@modelcontextprotocol/sdk` 实现 MCP 服务器

**理由**: 官方 SDK 提供完整的 MCP 协议实现，包括工具注册、请求处理、错误处理。

**备选方案**:
- 手写 MCP 协议: 工作量大，容易出错
- 使用其他语言: Node.js 生态成熟，SDK 支持完善

### 2. 使用 HTTP POST 调用 ScriptServer

**理由**: ScriptServer 已经提供 HTTP 接口，直接调用即可，无需额外封装。

**请求格式**:
```
POST /eval HTTP/1.1
Content-Type: text/plain
Content-Length: <script-length>

<script-content>
```

**响应格式**:
```
SUCCESS
--- LOGS ---
<log-lines>
--- RESULT ---
<result>
```
或
```
FAILED
--- LOGS ---
<log-lines>
--- ERROR ---
<error-message>
```

### 3. 项目结构

```
harness-test/
├── package.json
├── tsconfig.json
├── src/
│   └── index.ts      # MCP 服务器入口
└── README.md
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| ScriptServer 未启动 | 工具返回明确错误信息 |
| 网络连接失败 | 捕获异常，返回友好错误 |
| 脚本执行超时 | ScriptServer 已有 30s 超时机制 |
