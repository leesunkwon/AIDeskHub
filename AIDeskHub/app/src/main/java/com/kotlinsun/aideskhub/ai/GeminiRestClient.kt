package com.kotlinsun.aideskhub.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GeminiRestClient(
    private val apiKey: String,
) {
    fun generateAnswer(question: String): Result<String> {
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isBlank()) {
            return Result.failure(IllegalArgumentException("질문을 인식하지 못했습니다. 다시 말씀해 주세요."))
        }

        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API 키가 설정되어 있지 않습니다."))
        }

        return runCatching {
            val connection = createConnection()
            try {
                connection.outputStream.use { output ->
                    OutputStreamWriter(output, StandardCharsets.UTF_8).use { writer ->
                        writer.write(createRequestBody(trimmedQuestion).toString())
                    }
                }

                val responseCode = connection.responseCode
                val responseBody = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    connection.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                }

                if (responseCode !in 200..299) {
                    throw IllegalStateException(parseErrorMessage(responseBody, responseCode))
                }

                parseAnswer(responseBody)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun createConnection(): HttpURLConnection {
        val encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8.name())
        val url = URL("$GEMINI_ENDPOINT?key=$encodedKey")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun createRequestBody(question: String): JSONObject {
        return JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", question)),
                    ),
                ),
            )
    }

    private fun parseAnswer(responseBody: String): String {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: JSONArray()
        val firstCandidate = candidates.optJSONObject(0)
            ?: throw IllegalStateException("Gemini 응답이 비어 있습니다.")
        val content = firstCandidate.optJSONObject("content")
            ?: throw IllegalStateException("Gemini 응답 형식이 올바르지 않습니다.")
        val parts = content.optJSONArray("parts") ?: JSONArray()

        val answer = buildString {
            for (index in 0 until parts.length()) {
                val text = parts.optJSONObject(index)?.optString("text").orEmpty()
                if (text.isNotBlank()) {
                    append(text)
                }
            }
        }.trim()

        if (answer.isBlank()) {
            throw IllegalStateException("Gemini 답변이 비어 있습니다.")
        }
        return answer
    }

    private fun parseErrorMessage(responseBody: String, responseCode: Int): String {
        val apiMessage = runCatching {
            JSONObject(responseBody)
                .optJSONObject("error")
                ?.optString("message")
                .orEmpty()
        }.getOrDefault("")

        return if (apiMessage.isBlank()) {
            "Gemini API 호출에 실패했습니다. ($responseCode)"
        } else {
            "Gemini API 오류: $apiMessage"
        }
    }

    companion object {
        private const val MODEL_NAME = "gemini-3.1-flash-lite"
        private const val GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}
