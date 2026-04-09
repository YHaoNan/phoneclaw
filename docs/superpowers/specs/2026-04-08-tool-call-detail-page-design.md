# 工具调用详情页设计文档

**日期**: 2026-04-08  
**状态**: 设计完成，待用户审查  
**范围**: 工具调用消息气泡的详情页功能，解决参数和结果被截断的问题

---

## 1. 概述

### 1.1 背景

当前工具调用消息气泡展开后，参数和结果设置了maxLines=10限制。当内容超过10行时被截断，用户无法查看完整信息。如果移除maxLines限制，长内容会占用过多空间，影响消息列表浏览体验。

### 1.2 目标

1. 保留消息列表的简洁性，不影响浏览体验
2. 允许用户查看完整的工具调用参数和结果
3. 提供便捷的复制、格式化、搜索功能
4. 符合Material Design设计规范

### 1.3 非目标

- 不修改其他消息类型（Agent消息、Skill消息）的展示方式
- 不涉及工具调用的实时更新逻辑
- 不涉及消息列表的性能优化

---

## 2. 整体架构

### 2.1 架构设计

采用"列表+详情页"的二级架构：

```
消息列表（ChatActivity + MessageAdapter）
  └─ 工具调用气泡（ToolCallViewHolder）
      └─ 检测内容是否被截断
          └─ 显示"查看完整详情"按钮
              └─ 点击跳转详情页

工具调用详情页（ToolCallDetailActivity）
  ├─ 状态展示（SUCCESS/FAILED/RUNNING）
  ├─ 参数展示（可滚动、支持复制/格式化/搜索）
  └─ 结果展示（可滚动、支持复制/格式化/搜索）
```

### 2.2 数据流

```
用户点击"查看完整详情"
  ↓
创建Intent，传递工具调用信息
  ├─ toolName: String
  ├─ params: String
  ├─ result: String?
  └─ state: String (RUNNING/SUCCESS/FAILED)
  ↓
启动ToolCallDetailActivity
  ↓
显示完整内容，提供复制/格式化/搜索功能
```

---

## 3. 消息列表改动

### 3.1 ToolCallViewHolder改造

**检测内容截断：**

```kotlin
private fun isContentTruncated(text: String, maxLines: Int): Boolean {
    if (text.isEmpty()) return false
    
    val textView = binding.paramsText
    val width = textView.width - textView.paddingLeft - textView.paddingRight
    
    val layout = StaticLayout.Builder.obtain(
        text, 0, text.length, textView.paint, width
    )
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()
    
    return layout.lineCount > maxLines || layout.getEllipsisCount(maxLines - 1) > 0
}

override fun bind(item: MessageItem) {
    // ... 现有代码 ...
    
    // 检测是否需要显示"查看完整"按钮
    val paramsTruncated = isContentTruncated(item.params, 10)
    val resultTruncated = item.result?.let { isContentTruncated(it, 10) } ?: false
    
    if (paramsTruncated || resultTruncated) {
        binding.viewFullButton.visibility = View.VISIBLE
        binding.viewFullButton.setOnClickListener {
            val context = binding.root.context
            val intent = Intent(context, ToolCallDetailActivity::class.java).apply {
                putExtra("toolName", item.toolName)
                putExtra("params", item.params)
                putExtra("result", item.result)
                putExtra("state", item.state.name)
                putExtra("timestamp", item.timestamp)
            }
            context.startActivity(intent)
        }
    } else {
        binding.viewFullButton.visibility = View.GONE
    }
}
```

**注意事项：**
- 在bind方法中检测截断，而不是在onCreateViewHolder中
- 需要等待TextView完成测量后才能计算行数
- 如果内容为空或未超过10行，隐藏按钮

### 3.2 布局文件改动

在`item_message_tool.xml`的`details_container`内添加按钮：

```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/view_full_button"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:text="查看完整详情"
    android:visibility="gone"
    app:icon="@drawable/ic_open_in_new"
    app:iconGravity="textEnd"
    style="@style/Widget.Material3.Button.TextButton" />
```

**位置：** 放在result_text之后，details_container的最后

---

## 4. 详情页设计

### 4.1 Activity结构

**创建新Activity：** `ToolCallDetailActivity`

**清单文件注册：**
```xml
<activity
    android:name=".ui.chat.ToolCallDetailActivity"
    android:exported="false"
    android:theme="@style/Theme.PhoneClaw" />
```

### 4.2 布局设计

