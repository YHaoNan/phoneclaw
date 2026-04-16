## 1. Domain Contracts And Validation

- [x] 1.1 新增 AskUser 请求/响应 domain 数据结构（question、answers、other、confirmed）
- [x] 1.2 为 AskUser 增加参数校验（question 非空、answers 数量 1-5、选项文本非空）
- [x] 1.3 增加“其他”输入与确认语义校验（选择 other 时文本必填，未确认不得提交）

## 2. Executor Integration (Normal Tool Path)

- [x] 2.1 在 AskUser 调用点接入现有工具调用链路，不新增状态机状态
- [x] 2.2 将用户确认结果映射为标准工具返回值并注入当前上下文
- [x] 2.3 确保 AskUser 处理后流程按既有执行路径继续至完成或错误

## 3. Persistence And Observability

- [x] 3.1 沿用现有 tool start/end 回调，不新增回调接口
- [x] 3.2 将 AskUser 请求与最终确认答案持久化到会话消息/元数据
- [x] 3.3 增加日志与埋点，覆盖提问发起、超时/取消、确认完成等路径

## 4. UI Interaction And Test Coverage

- [x] 4.1 实现询问弹窗交互：选项选择、其他文本输入、确认按钮提交
- [x] 4.2 保证“点击选项不自动提交”，仅点击确认后才返回最终答案
- [x] 4.3 将 AskUser 弹窗实现为底部弹出（bottom sheet）并校验样式与交互
- [x] 4.4 补充测试：参数校验、普通工具链路执行、确认语义、异常与超时路径、底部弹出展示
