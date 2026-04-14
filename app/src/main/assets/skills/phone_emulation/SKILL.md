---
name: phone_emulation
description: 此技能允许你通过Lua脚本来模拟用户操作手机，包括但不限于获取手机屏幕、获取App列表、执行人类操作
---

# 执行方式
通过调用`executeScript`工具，传入待执行的Lua脚本

# 脚本API
提供了`emu`对象专门用于处理一切用户模拟操作，下面介绍它的API。

## 手机操作类

### openApp——打开应用

**参数表**：
参数|类型| 功能 |是否必须|默认值
:-:|:-:|:--:|:-:|:-:
packageName|字符串|指定要打开的应用|是|-

**返回值**：

> 返回boolean类型值，代表是否打开成功


```lua
local result = emu:openApp("com.xxx.yyy")
```

1. 通常不要依赖它的返回值
2. 推荐使用`waitWindowOpened`来确认自己已经进入指定应用


### getInstalledApp——搜索应用

**参数表**：
参数|类型| 功能 |是否必须|默认值
:-:|:-:|:--:|:-:|:-:
filterPattern|字符串|要匹配的模式，若包名或应用名中包含，则命中。若为空，则返回全量应用|否|nil

**返回值**：
由lua table表示的数组，若无匹配app，则返回空数组。列表上的`pretty`方法会返回结构化文本，建议配合`print(...)`输出到控制台。

```lua
local apps = emu:getInstalledApps("抖音")
print(apps:pretty())
```

1. 当你接收到用户的`打开xxx，然后yyy`这样的任务时，除非你知道xxx的具体包名，你都应该去调用该方法
2. 你可以调用`print(apps:pretty())`，然后观察控制台输出，拿到包名。此后你应该都不需要再调用该方法了。


### waitWindowOpened——等待窗口打开

**参数表**：

| 参数 | 类型 | 功能 | 是否必需 | 默认值 |
|:-:|:-:|:--:|:----:|:-:|
| packageName | 字符串	nil | 目标窗口包名，传 `nil` 表示不按包名过滤 |  是   | nil |
| activityName | 字符串	nil | 目标 Activity 名称，传 `nil` 表示不按 Activity 过滤 |  否   | nil |
| timeoutMs | number | 最大等待时长（毫秒） |  是   | - |

**返回值**：
> 若命中窗口则返回安卓原生 `AccessibilityWindowInfo` 对象；超时/失败返回 `nil`。

```lua
local window = emu:waitWindowOpened("com.ss.android.ugc.aweme", nil, 5000)
if window == nil then
  return "timeout"
end
print("应用已打开")
```

1. 推荐在 `openApp` 后立即调用，作为是否真正进入目标页面的判据。
2. 合理设置timeout
3. 尽量不要使用activityName，这是一个实验性功能
4. 该返回值是安卓原生的AccessibilityWindowInfo对象，没有 `pretty()`，不建议直接用Lua脚本访问
5. 确认窗口已打开后，如需读取界面，请改用 `getCurrentWindowByAccessibilityService(...)`。

### waitMS——等待固定时长

**参数表**：

| 参数 | 类型 | 功能 | 是否必需 | 默认值 |
|:-:|:-:|:--:|:-:|:-:|
| milliseconds | number | 阻塞等待时长（毫秒） | 是 | - |

**返回值**：
> 无（Lua 侧为 `nil`）。

```lua
emu:waitMS(1500)
```

1. 仅用于必要的页面稳定等待，避免无意义长等待。
2. 优先使用“事件/状态确认”方法（如 `waitWindowOpened`）代替硬等待。

### back——系统返回

**参数表**：无

**返回值**：
> `boolean`，表示系统是否接受返回动作请求。

```lua
local ok = emu:back()
```

1. `true` 仅表示请求已发出，不代表页面一定切换成功。
2. 返回后建议通过窗口/节点再次确认当前页面状态。
3. 慎用，尽量通过UI元素来实现返回

### home——回到桌面

**参数表**：无

**返回值**：
> `boolean`，表示系统是否接受 HOME 动作请求。

```lua
local ok = emu:home()
```

1. 常用于任务收尾或故障恢复。
2. 执行后若要继续自动化，需重新定位目标应用窗口。
3. 慎用！

### getCurrentWindowByAccessibilityService——读取当前窗口UI

**参数表**：

