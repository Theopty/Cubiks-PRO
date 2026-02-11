package com.cubikspro.api

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client for the Anthropic Messages API with vision support.
 * Sends images of CUBIKS questions and receives structured solutions.
 */
class AnthropicApiClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-sonnet-4-20250514"
        private const val MAX_TOKENS = 4096
    }

    /**
     * Analyze a CUBIKS question image and return a structured solution.
     *
     * @param imageBytes The JPEG image data
     * @param questionType Optional hint about the question type (e.g., "spatial", "numerical")
     * @return [CubiksResult] with the analysis and answer
     */
    suspend fun solveCubiksQuestion(
        imageBytes: ByteArray,
        questionType: String? = null
    ): CubiksResult = withContext(Dispatchers.IO) {
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val systemPrompt = buildSystemPrompt(questionType)
        val requestBody = buildRequestBody(base64Image, systemPrompt)

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw ApiException("Empty response from API")

        if (!response.isSuccessful) {
            val errorJson = JSONObject(responseBody)
            val errorMessage = errorJson.optJSONObject("error")?.optString("message")
                ?: "Unknown API error (${response.code})"
            throw ApiException(errorMessage)
        }

        parseResponse(responseBody)
    }

    private fun buildSystemPrompt(questionType: String?): String {
        val typeHint = questionType?.let { " The question type is: $it." } ?: ""
        return """You are an expert at solving CUBIKS psychometric test questions. These include:
- Spatial reasoning (3D cube folding/unfolding, rotations, pattern matching)
- Numerical reasoning (data interpretation, number sequences, calculations)
- Verbal reasoning (logical deductions from text passages)
- Abstract reasoning (pattern sequences, shape transformations)
- Logiks (logical grid puzzles)

Analyze the image of the CUBIKS question carefully.$typeHint

You MUST respond in this exact format:

QUESTION_TYPE: [spatial/numerical/verbal/abstract/logiks]

ANALYSIS:
[Step-by-step breakdown of what you see in the image and your reasoning]

ANSWER: [The answer - letter choice like A/B/C/D/E, or a number, or true/false]

CONFIDENCE: [high/medium/low]

EXPLANATION:
[Clear explanation of why this is the correct answer, suitable for someone learning to solve these questions]"""
    }

    private fun buildRequestBody(base64Image: String, systemPrompt: String): JSONObject {
        val imageContent = JSONObject().apply {
            put("type", "image")
            put("source", JSONObject().apply {
                put("type", "base64")
                put("media_type", "image/jpeg")
                put("data", base64Image)
            })
        }

        val textContent = JSONObject().apply {
            put("type", "text")
            put("text", "Please analyze this CUBIKS question and provide the solution.")
        }

        val userMessage = JSONObject().apply {
            put("role", "user")
            put("content", JSONArray().apply {
                put(imageContent)
                put(textContent)
            })
        }

        return JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", MAX_TOKENS)
            put("system", systemPrompt)
            put("messages", JSONArray().apply {
                put(userMessage)
            })
        }
    }

    private fun parseResponse(responseBody: String): CubiksResult {
        val json = JSONObject(responseBody)
        val content = json.getJSONArray("content")

        val fullText = StringBuilder()
        for (i in 0 until content.length()) {
            val block = content.getJSONObject(i)
            if (block.getString("type") == "text") {
                fullText.append(block.getString("text"))
            }
        }

        val text = fullText.toString()
        return CubiksResult(
            rawResponse = text,
            questionType = extractField(text, "QUESTION_TYPE"),
            analysis = extractField(text, "ANALYSIS"),
            answer = extractField(text, "ANSWER"),
            confidence = extractField(text, "CONFIDENCE"),
            explanation = extractField(text, "EXPLANATION"),
        )
    }

    private fun extractField(text: String, field: String): String {
        // Match "FIELD:" or "FIELD: " followed by content up to the next known field or end
        val fields = listOf("QUESTION_TYPE", "ANALYSIS", "ANSWER", "CONFIDENCE", "EXPLANATION")
        val fieldIndex = fields.indexOf(field)
        val nextFields = fields.drop(fieldIndex + 1)

        val pattern = if (nextFields.isEmpty()) {
            Regex("""$field:\s*(.+)""", RegexOption.DOT_MATCHES_ALL)
        } else {
            val nextFieldPattern = nextFields.joinToString("|")
            Regex("""$field:\s*(.*?)(?=\n(?:$nextFieldPattern):|\z)""", RegexOption.DOT_MATCHES_ALL)
        }

        return pattern.find(text)?.groupValues?.get(1)?.trim() ?: ""
    }
}

data class CubiksResult(
    val rawResponse: String,
    val questionType: String,
    val analysis: String,
    val answer: String,
    val confidence: String,
    val explanation: String,
)

class ApiException(message: String) : Exception(message)
