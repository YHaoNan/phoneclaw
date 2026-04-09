package top.yudoge.phoneclaw.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AgentStatusManager {

    private val _status = MutableStateFlow<AgentStatus>(AgentStatus.Idle)
    val status: StateFlow<AgentStatus> = _status.asStateFlow()

    sealed class AgentStatus {
        object Idle : AgentStatus()
        object Thinking : AgentStatus()
        data class ToolCalling(
            val name: String,
            val state: CallState
        ) : AgentStatus()
        data class SkillCalling(
            val name: String,
            val state: CallState
        ) : AgentStatus()
        data class Completed(val success: Boolean) : AgentStatus()
    }

    enum class CallState { RUNNING, SUCCESS, FAILED }

    fun setStatus(status: AgentStatus) {
        _status.value = status
    }

    fun setThinking() {
        _status.value = AgentStatus.Thinking
    }

    fun setToolCalling(name: String, state: CallState) {
        _status.value = AgentStatus.ToolCalling(name, state)
    }

    fun setSkillCalling(name: String, state: CallState) {
        _status.value = AgentStatus.SkillCalling(name, state)
    }

    fun setCompleted(success: Boolean) {
        _status.value = AgentStatus.Completed(success)
    }

    fun reset() {
        _status.value = AgentStatus.Idle
    }
}