| 参数 | 类型 |            功能            | 是否必需 | 默认值 |
|:-:|:-:|:------------------------:|:----:|:-:|
| maxDepth | number |        UI树最大遍历深度         |  否   | 50 |
| windowPackageName | 字符串	nil |        仅读取指定包名窗口         |  是   | nil |
| filterPattern | 字符串	nil | 文本/描述过滤模式（正则），若不填，则不执行过滤 |  否   | nil |
| requireClickable | boolean |         仅保留可点击节点         |  否   | false |
| requireLongClickable | boolean |         仅保留可长按节点         |  否   | false |
| requireScrollable | boolean |         仅保留可滚动节点         |  否   | false |
| requireEditable | boolean |         仅保留可编辑节点         |  否   | false |
| requireCheckable | boolean |         仅保留可勾选节点         |  否   | false |

**返回值**：
> 返回lua table，代表UIWindow对象，返回对象结构详见文末

```lua
local ui = emu:getCurrentWindowByAccessibilityService(
  50,
  "com.ss.android.ugc.aweme",
  "消息|私信",
  false,
  false,
  false,
  false,
  false
)

if ui ~= nil then
  print(ui:pretty())
end
```

1. 你应该总是经常使用filterPattern，除非你真的有必要查看全屏
2. 如果你搜索不到想要的内容，尝试把requireXXX条件放宽，很多时候即使requireClickable为false也可点击
3. 如果你处于探索阶段，请使用`print(ui:pretty())`这类形式将屏幕内容输出到控制台后观察控制台
4. 如果你希望执行一步到位的操作，则可以对返回对象进行编程。返回对象结构详见文末。

### clickById——按节点ID点击

**参数表**：

| 参数 | 类型 | 功能 | 是否必需 | 默认值 |
|:-:|:-:|:--:|:-:|:-:|
| id | 字符串 | 目标节点 id（resource-id） | 是 | - |

**返回值**：
> `boolean`，表示点击请求是否执行成功。

```lua
local ok = emu:clickById("com.xxx:id/btn_send")
```

1. 仅当UITree节点的idIsUnique=true时使用，否则使用clickByPos
2. 点击后建议二次读取 UI，确认动作生效
3. 禁止使用猜测的位置

### clickByPos——按坐标点击

**参数表**：

| 参数 | 类型 | 功能 | 是否必需 | 默认值 |
|:-:|:-:|:--:|:-:|:-:|
| x | number | X 坐标 | 是 | - |
| y | number | Y 坐标 | 是 | - |

**返回值**：
> `boolean`，表示点击动作是否提交成功。

```lua
local ok = emu:clickByPos(540, 1900)
```

1. 坐标点击对分辨率/布局变化敏感，在UITree节点isIsUnique=true时优先使用`clickById`
2. 坐标仅在你已明确位置且无稳定 id 时作为兜底手段。
3. 禁止使用猜测的位置

### longClickById——按节点ID长按

**参数表**：

| 参数 | 类型 | 功能 | 是否必需 | 默认值 |
|:-:|:-:|:--:|:-:|:-:|
| id | 字符串 | 目标节点 id | 是 | - |
| durationMs | number | 长按时长（毫秒） | 否 | 500 |

**返回值**：
> `boolean`，表示长按请求是否执行成功。

```lua
local ok = emu:longClickById("com.xxx:id/item", 800)
```

1. 仅当UITree节点的idIsUnique=true时使用，否则使用clickByPos
2. 不同应用对长按阈值敏感，必要时提高 `durationMs`。
3. 长按后通常会弹菜单，建议马上重新读取 UI。

### longClickByPos——按坐标长按

**参数表**：

| 参数 | 类型 | 功能 | 是否必需 | 默认值 |
|:-:|:-:|:--:|:-:|:-:|
| x | number | X 坐标 | 是 | - |
| y | number | Y 坐标 | 是 | - |
| durationMs | number | 长按时长（毫秒） | 否 | 500 |

**返回值**：
> `boolean`，表示长按动作是否提交成功。

```lua
local ok = emu:longClickByPos(540, 1200, 900)
```

1. 与 `clickByPos` 相同，坐标方案优先级应低于节点方案。
2. 长按后常触发上下文菜单，后续步骤请先做状态确认。
3. 禁止使用猜测的位置

### swipe——滑动手势

**参数表**：

| 参数 | 类型 | 功能 | 是否必需 | 默认值 |
|:-:|:-:|:--:|:-:|:-:|
| fromX | number | 起点 X | 是 | - |
| fromY | number | 起点 Y | 是 | - |
| toX | number | 终点 X | 是 | - |
| toY | number | 终点 Y | 是 | - |
| durationMs | number | 滑动时长（毫秒） | 是 | - |

**返回值**：
> `boolean`，表示滑动动作是否提交成功。

