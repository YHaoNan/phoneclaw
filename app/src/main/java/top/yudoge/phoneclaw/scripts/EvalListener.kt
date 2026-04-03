package top.yudoge.phoneclaw.scripts

interface EvalListener {

    /**
     * 当新增日志行
     */
    fun onLogAppended(evalId: String, lines: List<String>)

    /**
     * 当执行结束
     */
    fun onFinished(evalId: String, result: EvalResult)


}