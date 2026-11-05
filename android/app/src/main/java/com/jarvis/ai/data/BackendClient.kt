package com.jarvis.ai.data

import com.jarvis.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatTurn(val role: String, val text: String)
data class SearchResult(val title: String, val link: String, val snippet: String)

/** Sirf apne backend se baat karta hai. API keys app mein kabhi nahi hoti. */
object BackendClient {

    private fun post(path: String, body: JSONObject): JSONObject {
        val conn = (URL(BuildConfig.BACKEND_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = JSONObject(text.ifBlank { "{}" })
            if (code !in 200..299) throw IllegalStateException(json.optString("error", "HTTP $code"))
            return json
        } finally {
            conn.disconnect()
        }
    }

    suspend fun chat(message: String, history: List<ChatTurn>, memories: List<String>): String =
        withContext(Dispatchers.IO) {
            val hist = JSONArray()
            history.takeLast(20).forEach {
                hist.put(JSONObject().put("role", it.role).put("text", it.text))
            }
            val body = JSONObject()
                .put("message", message)
                .put("history", hist)
                .put("memories", JSONArray(memories))
            post("/api/chat", body).getString("reply")
        }

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val arr = post("/api/search", JSONObject().put("query", query)).optJSONArray("items") ?: JSONArray()
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            SearchResult(o.optString("title"), o.optString("link"), o.optString("snippet"))
        }
    }
}
