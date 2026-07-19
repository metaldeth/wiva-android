package com.viwa.android.ui.screens.service.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viwa.android.data.network.NetworkTrafficChannel
import com.viwa.android.data.network.NetworkTrafficDirection
import com.viwa.android.ui.screens.service.ServiceViewModel
import com.viwa.android.ui.screens.service.serviceMenuSp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PAGE_SIZE = 20
private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

private val ShapeLabel = RoundedCornerShape(3.dp)
private val ShapeCard = RoundedCornerShape(6.dp)

@Stable
private data class WsLogItem(
    val id: Int,
    val time: String,
    val direction: NetworkTrafficDirection,
    val channel: NetworkTrafficChannel,
    val isJson: Boolean,
    val topicLabel: String,
    val formattedJson: String,
)

private enum class NetworkLogFilter { ALL, WS, HTTP }

@Composable
fun ViwaTelemetryWsLogTab(
    viewModel: ServiceViewModel,
) {
    val entries by viewModel.networkTrafficFlow.collectAsStateWithLifecycle()
    var activeFilter by rememberSaveable { mutableStateOf(NetworkLogFilter.ALL) }

    val filteredEntries =
        remember(entries, activeFilter) {
            when (activeFilter) {
                NetworkLogFilter.ALL -> entries
                NetworkLogFilter.WS -> entries.filter { it.channel == NetworkTrafficChannel.WS }
                NetworkLogFilter.HTTP -> entries.filter { it.channel == NetworkTrafficChannel.HTTP }
            }
        }

    val allReversed = remember(filteredEntries) { filteredEntries.asReversed() }
    val totalPages = maxOf(1, (allReversed.size + PAGE_SIZE - 1) / PAGE_SIZE)
    var currentPage by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(totalPages) {
        if (currentPage >= totalPages) currentPage = maxOf(0, totalPages - 1)
    }

    val pageItems = remember(allReversed, currentPage) {
        allReversed
            .drop(currentPage * PAGE_SIZE)
            .take(PAGE_SIZE)
            .map { entry ->
                val firstChar = entry.payload.trimStart().firstOrNull()
                val isJson = firstChar == '{' || firstChar == '['
                WsLogItem(
                    id = entry.id,
                    time = TIME_FORMAT.format(Date(entry.timestampMs)),
                    direction = entry.direction,
                    channel = entry.channel,
                    isJson = isJson,
                    topicLabel =
                        if (isJson) {
                            extractType(entry.payload) ?: "JSON"
                        } else {
                            entry.summary
                        },
                    formattedJson = if (isJson) prettyJson(entry.payload) else "",
                )
            }
    }

    val expandedIds = remember { mutableStateMapOf<Int, Boolean>() }

    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = scheme.background,
        contentColor = scheme.onBackground,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Логи сети",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall,
            )
            Button(
                onClick = {
                    viewModel.clearNetworkTrafficLog()
                    currentPage = 0
                    expandedIds.clear()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Очистить") }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = activeFilter == NetworkLogFilter.ALL,
                onClick = { activeFilter = NetworkLogFilter.ALL },
                label = { Text("ALL") },
            )
            FilterChip(
                selected = activeFilter == NetworkLogFilter.WS,
                onClick = { activeFilter = NetworkLogFilter.WS },
                label = { Text("WS") },
            )
            FilterChip(
                selected = activeFilter == NetworkLogFilter.HTTP,
                onClick = { activeFilter = NetworkLogFilter.HTTP },
                label = { Text("HTTP") },
            )
        }

        Spacer(Modifier.height(8.dp))

        if (allReversed.isEmpty()) {
            Text(
                "Нет сообщений по выбранному фильтру.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        PaginationBar(
            currentPage = currentPage,
            totalPages = totalPages,
            totalEntries = allReversed.size,
            onPrev = { if (currentPage > 0) currentPage-- },
            onNext = { if (currentPage < totalPages - 1) currentPage++ },
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(
                items = pageItems,
                key = { _, item -> item.id },
            ) { _, item ->
                val expanded = expandedIds[item.id] == true
                WsTrafficCard(
                    item = item,
                    expanded = expanded,
                    onToggle = { expandedIds[item.id] = !expanded },
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        }
    }
}

@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalEntries: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedButton(onClick = onPrev, enabled = currentPage > 0) {
            Text("‹ Пред", fontSize = serviceMenuSp(13))
        }
        Text(
            "стр ${currentPage + 1} / $totalPages  · всего $totalEntries",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = onNext, enabled = currentPage < totalPages - 1) {
            Text("След ›", fontSize = serviceMenuSp(13))
        }
    }
}

