package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.callback.AgentRunCallBack
import top.yudoge.phoneclaw.llm.domain.objects.Message
import top.yudoge.phoneclaw.llm.domain.objects.MessageRole
import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider
import top.yudoge.phoneclaw.llm.domain.objects.Session
import top.yudoge.phoneclaw.llm.domain.objects.Skill
import top.yudoge.phoneclaw.llm.integration.AgentConfig
import top.yudoge.phoneclaw.llm.integration.AgentIntegration

/**
 * 线程安全对象
 * 有状态对象
 */
class PhoneClawAgentExecutor(
    session: Session
) {

    init {
        flushSkills()
        flushModelProviders()
    }

    /**
     * 当发生技能变动时，可以回调该方法重新加载技能
     */
    fun flushSkills() {

    }

    /**
     * 当发生模型变动时，可以回调该方法重新加载模型提供商和模型列表
     */
    fun flushModelProviders() {

    }

    /**
     * 执行agent
     *
     * 该方法自动驱动ModelProvider实现，获取对应ChatModel。
     * 该方法需要在初始化时创建Memory（基于传入session）
     *
     * executor应维护agent内部状态（如果langchain本身有维护就不用我们来维护了），并在run时进行状态检测
     *
     * @param prompt 用户提示词
     * @param model  用户选择模型
     * @param callback  agent执行回调
     */
    fun run(
        prompt: String,
        model: Model,
        callback: AgentRunCallBack
    ) {

    }



}