```lua
local ok = emu:swipe(540, 1800, 540, 600, 300)
```

1. 上滑/下滑可通过起终点互换表达。
2. 滑动后列表位置改变，后续不要复用旧节点引用。

### inputTextById——按节点ID输入文本

**参数表**：

| 参数 | 类型 | 功能 | 是否必需 | 默认值 |
|:-:|:-:|:--:|:-:|:-:|
| id | 字符串 | 输入框节点 id | 是 | - |
| text | 字符串 | 待输入文本 | 是 | - |

**返回值**：
> `boolean`，表示输入动作是否执行成功。

```lua
local ok = emu:inputTextById("com.xxx:id/edit", "hello")
```

1. 输入前应确认节点可编辑（可用 UI 过滤参数提前筛选）。
2. 输入后可重新读取节点文本做校验。

### inputTextByPos——按坐标输入文本

**参数表**：

| 参数 | 类型 | 功能 | 是否必需 | 默认值 |
|:-:|:-:|:--:|:-:|:-:|
| x | number | 目标输入区域 X 坐标 | 是 | - |
| y | number | 目标输入区域 Y 坐标 | 是 | - |
| text | 字符串 | 待输入文本 | 是 | - |

**返回值**：
> `boolean`，表示输入动作是否执行成功。

```lua
local ok = emu:inputTextByPos(540, 2100, "你好")
```

1. 坐标输入依赖焦点状态，失败时可先点击输入区域再输入。
2. 与其他坐标类操作一样，尽量仅作为兜底方案使用。
3. 禁止使用猜测的位置

## 执行建议
1. 当执行用户任务时，应该分为两个阶段
2. 第一个阶段为探索阶段，在这个阶段中应该多次通过调用`executeScript`获取界面结构，尝试执行任务，目的是了解环境
3. 第二个阶段为执行阶段，当你觉得此任务流程比较固定，可以总结成一整个脚本来提速任务执行时，请你重新思考并编写整个脚本
4. 不要猜测，当有问题不确定时，询问用户

## UIWindow与UITree结构补全

### UIWindow 结构（Lua table）

`emu:getCurrentWindowByAccessibilityService(...)` 返回的对象是 `UIWindow` 形态的 Lua table，结构如下：

```lua
UIWindow = {
  __type = "UIWindow",
  packageName = "com.xxx",      -- string | nil
  activityName = "MainActivity", -- string | nil
  root = UITree | nil,            -- 当 filterPattern 为空时，通常可拿到完整树
  matchedNodes = { UITree, ... }, -- 数组（1-based）

  -- 方法
  pretty = function(self) ... end -- 返回格式化字符串
}
```

字段说明：

1. `packageName`：当前窗口所属应用包名。
2. `activityName`：当前窗口 Activity 名称，可能为空。
3. `root`：完整 UI 树根节点；当你做全屏探索时优先使用。
4. `matchedNodes`：匹配 `filterPattern` 后的节点数组；做定向操作时优先使用。
5. `pretty()`：返回窗口与节点结构的格式化字符串，通常配合`print(window:pretty())`使用。

---

### UITree 结构（Lua table）

`UIWindow.root` 与 `UIWindow.matchedNodes[i]` 的节点均为 `UITree` 形态：

```lua
UITree = {
  __type = "UITree",
  id = "com.xxx:id/button",      -- string | nil
  isIdUnique = false,            -- boolean
  className = "android.widget.TextView", -- string | nil
  text = "消息",                  -- string | nil
  desc = "消息，按钮",             -- string | nil
  hintText = "请输入内容",         -- string | nil

  bounds = {
    left = 100,
    top = 200,
    right = 300,
    bottom = 260
  } | nil,

  clickable = false,
  longClickable = false,
  scrollable = false,
  editable = false,
  checkable = false,
  checked = false,

  children = { UITree, ... }, -- 数组（1-based）

  -- 方法
  pretty = function(self) ... end -- 返回该节点（及其子树）的格式化字符串
}
```

字段说明：

1. `id`：resource-id，优先用于稳定定位。
2. `isIdUnique`：当前id在屏幕中是否唯一
3. `className`：控件类型（TextView/Button/...）。
4. `text` / `desc` / `hintText`：常用于语义识别与过滤。
5. `bounds`：节点屏幕坐标范围，坐标点击时可用于计算中心点。
6. `clickable/longClickable/...`：交互能力标记。
7. `children`：子节点数组，支持递归遍历。
8. `pretty()`：返回当前节点树的格式化字符串，通常配合`print(node:pretty())`使用。
