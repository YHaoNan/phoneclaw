package top.yudoge.phoneclaw.app

import android.content.Context
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.emu.domain.EmuAccessibilityScreenOperator
import top.yudoge.phoneclaw.emu.domain.EmuAccessibilityScreenReader
import top.yudoge.phoneclaw.emu.domain.EmuAccessibilityServiceInterface
import top.yudoge.phoneclaw.emu.domain.EmuFacade
import top.yudoge.phoneclaw.emu.domain.LuaFriendlyEmuFacadeProxy
import top.yudoge.phoneclaw.emu.domain.EmuVLMScreenReader
import top.yudoge.phoneclaw.llm.data.repository.BuiltInSkillRepository
import top.yudoge.phoneclaw.llm.data.repository.MessageRepository
import top.yudoge.phoneclaw.llm.data.repository.MessageRepositoryImpl
import top.yudoge.phoneclaw.llm.data.repository.ModelProviderRepository
import top.yudoge.phoneclaw.llm.data.repository.ModelProviderRepositoryImpl
import top.yudoge.phoneclaw.llm.data.repository.ModelRepository
import top.yudoge.phoneclaw.llm.data.repository.ModelRepositoryImpl
import top.yudoge.phoneclaw.llm.data.repository.SessionRepository
import top.yudoge.phoneclaw.llm.data.repository.SessionRepositoryImpl
import top.yudoge.phoneclaw.llm.data.repository.UserSkillRepository
import top.yudoge.phoneclaw.llm.domain.ModelProviderFacade
import top.yudoge.phoneclaw.llm.domain.ModelProviderFactory
import top.yudoge.phoneclaw.llm.domain.PhoneClawAgentExecutor
import top.yudoge.phoneclaw.llm.domain.SessionFacade
import top.yudoge.phoneclaw.llm.domain.SkillFacade
import top.yudoge.phoneclaw.llm.domain.objects.Session
import top.yudoge.phoneclaw.llm.integration.ModelProviderFactoryImpl

class AppContainer private constructor(private val context: Context) {

    val appContext: Context
        get() = context
    
    val databaseHelper: PhoneClawDatabaseHelper by lazy {
        PhoneClawDatabaseHelper.getInstance(context)
    }
    
    val modelProviderRepository: ModelProviderRepository by lazy {
        ModelProviderRepositoryImpl(databaseHelper)
    }
    
    val modelRepository: ModelRepository by lazy {
        ModelRepositoryImpl(databaseHelper)
    }
    
    val sessionRepository: SessionRepository by lazy {
        SessionRepositoryImpl(databaseHelper)
    }
    
    val messageRepository: MessageRepository by lazy {
        MessageRepositoryImpl(databaseHelper)
    }
    
    val builtInSkillRepository: BuiltInSkillRepository by lazy {
        BuiltInSkillRepository(context)
    }
    
    val userSkillRepository: UserSkillRepository by lazy {
        UserSkillRepository(context, databaseHelper)
    }
    
    val modelProviderFactory: ModelProviderFactory by lazy {
        ModelProviderFactoryImpl()
    }
    
    val modelProviderFacade: ModelProviderFacade by lazy {
        ModelProviderFacade(modelProviderRepository, modelRepository, modelProviderFactory)
    }
    
    val skillFacade: SkillFacade by lazy {
        SkillFacade(builtInSkillRepository, userSkillRepository)
    }
    
    val sessionFacade: SessionFacade by lazy {
        SessionFacade(sessionRepository, messageRepository)
    }
    
    val emuAccessibilityReader: EmuAccessibilityScreenReader by lazy {
        EmuAccessibilityScreenReader { accessibilityService }
    }
    
    val emuVLMScreenReader: EmuVLMScreenReader by lazy {
        EmuVLMScreenReader()
    }
    
    val emuOperator: EmuAccessibilityScreenOperator by lazy {
        EmuAccessibilityScreenOperator { accessibilityService }
    }
    
    val emuFacade: EmuFacade by lazy {
        EmuFacade(
            accessibilityReader = emuAccessibilityReader,
            vlmReader = emuVLMScreenReader,
            operator = emuOperator,
            serviceProvider = { accessibilityService }
        )
    }

    val luaFriendlyEmuFacadeProxy: LuaFriendlyEmuFacadeProxy by lazy {
        LuaFriendlyEmuFacadeProxy(emuFacade)
    }
    
    var accessibilityService: EmuAccessibilityServiceInterface? = null
        public set
    
    fun createAgentExecutor(session: Session): PhoneClawAgentExecutor {
        return PhoneClawAgentExecutor(
            session = session,
            skillFacade = skillFacade,
            modelProviderFacade = modelProviderFacade,
            sessionFacade = sessionFacade,
            emuFacade = emuFacade
        )
    }
    
    companion object {
        @Volatile
        private var instance: AppContainer? = null
        
        @JvmStatic
        fun init(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
        }
        
        @JvmStatic
        fun getInstance(): AppContainer {
            return instance ?: throw IllegalStateException("AppContainer not initialized. Call init() first.")
        }
    }
}
