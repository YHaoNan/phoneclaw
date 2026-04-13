# 查找应用
```lua
local apps = emu:getInstalledApps("抖音")
if apps == nil then
    print("无法获取应用列表")
elseif apps:size() == 0 then
    print("没找到")
else
    print("找到 " .. apps:size() .. " 个:")
    for i = 0, apps:size() - 1 do
        local app = apps:get(i)
        print(app:getPackageName() .. " - " .. app:getAppName())
    end
end
```

```text
SUCCESS
--- LOGS ---
找到 1 个:
抖音 (com.ss.android.ugc.aweme)
--- RESULT ---
```

# 打开应用
```lua
-- 打开抖音
local success = emu:openApp("com.ss.android.ugc.aweme")
if not success then
    print("打开抖音失败")
    return "failed: could not open app"
end
print("已发送打开抖音请求")

-- 等待窗口打开
local window = emu:waitWindowOpened("com.ss.android.ugc.aweme", nil, 5000)
if window == nil then
    print("等待窗口超时")
    return "failed: timeout waiting for window"
end
print("窗口已打开")

-- 等待内容加载
emu:waitMS(2000)

-- 搜索与"消息"相关的屏幕元素
local ui = emu:getCurrentWindowByAccessibilityService(
    50,                    -- maxDepth
    "com.ss.android.ugc.aweme",                   -- windowPackageName
    "消息|私信|聊天|信箱|通知",  -- filterPattern (正则)
    false,                  -- requireClickable
    false,                 -- requireLongClickable
    false,                 -- requireScrollable
    false,                 -- requireEditable
    false                  -- requireCheckable
)

if ui == nil then
    print("无法获取屏幕内容")
    return "failed: could not get UI"
end

print(ui:toString())
```

```text
SUCCESS
--- LOGS ---
已发送打开抖音请求
窗口已打开
UIWindow {package=com.ss.android.ugc.aweme, matched=1 nodes}
  [1] UITree(id=com.ss.android.ugc.aweme:id/0yq, className=TextView, text=消息, desc=消息，按钮, hintText=null, bounds=Rect(710, 2160 - 802, 2223), clickable=false, longClickable=false, scrollable=false, editable=false, checkable=false, checked=false, children=[])

--- RESULT ---
```




# TODO
1. 返回的java列表对象、java实体对象，数据访问方式和原生lua不一致，容易导致模型幻觉
   - emu类包一个代理，以lua方式交互
2. 获取窗口内容，windowPackageName不能为空
3. UIWindow、UITree等对象需要能更好的被访问
   - AI访问模式1：获取、打印、观察、下一turn修复代码
     - jsonString()
     - simpleJsonString()
   - AI访问模式2：单轮解决所有问题，所以需要lua内遍历
     - 各个属性获取方法
4. phone emulation skill编写
5. ✅构建一个mcp，可以从opencode这种agent直接测试harness，快，省钱
6. agent操作时总是先看当前屏幕
7. 用户操作手机时如何打断执行流？或者让agent知道用户的操作结果。（和6差不多，屏幕总是在变化的如何让agent了解屏幕发生了变化并及时更新自己的认知）
8. 设置屏幕常亮
9. 严格执行用户任务，不要做多余的事情
10. 知识总结（Agent应该能总结app的界面结构，常用脚本，并再下次执行任务时召回）