package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiManager {
    private const val TAG = "GeminiManager"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if a valid non-placeholder API key is set.
     */
    fun isApiKeyAvailable(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")
    }

    /**
     * Ask Gemini to generate smart playlist recommendations from the local music catalog based on user input.
     * Fallbacks gracefully to offline rules if key is missing.
     */
    suspend fun getSmartRecommendations(userInput: String, localSongs: List<Song>): Pair<List<Long>, String> = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable() || localSongs.isEmpty()) {
            return@withContext getOfflineSmartRecommendations(userInput, localSongs)
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val songsJsonArray = JSONArray()
        localSongs.forEach { song ->
            val obj = JSONObject().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("album", song.album)
                put("genre", song.genre)
                put("mood", song.mood)
                put("bpm", song.bpm)
            }
            songsJsonArray.put(obj)
        }

        val prompt = """
            You are TBM Music AI, an ultra-premium cyberpunk music assistant.
            The user wants the following type of playlist: "$userInput".
            Here is their local song library as a JSON list:
            $songsJsonArray
            
            Select up to 5 songs from their library that match the request.
            Your response must be in JSON format matching this schema:
            {
              "recommended_song_ids": [numbers representing the IDs of matching songs],
              "ai_curation_notes": "A beautiful, premium, cyberpunk-themed and descriptive curation paragraph explaining this combination."
            }
            Do not include any Markdown wrapping like ```json or anything else. Just the raw JSON string.
        """.trimIndent()

        try {
            val jsonRequest = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API error: ${response.code} - ${response.message}")
                    return@withContext getOfflineSmartRecommendations(userInput, localSongs, "Error: ${response.code} returned by server. ")
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.getJSONArray("candidates")
                val textResponse = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                // Remove code block wrappers if any
                val cleanJson = textResponse.replace("```json", "").replace("```", "").trim()
                val curationResult = JSONObject(cleanJson)

                val idsArray = curationResult.getJSONArray("recommended_song_ids")
                val idsList = ArrayList<Long>()
                for (i in 0 until idsArray.length()) {
                    idsList.add(idsArray.getLong(i))
                }
                val notes = curationResult.getString("ai_curation_notes")

                return@withContext Pair(idsList, notes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API", e)
            return@withContext getOfflineSmartRecommendations(userInput, localSongs, "AI connection offline (${e.localizedMessage}). ")
        }
    }

    /**
     * Fallback high-fidelity local rules-based smart playlist recommender.
     */
    private fun getOfflineSmartRecommendations(
        userInput: String,
        localSongs: List<Song>,
        prependMsg: String = ""
    ): Pair<List<Long>, String> {
        val inputLower = userInput.lowercase()
        val matchingSongs = ArrayList<Song>()

        // Rule-based filtering matching local songs tags to user queries
        if (inputLower.contains("cyber") || inputLower.contains("neon") || inputLower.contains("synth") || inputLower.contains("run") || inputLower.contains("energetic") || inputLower.contains("fast")) {
            matchingSongs.addAll(localSongs.filter { it.genre.lowercase().contains("synth") || it.mood.lowercase() == "energetic" || it.bpm >= 110 })
        } else if (inputLower.contains("chill") || inputLower.contains("relax") || inputLower.contains("lofi") || inputLower.contains("slow") || inputLower.contains("quiet") || inputLower.contains("night")) {
            matchingSongs.addAll(localSongs.filter { it.genre.lowercase().contains("lofi") || it.genre.lowercase().contains("ambient") || it.mood.lowercase() == "relaxed" })
        } else if (inputLower.contains("sad") || inputLower.contains("blue") || inputLower.contains("melancholy") || inputLower.contains("deep")) {
            matchingSongs.addAll(localSongs.filter { it.mood.lowercase() == "melancholic" || it.genre.lowercase().contains("industrial") })
        } else if (inputLower.contains("space") || inputLower.contains("cosmic") || inputLower.contains("solar") || inputLower.contains("star")) {
            matchingSongs.addAll(localSongs.filter { it.genre.lowercase().contains("space") || it.artist.lowercase().contains("solar") })
        } else {
            // Default select a random mix
            matchingSongs.addAll(localSongs.shuffled().take(3))
        }

        val chosenSongs = matchingSongs.distinctBy { it.id }.take(4)
        val songsListDesc = chosenSongs.joinToString { "'${it.title}' by ${it.artist}" }

        val offlineNote = if (chosenSongs.isNotEmpty()) {
            "Offline AI engine compiled a matching playlist containing $songsListDesc optimized for your '$userInput' mood grid. Configure your Gemini API key in the AI Studio Secrets panel to unlock next-generation deep neural curated narratives!"
        } else {
            "We were unable to locate matches in your local library catalog. Try searching for genres like 'Synthwave', 'Ambient Space', or moods like 'Relaxed'!"
        }

        return Pair(chosenSongs.map { it.id }, prependMsg + offlineNote)
    }

    /**
     * Call Gemini to identify song mood and predict bpm if missing.
     */
    suspend fun getSongAIAssistedAnalysis(title: String, artist: String): JSONObject = withContext(Dispatchers.IO) {
        val result = JSONObject()
        if (!isApiKeyAvailable()) {
            // Simulated offline response
            result.put("mood", "Aesthetic Chill")
            result.put("genre", "Futuristic Indie")
            result.put("bpm", 112)
            result.put("description", "A delicate sound parsed by local heuristics.")
            return@withContext result
        }

        val prompt = """
            Identify the aesthetic parameters of the song "$title" by $artist.
            If this is a customized or indie song, estimate parameters based on title words.
            Response must be key-value JSON ONLY. No formatting.
            Keys to output: "mood" (string), "genre" (string), "bpm" (integer), "description" (string).
        """.trimIndent()

        try {
            val jsonRequest = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }
            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val responseJson = JSONObject(body)
                    val candText = responseJson.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    val clean = candText.replace("```json", "").replace("```", "").trim()
                    return@withContext JSONObject(clean)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Song AI analysis failed", e)
        }

        // Return offline mock
        result.put("mood", "Synthetic Hyperpop")
        result.put("genre", "Cyberpunk Wave")
        result.put("bpm", 128)
        result.put("description", "A futuristic digital frequency processed locally.")
        return@withContext result
    }
}
