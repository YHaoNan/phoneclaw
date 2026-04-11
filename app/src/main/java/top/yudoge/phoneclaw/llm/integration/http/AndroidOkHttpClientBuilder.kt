package top.yudoge.phoneclaw.llm.integration.http

import dev.langchain4j.exception.HttpException
import dev.langchain4j.http.client.HttpClient
import dev.langchain4j.http.client.HttpClientBuilder
import dev.langchain4j.http.client.HttpMethod
import dev.langchain4j.http.client.HttpRequest
import dev.langchain4j.http.client.SuccessfulHttpResponse
import dev.langchain4j.http.client.sse.ServerSentEventListener
import dev.langchain4j.http.client.sse.ServerSentEventParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.time.Duration

class AndroidOkHttpClientBuilder : HttpClientBuilder {
    private var connectTimeout: Duration = Duration.ofSeconds(15)
    private var readTimeout: Duration = Duration.ofSeconds(60)

    override fun connectTimeout(): Duration = connectTimeout

    override fun connectTimeout(timeout: Duration): HttpClientBuilder = apply {
        this.connectTimeout = timeout
    }

    override fun readTimeout(): Duration = readTimeout

    override fun readTimeout(timeout: Duration): HttpClientBuilder = apply {
        this.readTimeout = timeout
    }

    override fun build(): HttpClient {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .build()
        return AndroidOkHttpClient(okHttpClient)
    }
}

private class AndroidOkHttpClient(
    private val client: OkHttpClient
) : HttpClient {

    override fun execute(request: HttpRequest): SuccessfulHttpResponse {
        client.newCall(request.toOkHttpRequest()).execute().use { response ->
            if (!response.isSuccessful) {
                throw HttpException(response.code, response.body?.string().orEmpty())
            }
            return response.toSuccessfulHttpResponse(response.body?.string())
        }
    }

    override fun execute(
        request: HttpRequest,
        parser: ServerSentEventParser,
        listener: ServerSentEventListener
    ) {
        client.newCall(request.toOkHttpRequest()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                listener.onError(e)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        listener.onError(HttpException(it.code, it.body?.string().orEmpty()))
                        return
                    }

                    listener.onOpen(it.toSuccessfulHttpResponse(body = ""))

                    val stream = it.body?.byteStream()
                    if (stream == null) {
                        listener.onError(IllegalStateException("Empty response body"))
                        return
                    }

                    parser.parse(stream, listener)
                    listener.onClose()
                }
            }
        })
    }

    private fun HttpRequest.toOkHttpRequest(): Request {
        val requestBuilder = Request.Builder().url(url())

        headers().forEach { (name, values) ->
            values.forEach { value -> requestBuilder.addHeader(name, value) }
        }

        when (method()) {
            HttpMethod.GET -> requestBuilder.get()
            HttpMethod.POST -> {
                val requestBody = (body() ?: "").toRequestBody("application/json".toMediaType())
                requestBuilder.post(requestBody)
            }
            HttpMethod.DELETE -> requestBuilder.delete()
        }

        return requestBuilder.build()
    }

    private fun Response.toSuccessfulHttpResponse(body: String?): SuccessfulHttpResponse {
        return SuccessfulHttpResponse.builder()
            .statusCode(code)
            .headers(headers.toMultimap())
            .body(body)
            .build()
    }
}
