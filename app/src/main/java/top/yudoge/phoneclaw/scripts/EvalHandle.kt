package top.yudoge.phoneclaw.scripts


/**
 * 执行句柄对象
 *
 * 对执行句柄的操作应该是单线程的，并且eval仅允许调用一次
 */
interface EvalHandle {

    /**
     * 执行脚本
     *
     * 该接口为异步接口，调用后立即返回
     */
    fun eval(listener: EvalListener, waitToExecuteTimeout: Long)

    /**
     * 获取脚本状态
     */
    fun state(): ScriptState

    /**
     * 注入Java对象到脚本执行环境中
     */
    fun inject(name: String, obj: Any)

    /**
     * 终止脚本执行
     */
    fun interruptEval();

}