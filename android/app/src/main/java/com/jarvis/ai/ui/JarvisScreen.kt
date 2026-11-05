package com.jarvis.ai.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.ai.ChatMessage
import com.jarvis.ai.JarvisViewModel
import com.jarvis.ai.Status
import com.jarvis.ai.data.SearchResult

private val JarvisBlue = Color(0xFF00E5FF)
private val JarvisPurple = Color(0xFF9D4EDD)
private val Glass = Color(0x2216213A)
private val DeepBlack = Color(0xFF05060A)

@Composable
fun JarvisApp(vm: JarvisViewModel = viewModel()) {
    val messages by vm.messages.collectAsState()
    val status by vm.status.collectAsState()
    val memories by vm.memories.collectAsState()
    var input by remember { mutableStateOf("") }
    var showMemory by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.toggleListening()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = JarvisBlue,
            secondary = JarvisPurple,
            background = DeepBlack,
            surface = DeepBlack,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = DeepBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
                Header(status, onMemory = { showMemory = true })
                AiCore(status, Modifier.align(Alignment.CenterHorizontally).size(140.dp))
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    items(messages, key = { it.id }) { MessageBubble(it) }
                }
                InputBar(
                    input = input,
                    onInput = { input = it },
                    onSend = {
                        vm.send(input)
                        input = ""
                    },
                    listening = status == Status.LISTENING,
                    onMic = {
                        if (status == Status.LISTENING) {
                            vm.toggleListening()
                        } else {
                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                )
            }
        }
    }

    if (showMemory) {
        MemoryDialog(
            memories = memories,
            onDelete = vm::deleteMemory,
            onDismiss = { showMemory = false },
        )
    }
}

@Composable
private fun Header(status: Status, onMemory: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "JARVIS",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = JarvisBlue,
            )
            Text(statusText(status), style = MaterialTheme.typography.bodySmall, color = JarvisPurple)
        }
        TextButton(onClick = onMemory) { Text("Memory", color = JarvisBlue) }
    }
}

private fun statusText(s: Status): String = when (s) {
    Status.IDLE -> "Ready"
    Status.LISTENING -> "Listening…"
    Status.THINKING -> "Thinking…"
    Status.SPEAKING -> "Speaking…"
}

@Composable
private fun AiCore(status: Status, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "core")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "rotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (status == Status.IDLE) 1800 else 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * 0.8f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                listOf(JarvisBlue.copy(alpha = 0.5f), Color.Transparent),
                center = c,
                radius = r * 1.4f,
            ),
            radius = r * 1.4f,
            center = c,
        )
        drawCircle(color = JarvisPurple.copy(alpha = 0.6f), radius = r, center = c, style = Stroke(3.dp.toPx()))
        rotate(rotation, c) {
            drawArc(
                color = JarvisBlue,
                startAngle = 0f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(c.x - r * 1.2f, c.y - r * 1.2f),
                size = Size(r * 2.4f, r * 2.4f),
                style = Stroke(4.dp.toPx()),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White, JarvisBlue, Color.Transparent),
                center = c,
                radius = r * 0.6f,
            ),
            radius = r * 0.6f,
            center = c,
        )
    }
}

@Composable
private fun MessageBubble(m: ChatMessage) {
    val isUser = m.role == "user"
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        val label = when (m.source) {
            "search" -> "🌐 Live search results"
            "gemini" -> "✨ Gemini AI"
            else -> null
        }
        if (!isUser && label != null) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = JarvisBlue)
        }
        Box(
            Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isUser) JarvisPurple.copy(alpha = 0.35f) else Glass)
                .padding(12.dp),
        ) {
            Text(m.text)
        }
        m.links.forEach { LinkCard(it) }
    }
}

@Composable
private fun LinkCard(r: SearchResult) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier
            .padding(vertical = 4.dp)
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Glass)
            .clickable { uriHandler.openUri(r.link) }
            .padding(10.dp),
    ) {
        Text(
            r.title,
            color = JarvisBlue,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            r.snippet,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            r.link,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InputBar(
    input: String,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit,
    listening: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInput,
            singleLine = true,
            placeholder = { Text("Hinglish / English mein likho…") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onMic,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (listening) JarvisPurple else JarvisBlue,
            ),
        ) {
            Text(if (listening) "■" else "🎤")
        }
        Spacer(Modifier.width(6.dp))
        Button(onClick = onSend, enabled = input.isNotBlank()) { Text("Send") }
    }
}

@Composable
private fun MemoryDialog(memories: List<String>, onDelete: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Saved memories") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (memories.isEmpty()) {
                    Text("Abhi koi memory nahi. Chat mein 'remember ...' likho.")
                }
                memories.forEachIndexed { index, memory ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(memory, Modifier.weight(1f))
                        TextButton(onClick = { onDelete(index) }) { Text("Delete") }
                    }
                }
            }
        },
    )
}
