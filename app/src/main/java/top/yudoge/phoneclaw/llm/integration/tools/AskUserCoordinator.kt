package top.yudoge.phoneclaw.llm.integration.tools

import top.yudoge.phoneclaw.llm.domain.objects.AskUserAnswerSource
import top.yudoge.phoneclaw.llm.domain.objects.AskUserRequest
import top.yudoge.phoneclaw.llm.domain.objects.AskUserResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import kotlin.concurrent.withLock

object AskUserCoordinator {
    private val logger: Logger = Logger.getLogger(AskUserCoordinator::class.java.name)

    interface Listener {
        fun onAskUserRequested(request: AskUserRequest)
    }

    private data class PendingRequest(
        val request: AskUserRequest,
        val latch: CountDownLatch = CountDownLatch(1),
        @Volatile var response: AskUserResponse? = null
    )

    private val lock = ReentrantLock()
    private var listener: Listener? = null
    private var pending: PendingRequest? = null

    fun setListener(listener: Listener?) {
        lock.withLock {
            this.listener = listener
        }
    }

    fun requestUserAnswer(request: AskUserRequest): AskUserResponse {
        val listenerSnapshot: Listener
        val pendingRequest = PendingRequest(request)
        lock.withLock {
            if (pending != null) {
                val error = "Another AskUser request is already pending"
                logger.warning("requestUserAnswer: $error")
                return AskUserResponse(
                    requestId = request.requestId,
                    confirmed = false,
                    source = AskUserAnswerSource.CANCELLED,
                    error = error
                )
            }
            pending = pendingRequest
            listenerSnapshot = listener ?: run {
                pending = null
                val error = "No AskUser listener registered"
                logger.warning("requestUserAnswer: $error")
                return AskUserResponse(
                    requestId = request.requestId,
                    confirmed = false,
                    source = AskUserAnswerSource.CANCELLED,
                    error = error
                )
            }
        }

        logger.info("requestUserAnswer: requestId=${request.requestId} dispatched")
        listenerSnapshot.onAskUserRequested(request)

        val completed = pendingRequest.latch.await(request.timeoutMs, TimeUnit.MILLISECONDS)
        val response = if (completed) {
            pendingRequest.response ?: AskUserResponse(
                requestId = request.requestId,
                confirmed = false,
                source = AskUserAnswerSource.CANCELLED,
                error = "Request completed without a response"
            )
        } else {
            logger.warning("requestUserAnswer: requestId=${request.requestId} timeout")
            AskUserResponse(
                requestId = request.requestId,
                confirmed = false,
                source = AskUserAnswerSource.TIMEOUT,
                error = "User response timeout"
            )
        }

        lock.withLock {
            if (pending === pendingRequest) {
                pending = null
            }
        }
        return response
    }

    fun submitResponse(response: AskUserResponse): Boolean {
        val pendingRequest = lock.withLock {
            val current = pending ?: return false
            if (current.request.requestId != response.requestId) {
                return false
            }
            current.response = response
            current
        }
        logger.info(
            "submitResponse: requestId=${response.requestId}, confirmed=${response.confirmed}, source=${response.source}"
        )
        pendingRequest.latch.countDown()
        return true
    }
}
