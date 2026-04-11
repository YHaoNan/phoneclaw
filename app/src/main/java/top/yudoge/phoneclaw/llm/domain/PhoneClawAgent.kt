package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.domain.objects.Message
import top.yudoge.phoneclaw.llm.domain.objects.MessageRole
import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider
import top.yudoge.phoneclaw.llm.integration.AgentConfig
import top.yudoge.phoneclaw.llm.integration.AgentIntegration

class PhoneClawAgent(
    private val agentIntegration: AgentIntegration,
    private val modelProviderFacade: ModelProviderFacade,
    private val sessionFacade: SessionFacade,
    private val skillFacade: SkillFacade
) {
    private var currentConfig: AgentConfig? = null
    private var currentProvider: ModelProvider? = null
    private var currentModel: Model? = null
    
    fun configure(providerId: Long, modelId: String): Boolean {
        val provider = modelProviderFacade.getProviderById(providerId) ?: return false
        val model = modelProviderFacade.getModelById(modelId) ?: return false
        
        currentProvider = provider
        currentModel = model
        currentConfig = agentIntegration.createAgentConfig(provider, model)
        return true
    }
    
    fun configureWithTools(providerId: Long, modelId: String, tools: List<String>): Boolean {
        val provider = modelProviderFacade.getProviderById(providerId) ?: return false
        val model = modelProviderFacade.getModelById(modelId) ?: return false
        
        currentProvider = provider
        currentModel = model
        currentConfig = agentIntegration.createAgentConfigWithTools(provider, model, tools)
        return true
    }
    
    fun chat(userMessage: String, sessionId: String): String {
        val config = currentConfig ?: throw IllegalStateException("Agent not configured")
        
        sessionFacade.addMessage(
                        Message(
                            id = java.util.UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            role = MessageRole.USER,
                            content = userMessage,
                timestamp = System.currentTimeMillis()
            )
        )
        
        val response = executeModelCall(config, userMessage)
        
        sessionFacade.addMessage(
            Message(
                id = java.util.UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = MessageRole.AGENT,
                content = response,
                timestamp = System.currentTimeMillis()
            )
        )
        
        return response
    }
    
    fun chatWithSkill(userMessage: String, sessionId: String, skillName: String): String {
        val skill = skillFacade.getSkillByName(skillName) 
            ?: throw IllegalArgumentException("Skill not found: $skillName")
        val skillContent = skillFacade.getSkillContent(skill)
            ?: throw IllegalArgumentException("Skill content not found: $skillName")
        
        return chatWithSystemPrompt(userMessage, sessionId, skillContent.content)
    }
    
    fun chatWithSystemPrompt(userMessage: String, sessionId: String, systemPrompt: String): String {
        val config = currentConfig ?: throw IllegalStateException("Agent not configured")
        
        sessionFacade.addMessage(
            Message(
                id = java.util.UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = MessageRole.USER,
                content = userMessage,
                timestamp = System.currentTimeMillis()
            )
        )
        
        val response = executeModelCallWithSystem(config, userMessage, systemPrompt)
        
        sessionFacade.addMessage(
            Message(
                id = java.util.UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = MessageRole.AGENT,
                content = response,
                timestamp = System.currentTimeMillis()
            )
        )
        
        return response
    }
    
    private fun executeModelCall(config: AgentConfig, message: String): String {
        return "Model response placeholder for: $message"
    }
    
    private fun executeModelCallWithSystem(config: AgentConfig, message: String, systemPrompt: String): String {
        return "Model response with system prompt placeholder for: $message"
    }
    
    fun getCurrentModel(): Model? = currentModel
    fun getCurrentProvider(): ModelProvider? = currentProvider
}
