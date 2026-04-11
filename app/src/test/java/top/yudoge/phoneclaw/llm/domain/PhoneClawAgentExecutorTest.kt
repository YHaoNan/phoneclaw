package top.yudoge.phoneclaw.llm.domain

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import top.yudoge.phoneclaw.emu.domain.EmuFacade
import top.yudoge.phoneclaw.llm.callback.AgentRunCallBack
import top.yudoge.phoneclaw.llm.domain.objects.Session
import top.yudoge.phoneclaw.llm.domain.objects.ToolCallInfo
import top.yudoge.phoneclaw.llm.domain.objects.ToolCallResult

class PhoneClawAgentExecutorTest {
    
    private lateinit var mockSkillFacade: SkillFacade
    private lateinit var mockModelProviderFacade: ModelProviderFacade
    private lateinit var mockSessionFacade: SessionFacade
    private lateinit var mockEmuFacade: EmuFacade
    private lateinit var mockCallback: AgentRunCallBack
    
    private val testSession = Session(
        id = "test-session-1",
        title = "Test Session",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        mockSkillFacade = mock(SkillFacade::class.java)
        mockModelProviderFacade = mock(ModelProviderFacade::class.java)
        mockSessionFacade = mock(SessionFacade::class.java)
        mockEmuFacade = mock(EmuFacade::class.java)
        mockCallback = mock(AgentRunCallBack::class.java)
    }
    
    @Test
    fun `executor initializes without error`() {
        `when`(mockSessionFacade.getMessages(testSession.id)).thenReturn(emptyList())
        
        val executor = PhoneClawAgentExecutor(
            session = testSession,
            skillFacade = mockSkillFacade,
            modelProviderFacade = mockModelProviderFacade,
            sessionFacade = mockSessionFacade,
            emuFacade = mockEmuFacade
        )
        
        assertNotNull(executor)
    }
    
    @Test
    fun `flushSkills does not throw`() {
        `when`(mockSessionFacade.getMessages(testSession.id)).thenReturn(emptyList())
        
        val executor = PhoneClawAgentExecutor(
            session = testSession,
            skillFacade = mockSkillFacade,
            modelProviderFacade = mockModelProviderFacade,
            sessionFacade = mockSessionFacade,
            emuFacade = mockEmuFacade
        )
        
        executor.flushSkills()
    }
    
    @Test
    fun `flushModelProviders does not throw`() {
        `when`(mockSessionFacade.getMessages(testSession.id)).thenReturn(emptyList())
        
        val executor = PhoneClawAgentExecutor(
            session = testSession,
            skillFacade = mockSkillFacade,
            modelProviderFacade = mockModelProviderFacade,
            sessionFacade = mockSessionFacade,
            emuFacade = mockEmuFacade
        )
        
        executor.flushModelProviders()
    }
}
