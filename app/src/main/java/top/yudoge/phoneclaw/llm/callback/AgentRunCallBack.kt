package top.yudoge.phoneclaw.llm.callback

interface AgentRunCallBack {

    fun onAgentStart()

    fun onAgentError(e: Throwable)

    fun onAgentEnd()

    fun onReasoningStart();

    fun onReasoningEnd();

    fun onTextDelta(text: String)

    fun onTextDeltaComplete(text: String)

    /**
     * AI扩充该方法，给定tool对象，参数
     */
    fun onToolCallStart()

    /**
     * AI扩充该方法，给定tool对象，返回值
     */
    fun onToolCallEnd()


}