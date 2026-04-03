package top.yudoge.phoneclaw.scripts


data class EvalResult (
    /**
     * 是否执行成功
     */
    val success: Boolean,

    /**
     * 执行结果。当success为true有值
     */
    val evalResult: String,

    /**
     * 错误信息，当success为false时有值
     */
    val error: String,
)