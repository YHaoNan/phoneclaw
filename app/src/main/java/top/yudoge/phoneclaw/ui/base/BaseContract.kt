package top.yudoge.phoneclaw.ui.base

interface BaseContract {
    interface View {
        fun showError(message: String)
        fun showLoading()
        fun hideLoading()
    }
    
    interface Presenter<V : View> {
        fun attachView(view: V)
        fun detachView()
    }
}
