package com.jarvis.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.data.BackendClient
import com.jarvis.ai.data.ChatTurn
import com.jarvis.ai.data.MemoryStore
import com.jarvis.ai.data.SearchResult
import com.jarvis.ai.device.PhoneControl
import com.jarvis.ai.device.VoiceController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Status { IDLE, LISTENING, THINKING, SPEAKING }

data class ChatMessage(
    val role: String,
    val text: String,
    val source: String = "gemini",
    val links: List<SearchResult> = emptyList(),
    val id: Long = System.nanoTime(),
)

class JarvisViewModel(app: Application) : AndroidViewModel(app) {

    private val memoryStore = MemoryStore(app)
    private val voice = VoiceController(app)

    private val memoryCmd = Regex("^(remember|yaad rakho)\\b\\s*:?\\s*", RegexOption.IGNORE_CASE)
    private val searchCmd = Regex("^search\\s+", RegexOption.IGNORE_CASE)

    private val _messages = MutableStateFlow(
        listOf(ChatMessage("assistant", "Hey jaan! ❤️ Main JARVIS hoon. Batao, kya help chahiye?", "system"))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _status = MutableStateFlow(Status.IDLE)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _memories = MutableStateFlow(memoryStore.all())
    val memories: StateFlow<List<String>> = _memories.asStateFlow()

    init {
        voice.onSpeakingChanged = { speaking ->
            if (speaking) {
                _status.value = Status.SPEAKING
            } else if (_status.value == Status.SPEAKING) {
                _status.value = Status.IDLE
            }
        }
    }

    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return

        val history = _messages.value.map { ChatTurn(it.role, it.text) }
        append(ChatMessage("user", text, "user"))
        voice.stop()

        when {
            memoryCmd.containsMatchIn(text) -> {
                val fact = text.replaceFirst(memoryCmd, "").trim()
                if (fact.isEmpty()) {
                    respond("Kya yaad rakhna hai? 'remember ...' ke baad likho.", "system")
                } else {
                    memoryStore.add(fact)
                    _memories.value = memoryStore.all()
                    respond("Theek hai, yaad rakh liya: $fact", "system")
                }
            }
            searchCmd.containsMatchIn(text) -> runSearch(text.replaceFirst(searchCmd, "").trim())
            else -> {
                val phoneReply = PhoneControl.handle(getApplication(), text)
                if (phoneReply != null) respond(phoneReply, "system")
                else askGemini(text, history)
            }
        }
    }

    fun toggleListening() {
        if (_status.value == Status.LISTENING) {
            voice.stopListening()
            _status.value = Status.IDLE
            return
        }
        voice.stop()
        _status.value = Status.LISTENING
        voice.listen(
            onResult = { spoken -> send(spoken) },
            onListeningChanged = { active ->
                if (!active && _status.value == Status.LISTENING) _status.value = Status.IDLE
            },
            onFail = { msg ->
                _status.value = Status.IDLE
                append(ChatMessage("assistant", msg, "system"))
            },
        )
    }

    fun stopSpeaking() = voice.stop()

    fun deleteMemory(index: Int) {
        memoryStore.delete(index)
        _memories.value = memoryStore.all()
    }

    private fun askGemini(text: String, history: List<ChatTurn>) {
        _status.value = Status.THINKING
        viewModelScope.launch {
            try {
                val reply = BackendClient.chat(text, history, memoryStore.all())
                respond(reply, "gemini")
            } catch (e: Exception) {
                respond("Backend se connect nahi ho paya: ${e.message}", "system")
            }
        }
    }

    private fun runSearch(query: String) {
        if (query.isEmpty()) {
            respond("Kya search karna hai? 'search ...' ke baad likho.", "system")
            return
        }
        _status.value = Status.THINKING
        viewModelScope.launch {
            try {
                val results = BackendClient.search(query)
                val header = if (results.isEmpty()) {
                    "Live search mein kuch nahi mila."
                } else {
                    "Live search results for \"$query\":"
                }
                respond(header, "search", results)
            } catch (e: Exception) {
                respond("Search fail ho gaya: ${e.message}", "system")
            }
        }
    }

    private fun respond(text: String, source: String, links: List<SearchResult> = emptyList()) {
        append(ChatMessage("assistant", text, source, links))
        _status.value = Status.IDLE
        if (source != "search") voice.speak(text)
    }

    private fun append(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    override fun onCleared() {
        voice.shutdown()
        super.onCleared()
    }
}
