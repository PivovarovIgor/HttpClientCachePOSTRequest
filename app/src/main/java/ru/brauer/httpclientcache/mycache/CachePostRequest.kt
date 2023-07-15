package ru.brauer.httpclientcache.mycache

import okhttp3.*

class TransformPostRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        // cache
        if (request.isCachePostRequest()) {
            val builder = request.newBuilder()
                // Change POST to GET
                .method("GET", null)

            // save the body
            saveRequestBody(builder, request.body)

            request = builder.build()
        }

        return chain.proceed(request)
    }
}

class CachePostResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        // Before the actual request
        if (request.isCachePostRequest()) {
            request = request.newBuilder()
                .method("POST", request.body)
                .build()
        }

        // Initiate a request
        var response = chain.proceed(request)

        // Get the request result
        // Cache for this interface
        if (response.request.isCachePostRequest()) {
            val builder = response.request.newBuilder()
                // Change POST to GET
                .method("GET", null)

            // save the body
            saveRequestBody(builder, request.body)

            response = response.newBuilder()
                .request(builder.build())
                .build()
        }

        return response
    }
}

private fun saveRequestBody(builder: Request.Builder, body: RequestBody?) {
    val bodyField = builder.javaClass.getDeclaredField("body")
    bodyField.isAccessible = true
    bodyField.set(builder, body)
}

private fun Request.isCachePostRequest(): Boolean = method == "POST" || body != null