**文件：** `activity_tool_call_detail.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:navigationIcon="@drawable/ic_close"
            app:title="工具调用详情" />

    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- 状态卡片 -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardCornerRadius="12dp"
                app:cardElevation="0dp"
                app:cardBackgroundColor="?attr/colorSurfaceContainer">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="工具名称"
                        android:textAppearance="@style/TextAppearance.Material3.LabelSmall"
                        android:textColor="?attr/colorOnSurfaceVariant" />

                    <TextView
                        android:id="@+id/tool_name_text"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="4dp"
                        android:textAppearance="@style/TextAppearance.Material3.TitleMedium"
                        android:textColor="?attr/colorOnSurface" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="12dp"
                        android:text="状态"
                        android:textAppearance="@style/TextAppearance.Material3.LabelSmall"
                        android:textColor="?attr/colorOnSurfaceVariant" />

                    <com.google.android.material.chip.Chip
                        android:id="@+id/status_chip"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="4dp"
                        android:clickable="false" />

                </LinearLayout>

            </com.google.android.material.card.MaterialCardView>

            <!-- 参数卡片 -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="0dp"
                app:cardBackgroundColor="?attr/colorSurfaceContainer">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="参数"
                            android:textAppearance="@style/TextAppearance.Material3.TitleSmall"
                            android:textColor="?attr/colorOnSurface" />

                        <ImageButton
                            android:id="@+id/format_params_button"
                            android:layout_width="36dp"
                            android:layout_height="36dp"
                            android:src="@drawable/ic_code"
                            android:background="@drawable/bg_ripple_circle"
                            android:tint="?attr/colorOnSurfaceVariant"
                            android:contentDescription="格式化JSON" />

                        <ImageButton
                            android:id="@+id/copy_params_button"
                            android:layout_width="36dp"
                            android:layout_height="36dp"
                            android:layout_marginStart="4dp"
                            android:src="@drawable/ic_copy"
                            android:background="@drawable/bg_ripple_circle"
                            android:tint="?attr/colorOnSurfaceVariant"
                            android:contentDescription="复制参数" />

                    </LinearLayout>

                    <EditText
                        android:id="@+id/params_text"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp"
                        android:background="@drawable/bg_code_block"
                        android:padding="12dp"
                        android:textAppearance="@style/TextAppearance.Material3.BodySmall"
                        android:fontFamily="monospace"
                        android:textIsSelectable="true"
                        android:inputType="none"
                        android:focusable="false"
                        android:focusableInTouchMode="false" />

                </LinearLayout>

            </com.google.android.material.card.MaterialCardView>

            <!-- 结果卡片 -->
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/result_card"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:visibility="gone"
                app:cardCornerRadius="12dp"
                app:cardElevation="0dp"
                app:cardBackgroundColor="?attr/colorSurfaceContainer">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="返回结果"
                            android:textAppearance="@style/TextAppearance.Material3.TitleSmall"
                            android:textColor="?attr/colorOnSurface" />

                        <ImageButton
                            android:id="@+id/format_result_button"
                            android:layout_width="36dp"
                            android:layout_height="36dp"
                            android:src="@drawable/ic_code"
                            android:background="@drawable/bg_ripple_circle"
                            android:tint="?attr/colorOnSurfaceVariant"
                            android:contentDescription="格式化JSON" />

                        <ImageButton
                            android:id="@+id/copy_result_button"
                            android:layout_width="36dp"
                            android:layout_height="36dp"
                            android:layout_marginStart="4dp"
                            android:src="@drawable/ic_copy"
                            android:background="@drawable/bg_ripple_circle"
                            android:tint="?attr/colorOnSurfaceVariant"
                            android:contentDescription="复制结果" />

                    </LinearLayout>

                    <EditText
                        android:id="@+id/result_text"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp"
                        android:background="@drawable/bg_code_block"
                        android:padding="12dp"
                        android:textAppearance="@style/TextAppearance.Material3.BodySmall"
                        android:fontFamily="monospace"
                        android:textIsSelectable="true"
                        android:inputType="none"
                        android:focusable="false"
                        android:focusableInTouchMode="false" />

                </LinearLayout>

            </com.google.android.material.card.MaterialCardView>

        </LinearLayout>

    </androidx.core.widget.NestedScrollView>

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 4.3 Activity代码实现

**文件：** `ToolCallDetailActivity.kt`

```kotlin
package top.yudoge.phoneclaw.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.ActivityToolCallDetailBinding
import java.util.regex.Pattern

class ToolCallDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityToolCallDetailBinding
    private var toolName: String = ""
    private var params: String = ""
    private var result: String? = null
    private var state: String = "SUCCESS"

    private var isParamsFormatted = false
    private var isResultFormatted = false
    private var originalParams: String = ""
    private var originalResult: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolCallDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取传递的数据
        toolName = intent.getStringExtra("toolName") ?: ""
        params = intent.getStringExtra("params") ?: ""
        result = intent.getStringExtra("result")
        state = intent.getStringExtra("state") ?: "SUCCESS"

        originalParams = params
        originalResult = result ?: ""

        setupToolbar()
        setupContent()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.toolbar.title = toolName
    }

    private fun setupContent() {
        // 设置工具名称
        binding.toolNameText.text = toolName

        // 设置状态
        when (state) {
            "RUNNING" -> {
                binding.statusChip.text = "执行中"
                binding.statusChip.chipBackgroundColorResource = R.color.primary_container
            }
            "SUCCESS" -> {
                binding.statusChip.text = "成功"
                binding.statusChip.chipBackgroundColorResource = R.color.success_container
            }
            "FAILED" -> {
                binding.statusChip.text = "失败"
                binding.statusChip.chipBackgroundColorResource = R.color.error_container
            }
        }

        // 设置参数
        binding.paramsText.setText(params)

        // 设置结果
        if (result != null) {
            binding.resultCard.visibility = android.view.View.VISIBLE
            binding.resultText.setText(result)
        } else {
            binding.resultCard.visibility = android.view.View.GONE
        }
    }

    private fun setupListeners() {
        // 复制参数
        binding.copyParamsButton.setOnClickListener {
            copyToClipboard(binding.paramsText.text.toString(), "工具参数")
        }

        // 格式化参数
        binding.formatParamsButton.setOnClickListener {
            if (isParamsFormatted) {
                binding.paramsText.setText(originalParams)
                isParamsFormatted = false
            } else {
                val formatted = formatJson(originalParams)
                binding.paramsText.setText(formatted)
                isParamsFormatted = true
            }
        }

        // 复制结果
        binding.copyResultButton.setOnClickListener {
            copyToClipboard(binding.resultText.text.toString(), "工具结果")
        }

        // 格式化结果
        binding.formatResultButton.setOnClickListener {
            if (isResultFormatted) {
                binding.resultText.setText(originalResult)
                isResultFormatted = false
            } else {
                val formatted = formatJson(originalResult)
                binding.resultText.setText(formatted)
                isResultFormatted = true
            }
        }
    }

    private fun formatJson(jsonString: String): String {
        return try {
            val json = JSONObject(jsonString)
            json.toString(2)
        } catch (e: Exception) {
            jsonString
        }
    }

    private fun copyToClipboard(text: String, label: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Snackbar.make(binding.root, "已复制到剪贴板", Snackbar.LENGTH_SHORT).show()
    }

    // 搜索功能
    private var searchKeyword: String = ""

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tool_detail, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as? androidx.appcompat.widget.SearchView
        
        searchView?.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchKeyword = newText ?: ""
                highlightSearchResults()
                return true
            }
        })
        
        return true
    }

    private fun highlightSearchResults() {
        if (searchKeyword.isEmpty()) {
            binding.paramsText.setText(if (isParamsFormatted) formatJson(originalParams) else originalParams)
            if (result != null) {
                binding.resultText.setText(if (isResultFormatted) formatJson(originalResult) else originalResult)
            }
            return
        }

        // 高亮参数中的搜索结果
        highlightText(binding.paramsText, searchKeyword)
        
        // 高亮结果中的搜索结果
        if (result != null) {
            highlightText(binding.resultText, searchKeyword)
        }
    }

    private fun highlightText(editText: EditText, keyword: String) {
        val text = editText.text.toString()
        val spannableString = SpannableString(text)
        
        try {
            val pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            
            while (matcher.find()) {
                spannableString.setSpan(
                    BackgroundColorSpan(Color.YELLOW),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            
            editText.setText(spannableString)
        } catch (e: Exception) {
            // 忽略无效的正则表达式
        }
    }
}
```

### 4.4 Menu资源文件

**文件：** `res/menu/menu_tool_detail.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <item
        android:id="@+id/action_search"
        android:icon="@drawable/ic_search"
        android:title="搜索"
        app:actionViewClass="androidx.appcompat.widget.SearchView"
        app:showAsAction="ifRoom|collapseActionView" />

</menu>
```

### 4.5 图标资源

需要创建以下图标（如果不存在）：

- `ic_open_in_new.xml` - 打开新页面图标
- `ic_code.xml` - 代码/格式化图标
- `ic_copy.xml` - 复制图标
- `ic_search.xml` - 搜索图标
- `ic_close.xml` - 关闭图标

---

## 5. 功能详细说明

### 5.1 复制功能

**触发方式：** 点击参数或结果卡片右上角的复制按钮

**实现：**
- 使用系统ClipboardManager
- 复制当前显示的内容（可能是原始内容或格式化后的内容）
- 显示Snackbar提示"已复制到剪贴板"

### 5.2 JSON格式化功能

**触发方式：** 点击参数或结果卡片右上角的格式化按钮

**实现：**
- 检测内容是否为合法JSON
- 如果是JSON，使用`JSONObject.toString(2)`格式化（缩进2空格）
- 如果不是JSON，保持原样
- 支持切换：点击按钮在原始内容和格式化内容之间切换

**状态管理：**
- `isParamsFormatted`: 标记参数是否已格式化
- `isResultFormatted`: 标记结果是否已格式化
- `originalParams`/`originalResult`: 保存原始内容

### 5.3 搜索功能

**触发方式：** 点击Toolbar上的搜索图标，输入关键词

**实现：**
- 使用SearchView作为Toolbar的菜单项
- 实时搜索（输入即搜索，无需按回车）
- 使用正则表达式匹配（不区分大小写）
- 匹配的内容使用黄色背景高亮显示

**注意事项：**
- 搜索时保留格式化状态
- 清空搜索关键词时恢复原样
- 无效的正则表达式被忽略

### 5.4 状态显示

**三种状态：**
- `RUNNING`: 执行中，显示蓝色背景Chip
- `SUCCESS`: 成功，显示绿色背景Chip
- `FAILED`: 失败，显示红色背景Chip

---

## 6. 边界情况处理

### 6.1 内容为空

- 参数为空：仍然显示参数卡片，内容显示空字符串
- 结果为空：隐藏结果卡片

### 6.2 内容不是JSON

- 点击格式化按钮时，检测是否为合法JSON
- 如果不是，保持原样，不报错

### 6.3 内容过长

- 使用NestedScrollView，支持滚动查看
- EditText设置为不可编辑，但可选择文本
- 使用等宽字体（monospace）便于查看代码

### 6.4 搜索关键词为空

- 清空搜索关键词时，恢复原始显示
- 不保留高亮状态

---

## 7. 性能考虑

### 7.1 内容截断检测

- 在bind方法中检测，每次绑定都会计算
- 如果性能有问题，可以在MessageItem中添加标志位缓存结果
- 或者使用更简单的估算：根据字符数/换行数判断

### 7.2 JSON格式化

- 格式化操作在主线程执行
- 如果JSON非常大（>1MB），可能需要异步处理
- 当前设计假设内容不会太大

### 7.3 搜索高亮

- 实时搜索，每次输入都会触发高亮
- 如果内容非常大，可能需要防抖（debounce）
- 当前设计假设内容大小适中

---

## 8. 测试策略

### 8.1 单元测试

- 测试`isContentTruncated`方法
- 测试`formatJson`方法
- 测试搜索高亮逻辑

### 8.2 集成测试

- 测试从消息列表跳转到详情页
- 测试数据传递是否正确
- 测试复制功能是否生效

### 8.3 UI测试

- 测试详情页显示是否正确
- 测试滚动是否流畅
- 测试搜索功能是否正常工作

---

## 9. 文件清单

需要创建/修改的文件：

**新增文件：**
1. `ui/chat/ToolCallDetailActivity.kt` - 详情页Activity
2. `res/layout/activity_tool_call_detail.xml` - 详情页布局
3. `res/menu/menu_tool_detail.xml` - 详情页菜单

**修改文件：**
1. `ui/chat/viewholders/ToolCallViewHolder.kt` - 添加截断检测和按钮
2. `res/layout/item_message_tool.xml` - 添加"查看完整详情"按钮
3. `AndroidManifest.xml` - 注册新Activity

**可能需要创建的图标：**
- `ic_open_in_new.xml`
- `ic_code.xml`
- `ic_copy.xml`
- `ic_search.xml`
- `ic_close.xml`

---

## 10. 实现优先级

**高优先级（核心功能）：**
1. 创建详情页Activity和布局
2. 实现截断检测和跳转逻辑
3. 实现复制功能

**中优先级（增强功能）：**
1. 实现JSON格式化功能
2. 实现搜索高亮功能

**低优先级（优化）：**
1. 性能优化（大文件处理）
2. 更多的交互细节

---

## 11. 风险与缓解

### 11.1 截断检测不准确

**风险：** StaticLayout计算可能不准确，导致按钮显示不一致

**缓解：**
- 使用更简单的估算方法（字符数/换行数）
- 添加测试用例验证

### 11.2 JSON格式化失败

**风险：** 格式化大JSON可能导致ANR

**缓解：**
- 限制格式化内容的大小
- 显示加载提示

### 11.3 搜索性能问题

**风险：** 实时搜索可能导致卡顿

**缓解：**
- 添加防抖机制
- 限制搜索频率

---

## 12. 下一步

设计文档已编写完成，接下来需要：

1. ✅ Spec自我审查
2. ⏳ 用户审查设计文档
3. ⏳ 开始实现

---

**设计审查清单：**
- [ ] 无TBD或TODO占位符
- [ ] 各部分逻辑一致，无矛盾
- [ ] 范围明确，聚焦单一功能
- [ ] 需求明确，无歧义
