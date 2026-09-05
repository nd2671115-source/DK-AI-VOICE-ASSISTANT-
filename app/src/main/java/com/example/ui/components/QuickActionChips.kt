package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary

data class QuickSuggestion(
    val label: String,
    val commandText: String,
    val iconEmoji: String
)

@Composable
fun QuickActionChips(
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        QuickSuggestion("Favourite Song", "DK mera favourite song play karo", "🎵"),
        QuickSuggestion("YouTube", "YouTube kholo", "▶️"),
        QuickSuggestion("WhatsApp", "WhatsApp kholo", "💬"),
        QuickSuggestion("Battery Status", "Battery kitni hai?", "🔋"),
        QuickSuggestion("10 Min Timer", "10 minute ka timer lagao", "⏰"),
        QuickSuggestion("Camera", "Camera kholo", "📸"),
        QuickSuggestion("WiFi Settings", "WiFi settings kholo", "📶"),
        QuickSuggestion("ChatGPT", "ChatGPT kholo", "🤖"),
        QuickSuggestion("Time & Date", "Time kya hua hai?", "🕒"),
        QuickSuggestion("Search Google", "Google par search karo best smartphones", "🔍")
    )

    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { item ->
            AssistChip(
                onClick = { onChipClick(item.commandText) },
                label = {
                    Text(
                        text = "${item.iconEmoji} ${item.label}",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = DarkSurfaceCard
                ),
                border = BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.testTag("quick_chip_${item.label.lowercase().replace(" ", "_")}")
            )
        }
    }
}
