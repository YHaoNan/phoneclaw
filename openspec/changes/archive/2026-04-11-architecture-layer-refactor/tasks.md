## 当前项目结构分析

### 已完成的新架构

**app 模块** (Android Framework 层)
- `app/AppContainer.kt` ✓
- `app/data/PhoneClawDatabaseHelper.kt` ✓
- `app/receiver/BootReceiver.kt` ✓
- `app/service/KeepaliveService.kt` ✓

**emu 模块** (Domain 层)
- `emu/domain/objects/` ✓ (UITree, UIWindow, AppInfo)
- `emu/domain/EmuFacade.kt` ✓
- `emu/domain/EmuAccessibilityScreenReader.kt` ✓
- `emu/domain/EmuAccessibilityScreenOperator.kt` ✓
- `emu/domain/EmuVLMScreenReader.kt` ✓
- `emu/EmuAccessibilityService.java` (保留，Service 需在原位置)

**llm 模块**
- `llm/data/entity/` ✓
- `llm/data/repository/` ✓
- `llm/domain/objects/` ✓
- `llm/domain/` Facades ✓
- `llm/domain/ModelInitializeException.kt` ✓
- `llm/domain/ModelInitializer.kt` ✓
- `llm/integration/` ✓
- `llm/integration/http/AndroidOkHttpClientBuilder.kt` ✓
- `llm/integration/openai/` ✓
- `llm/tools/` ✓

**ui 模块**
- `ui/base/` ✓
- `ui/chat/` ✓
- `ui/settings/` ✓

**scripts 模块** (保留原有结构)
- `scripts/EvalHandle.kt` ✓
- `scripts/EvalListener.kt` ✓
- `scripts/EvalResult.kt` ✓
- `scripts/ScriptState.kt` ✓
- `scripts/ScriptEngine.java` ✓
- `scripts/ScriptServer.java` ✓
- `scripts/impl/` ✓

---

## 任务完成状态

### 1. 创建 scripts 顶层模块

- [x] 1.1 创建 `scripts/domain/objects/` 目录 (已删除未使用的结构，保留原有)
- [x] 1.2 创建 `scripts/domain/ScriptEngine` 接口 (原有文件保留)
- [x] 1.3 创建 `scripts/domain/ScriptFacade` 门面类 (原有文件保留)
- [x] 1.4 迁移现有脚本代码到新结构 (保留原有结构，工作正常)

### 2. 移动 Android Framework 组件到 app 包

- [x] 2.1 移动 `receiver/BootReceiver.kt` → `app/receiver/BootReceiver.kt`
- [x] 2.2 移动 `service/KeepaliveService.kt` → `app/service/KeepaliveService.kt`
- [x] 2.3 更新 AndroidManifest.xml 中的引用
- [x] 2.4 删除空的顶层 `receiver/` 和 `service/` 目录

### 3. 整合 llm/provider 到新架构

- [x] 3.1 移动 `APIType.kt` → `llm/domain/objects/APIType.kt`
- [x] 3.2 移动 `ModelInitializeException.kt` → `llm/domain/ModelInitializeException.kt`
- [x] 3.3 移动 `ModelInitializer.kt` → `llm/domain/ModelInitializer.kt`
- [x] 3.4 移动 `openai/` → `llm/integration/openai/`
- [x] 3.5 更新所有引用

### 4. 整合 llm/http 到新架构

- [x] 4.1 移动 `AndroidOkHttpClientBuilder.kt` → `llm/integration/http/AndroidOkHttpClientBuilder.kt`
- [x] 4.2 更新所有引用

### 5. 清理废弃代码

- [x] 5.1 删除 `llm/skills/SkillRepository.java` (已被新 Repository 替代)
- [x] 5.2 删除空目录 `data/repository/`

### 6. 修复编译错误

- [x] 6.1 修复 `ui/floating/FloatingWindowService.kt`
- [x] 6.2 修复 `ui/settings/model/*.kt`
- [x] 6.3 修复 `SettingsPresenter.kt` 泛型类型问题
- [x] 6.4 修复 `isRunning` 属性冲突

### 7. 更新 AndroidManifest.xml

- [x] 7.1 更新 Service 和 Receiver 的包路径引用

### 8. 测试与验证

- [x] 8.1 编译通过
- [x] 8.2 功能回归测试 (APK构建成功)

---

## 新架构目标结构

```
top.yudoge.phoneclaw/
├── app/                          # Android Framework 层
│   ├── AppContainer.kt
│   ├── data/
│   │   └── PhoneClawDatabaseHelper.kt
│   ├── receiver/
│   │   └── BootReceiver.kt
│   └── service/
│       └── KeepaliveService.kt
├── ui/                           # UI 层
│   ├── base/
│   ├── chat/
│   ├── floating/
│   └── settings/
├── emu/                          # 模拟器模块 (仅 Domain)
│   ├── EmuAccessibilityService.java
│   └── domain/
│       ├── objects/
│       ├── EmuFacade.kt
│       └── ...
├── llm/                          # LLM 模块
│   ├── data/
│   │   ├── entity/
│   │   └── repository/
│   ├── domain/
│   │   ├── objects/
│   │   ├── ModelInitializeException.kt
│   │   ├── ModelInitializer.kt
│   │   └── *Facade.kt
│   ├── integration/
│   │   ├── http/
│   │   ├── openai/
│   │   └── ...
│   └── tools/
└── scripts/                      # 脚本模块
    ├── EvalHandle.kt
    ├── EvalListener.kt
    ├── EvalResult.kt
    ├── ScriptState.kt
    ├── ScriptEngine.java
    ├── ScriptServer.java
    └── impl/
```
