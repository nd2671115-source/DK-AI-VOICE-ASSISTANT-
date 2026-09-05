package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionStatus
import com.example.data.model.ActionType
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.ui.theme.ActionCardBackground
import com.example.ui.theme.AssistantBubbleBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.LaserPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UserBubbleBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: ChatMessage,
    onConfirmAction: (ChatMessage) -> Unit = {},
    onCancelAction: (ChatMessage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // DK Avatar Icon
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "DK Assistant",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) UserBubbleBackground else AssistantBubbleBackground,
                border = BorderStroke(
                    1.dp,
                    if (isUser) ElectricViolet.copy(alpha = 0.35f) else NeonCyan.copy(alpha = 0.35f)
                ),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeStr,
                        color = TextMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            // Interactive Action Confirmation Card
            if (message.actionType != ActionType.NONE && message.actionType != ActionType.SET_FAVOURITE_SONG) {
                Spacer(modifier = Modifier.height(6.dp))
                ActionCard(
                    message = message,
                    onConfirm = { onConfirmAction(message) },
                    onCancel = { onCancelAction(message) }
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    message: ChatMessage,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val icon: ImageVector = when (message.actionType) {
        ActionType.PHONE_CALL -> Icons.Default.Call
        ActionType.SEND_WHATSAPP, ActionType.SEND_SMS -> Icons.Default.Chat
        ActionType.APP_LAUNCH, ActionType.PLAY_STORE_SEARCH -> Icons.Default.Shop
        ActionType.YOUTUBE_SEARCH, ActionType.PLAY_FAVOURITE_SONG -> Icons.Default.PlayArrow
        ActionType.GOOGLE_SEARCH -> Icons.Default.Search
        ActionType.CAMERA -> Icons.Default.CameraAlt
        ActionType.GALLERY -> Icons.Default.MusicNote
        ActionType.SETTINGS_SYSTEM, ActionType.SETTINGS_WIFI, ActionType.SETTINGS_BLUETOOTH, ActionType.SETTINGS_APPS -> Icons.Default.Settings
        ActionType.TIMER, ActionType.ALARM -> Icons.Default.Alarm
        ActionType.MAPS_SEARCH -> Icons.Default.Map
        ActionType.DEVICE_INFO -> Icons.Default.Info
        else -> Icons.Default.SmartToy
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        colors = CardDefaults.cardColors(containerColor = ActionCardBackground),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = message.actionTitle ?: "Action",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )

                // Status Indicator
                when (message.actionStatus) {
                    ActionStatus.EXECUTED -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Executed", tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            Text("Done", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    ActionStatus.CANCELLED -> {
                        Text("Cancelled", color = TextMuted, fontSize = 11.sp)
                    }
                    ActionStatus.PENDING_CONFIRMATION -> {
                        Text("Pending", color = GlowAmber, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    else -> {}
                }
            }

            // If pending confirmation (e.g. Call or Message), show Confirm/Cancel buttons
            if (message.actionStatus == ActionStatus.PENDING_CONFIRMATION) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkSurfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.testTag("action_cancel_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("action_confirm_button")
                    ) {
                        Icon(
                            imageVector = if (message.actionType == ActionType.PHONE_CALL) Icons.Default.Call else Icons.Default.Send,
                            contentDescription = "Confirm",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (message.actionType == ActionType.PHONE_CALL) "Dial Call" else if (message.actionType == ActionType.PLAY_STORE_SEARCH) "Open Store" else "Send",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
