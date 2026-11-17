package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class AiRepository {
    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun enhanceCopy(rawInput: String, contextType: String, city: String): String {
        val prompt = """
            You are CITYLOOP's hyperlocal Indian community writing assistant.
            Rewrite and polish this $contextType draft for neighbors in $city, India.
            Keep it warm, authentic, concise (max 3 short sentences), and ready to publish.
            Do not add hashtags unless helpful (max 2). Return only the polished text.

            Draft:
            $rawInput
        """.trimIndent()

        return withTimeout(45_000L) {
            val response = model.generateContent(prompt)
            response.text?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("AI returned an empty response.")
        }
    }

    suspend fun translateText(text: String, targetLanguageLabel: String): String {
        val prompt = """
            Translate the following community post or listing into natural, everyday $targetLanguageLabel.
            Preserve prices in ₹, city names, and friendly tone. Return only the translation.

            Text:
            $text
        """.trimIndent()

        return withTimeout(45_000L) {
            val response = model.generateContent(prompt)
            response.text?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("AI returned an empty translation.")
        }
    }

    fun askCityConciergeStream(city: String, neighborhood: String, question: String, languageLabel: String): Flow<String> = flow {
        val prompt = """
            You are CITYLOOP AI Guide for $city ($neighborhood), India.
            Answer the resident's question concisely in $languageLabel with practical local context, safety/etiquette tips, or community ideas.
            Keep your answer structured in short scannable bullet points (under 140 words).

            Resident question: $question
        """.trimIndent()

        val sb = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        sb.append(chunkText)
                        emit(sb.toString())
                    }
                }
            }
            if (sb.isBlank()) {
                throw IllegalStateException("CityLoop AI Guide returned an empty response.")
            }
        } catch (e: CancellationException) {
            throw e
        }
    }
}
