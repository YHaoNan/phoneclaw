package top.yudoge.phoneclaw.llm.callback

import top.yudoge.phoneclaw.llm.domain.objects.ToolCallInfo
import top.yudoge.phoneclaw.llm.domain.objects.ToolCallResult

interface AgentRunCallBack {
    fun onAgentStart()
    fun onAgentError(e: Throwable)
    fun onAgentEnd()
    fun onReasoningStart()
    fun onReasoningEnd()
    fun onTextDelta(text: String)
    fun onTextDeltaComplete(text: String)
    fun onToolCallStart(info: ToolCallInfo)
    fun onToolCallEnd(result: ToolCallResult)
}
