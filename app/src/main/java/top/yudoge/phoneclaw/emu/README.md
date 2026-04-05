实现EmuApi


# 等待API
1. `waitWindowOpened`用于等待指定窗口打开。当前我的API设计基于窗口包名。我在想你能不能提供某种更细粒度的等待，如基于Activity的，因为一个常见用法是调用openApp打开一个应用，然后waitWindowOpened等待窗口打开，但是大部分应用都具有开屏页，那不是我们关心的页面，我们关心的是主页面。当然我只是觉得可能可以实现，不过不用勉强，不要为了这个功能写出一堆垃圾代码。

2. `waitWindowOpened`不应该使用无障碍的事件模型，而是主动轮询，避免错过事件导致的永久挂起

# 读取屏幕API
1. 目前仅实现基于无障碍的，不实现基于VLM的
2. UIWindow对象代表查询结果，它是一个Window对象嵌套该Window下的UI布局。UI布局是一个List<UITree>结构。
3. UITree代表一个UI组件，以及下属子组件List<UITree>。我们关注它的：
   - 基本信息: id、text、desc（无障碍提示？我也不确定）
   - 交互手段：clickable、long clickable、swipeable等
   - 位置信息：四角xy坐标
   - 另外：你对上面字段的命名以及扩展有着足够的权限，你应该充分调研并决定如何设计UIWindow和UITree对象


# 操作API
基于id的点击、openApp、home、back，都使用无障碍实现

基于坐标的操作，都使用gesture api实现


# 关于阻塞
所有阻塞都阻塞调用者线程

# 模块化

该包下应该初始化三个组件
- EmuAccessibilityService：用于实现无障碍能力
- EmuVLMService：用于实现vlm能力（先不实现，空壳）
- EmuGestureService：用于实现手势能力

# 注意
1. 注意权限配置（如gesture权限是否在清单中写了）
2. 注意编译代码检查错误
3. 忽略系统中已有的代码，专注于该模块

