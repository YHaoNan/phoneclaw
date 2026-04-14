# Lua 数据建模思想（PhoneClaw）

本文档说明 PhoneClaw 在 Lua 侧的数据建模原则，以及 `LuaFriendlyEmuFacadeProxy` 引入后的对象访问方式。

## 1. 核心思想

Lua 的统一数据容器是 `table`，在工程实践里可以把它同时当作：

- 数组（顺序容器，典型用法 `t[1]`, `#t`）
- 字典（键值容器，典型用法 `t.key`）
- 对象（字段 + 函数）

因此，PhoneClaw 的 Lua 友好层采用策略：

- 动作类 API：保留函数调用（例如 `emu:openApp(...)`）
- 数据类返回值：统一转换为 `table`（可直接 `#`、`[]`、`.` 访问）

目标是让 AI/人类脚本都使用一致、可预测、低心智负担的访问模型。

## 2. 为什么不能直接暴露 Java 对象

直接注入 Java/Kotlin 对象（`userdata`）时，Lua 脚本常见问题：

- `List` 不能用 `#list`，必须 `list:size()`
- 索引是 `get(i)`，并且通常是 0-based
- 字段读取依赖 getter（如 `obj:getText()`），不符合 Lua 常见直觉
- 模型在生成脚本时容易混淆“Java 调用方式”和“Lua 调用方式”

所以我们将返回数据投影为 Lua `table`，降低脚本错误率。

## 3. PhoneClaw 的建模分层

- `EmuFacade`：原始领域门面（Kotlin/Java 语义）
- `LuaFriendlyEmuFacadeProxy`：Lua 侧代理（动作转发 + 返回值转换）
- `LuaValueConverter`：集中转换器（对象/集合/null -> Lua 值）

职责边界：

- 代理只做“调用转发 + 结果转换”，不改业务语义
- 转换器只做“数据形状映射”，不做业务逻辑

## 4. 数据契约（稳定字段）

当前约定：

- `AppInfo` -> `{ __type="AppInfo", packageName, appName }`
- `UIWindow` -> `{ __type="UIWindow", packageName, activityName, root, matchedNodes }`
- `UITree` -> `{ __type="UITree", id, className, text, desc, hintText, bounds, clickable, longClickable, scrollable, editable, checkable, checked, children }`
- `Rect` -> `{ left, top, right, bottom }`

集合约定：

- `List/Array` -> Lua array-like table（1-based）
- 可使用 `#items`、`items[1]`

空值约定：

- `null` -> `nil`

## 5. 方法还能调用吗？（关键问题）

分两类看：

1. `emu` 本身的方法（动作 API）

- 还能调用，且推荐继续调用。
- 例如：`emu:openApp(...)`, `emu:clickById(...)`, `emu:getCurrentWindowByAccessibilityService(...)`

2. `emu` 返回的数据对象的方法（原始 Java getter / size / get）

- 默认不再依赖这些方法。
- 这些返回值已被转换为 `table`，应改用字段/索引访问。
- 例如：
  - 旧：`apps:size()` / `apps:get(0):getPackageName()`
  - 新：`#apps` / `apps[1].packageName`

结论：

- “动作方法”可调用（在 `emu` 上）
- “返回对象的 Java 方法”不应再作为主路径；改用 table 字段访问

## 6. UI 打印方法

示例能力：

- `ui:pretty()`：按需渲染窗口/节点文本
- `node:pretty(depth)`：按需递归渲染节点

这些方法直接作为 Lua 函数字段附加在转换后的 table 上，不依赖原始 Java 对象方法。

## 7. 生命周期与垃圾回收

当前实现不会在 Lua table 中保留原始 `UIWindow/UITree` 引用：

- Java/Kotlin 对象在转换完成后不再被 Lua 层持有
- Lua 层只持有纯 table + Lua 函数字段
- 当 Lua table 不再引用时，由 Lua GC 回收

因此不会出现“为了调用原始方法而延长 Java 大对象生命周期”的问题。

## 8. 推荐脚本范式

```lua
local apps = emu:getInstalledApps("抖音")
if apps == nil or #apps == 0 then
  return "no app"
end

local first = apps[1]
print(first.packageName .. " - " .. first.appName)

local ui = emu:getCurrentWindowByAccessibilityService(50, nil, "消息", false, false, false, false, false)
if ui ~= nil then
  print(ui:pretty())
  local node = ui:firstMatched()
  if node ~= nil then
    local hits = node:findText("消息")
    print("hit count = " .. tostring(#hits))
  end
end
```

## 9. 演进原则

后续新增字段/对象时遵循：

- 字段命名保持稳定优先（避免频繁改名）
- 优先补字段，不轻易破坏既有字段
- 每次扩展都要同步单测与回归脚本
- 新增 helper 优先作为 table 的 Lua 函数字段，避免把 Lua 需求反向耦合到 Java 实体

这能保证 AI 生成脚本的长期稳定性。
