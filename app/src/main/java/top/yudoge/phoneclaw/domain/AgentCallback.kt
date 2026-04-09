package top.yudoge.phoneclaw.domain

interface AgentCallback {
    fun onLLMStreamStart()
    
    fun onLLMTokenGenerated(token: String)
    
    fun onLLMStreamEnd()
    
    fun onToolCallStart(toolName: String, params: String)
    
    fun onToolCallEnd(toolName: String, result: String, success: Boolean)
    
    fun onSkillCallStart(skillName: String)
    
    fun onSkillCallEnd(skillName: String, success: Boolean)
    
    fun onAgentComplete()
    
    fun onAgentError(error: String)
}
