package top.yudoge.phoneclaw.ui.base

import java.lang.ref.WeakReference

abstract class BasePresenter<V : BaseContract.View> : BaseContract.Presenter<V> {
    private var viewRef: WeakReference<V>? = null
    
    protected val view: V?
        get() = viewRef?.get()
    
    protected val isViewAttached: Boolean
        get() = viewRef?.get() != null
    
    override fun attachView(view: V) {
        viewRef = WeakReference(view)
    }
    
    override fun detachView() {
        viewRef?.clear()
        viewRef = null
    }
}