@Composable
private fun WsTrafficCard(
    item: WsLogItem,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dirColor =
        when (item.direction) {
            NetworkTrafficDirection.IN -> scheme.tertiary
            NetworkTrafficDirection.OUT -> scheme.primary
            NetworkTrafficDirection.SYSTEM -> scheme.secondary
        }
    val bgColorTarget =
        (when (item.direction) {
            NetworkTrafficDirection.IN -> scheme.tertiaryContainer
            NetworkTrafficDirection.OUT -> scheme.primaryContainer
            NetworkTrafficDirection.SYSTEM -> scheme.secondaryContainer
        }).copy(alpha = if (expanded) 0.95f else 0.72f)
    val payloadTextColor =
        if (bgColorTarget.luminance() > 0.5f) {
            Color(0xFF212121)
        } else {
            scheme.onSurface
        }
    val bgColor by animateColorAsState(
        targetValue = bgColorTarget,
        animationSpec = tween(durationMillis = 200),
        label = "cardBg",
    )
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "arrow",
    )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(ShapeCard)
                .background(bgColor)
                .then(if (item.isJson) Modifier.clickable(onClick = onToggle) else Modifier)
                .animateContentSize(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    val channelLabel =
                        when (item.channel) {
                            NetworkTrafficChannel.WS -> "WS"
                            NetworkTrafficChannel.HTTP -> "HTTP"
                        }
                    val channelColor =
                        when (item.channel) {
                            NetworkTrafficChannel.WS -> Color(0xFF2E7D32)
                            NetworkTrafficChannel.HTTP -> Color(0xFF1565C0)
                        }
                    Text(
                        channelLabel,
                        color = Color.White,
                        fontSize = serviceMenuSp(10),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier =
                            Modifier
                                .background(channelColor, ShapeLabel)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    val dirLabel =
                        when (item.direction) {
                            NetworkTrafficDirection.IN -> "IN"
                            NetworkTrafficDirection.OUT -> "OUT"
                            NetworkTrafficDirection.SYSTEM -> "SYS"
                        }
                    Text(
                        dirLabel,
                        color = dirColor,
                        fontSize = serviceMenuSp(10),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier =
                            Modifier
                                .background(dirColor.copy(alpha = 0.15f), ShapeLabel)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        item.time,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = serviceMenuSp(11),
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.topicLabel,
                        color = dirColor,
                        fontSize = serviceMenuSp(12),
                        fontWeight = if (item.isJson) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.isJson) {
                    Text(
                        "▼",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = serviceMenuSp(11),
                        modifier =
                            Modifier
                                .padding(start = 4.dp)
                                .graphicsLayer { rotationZ = arrowRotation },
                    )
                }
            }

            if (expanded && item.isJson) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = dirColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    item.formattedJson,
                    color = payloadTextColor,
                    fontSize = serviceMenuSp(11),
                    fontFamily = FontFamily.Monospace,
                    lineHeight = serviceMenuSp(16),
                )
            }
        }
    }
}

private fun extractType(json: String): String? {
    return try {
        val start = json.indexOf("\"type\"")
        if (start == -1) return null
        val colon = json.indexOf(':', start)
        if (colon == -1) return null
        val valStart = json.indexOfFirst(colon + 1) { it == '"' }
        if (valStart == -1) return null
        val valEnd = json.indexOf('"', valStart + 1)
        if (valEnd == -1) return null
        json.substring(valStart + 1, valEnd)
    } catch (_: Exception) {
        null
    }
}

private fun String.indexOfFirst(from: Int, predicate: (Char) -> Boolean): Int {
    for (i in from until length) if (predicate(this[i])) return i
    return -1
}

private fun prettyJson(raw: String): String {
    val sb = StringBuilder(raw.length * 2)
    var indent = 0
    var inString = false
    var prevChar = ' '
    for (ch in raw) {
        when {
            ch == '"' && prevChar != '\\' -> {
                inString = !inString
                sb.append(ch)
            }
            inString -> sb.append(ch)
            ch == '{' || ch == '[' -> {
                sb.append(ch).append('\n')
                indent++
                repeat(indent * 2) { sb.append(' ') }
            }
            ch == '}' || ch == ']' -> {
                sb.append('\n')
                indent--
                repeat(indent * 2) { sb.append(' ') }
                sb.append(ch)
            }
            ch == ',' -> {
                sb.append(ch).append('\n')
                repeat(indent * 2) { sb.append(' ') }
            }
            ch == ':' -> sb.append(": ")
            ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t' -> Unit
            else -> sb.append(ch)
        }
        if (!inString) prevChar = ch
    }
    return sb.toString()
}
