package com.example.ai

import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Gemini REST Request/Response Models with Moshi
@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiConfig(
    @Json(name = "temperature") val temperature: Float = 0.7f,
    @Json(name = "topP") val topP: Float = 0.95f,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int = 500
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

class AIService {

    private val api: GeminiApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        retrofit.create(GeminiApi::class.java)
    }

    private val systemPrompt = """
        You are DK, a highly capable, polite, and helpful personal AI Assistant.
        You communicate effortlessly in Hindi, Hinglish, and English according to the user's language choice.
        Keep responses concise, conversational, and direct (1 to 3 short sentences) because your response will be read aloud by Text-To-Speech.
        Be warm, intelligent, and natural.
    """.trimIndent()

    suspend fun generateResponse(
        prompt: String,
        recentHistory: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getSmartOfflineResponse(prompt)
        }

        try {
            val contentList = mutableListOf<GeminiContent>()

            // Add up to last 4 conversation turns for context
            val historySlice = recentHistory.takeLast(4)
            for (msg in historySlice) {
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                if (msg.text.isNotBlank()) {
                    contentList.add(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = msg.text)),
                            role = role
                        )
                    )
                }
            }

            // Current user query
            contentList.add(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt)),
                    role = "user"
                )
            )

            val request = GeminiRequest(
                contents = contentList,
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                generationConfig = GeminiConfig(
                    temperature = 0.7f,
                    maxOutputTokens = 350
                )
            )

            val response = api.generateContent(apiKey, request)
            val replyText = response.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text?.trim()

            if (!replyText.isNullOrBlank()) {
                replyText
            } else {
                getSmartOfflineResponse(prompt)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getSmartOfflineResponse(prompt)
        }
    }

    private fun getSmartOfflineResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("kaise ho") || lower.contains("how are you") || lower.contains("kya haal") ->
                "Main badhiya hoon! Aap bataiye, main aapki kya madad kar sakta hoon?"

            lower.contains("naam kya") || lower.contains("who are you") || lower.contains("kaun ho") ->
                "Main DK hoon, aapka personal AI voice assistant. Aap mujhe apps kholne, call karne, messages bhejne ya sawaal puchne ke liye keh sakte hain."

            lower.contains("namaste") || lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Namaste! Main DK Assistant hoon. Kahiye, aaj main aapki kya madad karoon?"

            lower.contains("joke") || lower.contains("chutkula") || lower.contains("hasao") ->
                "Teacher: 10 me se 10 number lane wale ko inaam milega. Pappu: Sir, thoda discount dedo, 8 number me hi kaam chala lo!"

            lower.contains("dhanyawad") || lower.contains("thank") || lower.contains("shukriya") ->
                "Aapka swagat hai! Kabhi bhi madad chahiye ho toh bas boliye: Hey DK."

            lower.contains("mausam") || lower.contains("weather") ->
                "Mausam ki taaza jankari dekhne ke liye aap Google ya Weather app khol sakte hain."

            else ->
                "Main aapki baat samajh gaya. Kahiye, main iske baare me aur kya help karoon?"
        }
    }
}
