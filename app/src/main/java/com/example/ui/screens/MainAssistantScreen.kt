package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AssistantMode
import com.example.ui.components.AiOrb
import com.example.ui.components.ChatBubble
import com.example.ui.components.QuickActionChips
import com.example.ui.components.VoiceWaveform
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DeepVoidBlack
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.LaserPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UserBubbleBackground
import com.example.viewmodel.MainViewModel

@Composable
fun MainAssistantScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val messages by viewModel.chatMessages.collectAsState()
    val status by viewModel.assistantStatus.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showInstalledApps by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Permission launcher for Mic, Contacts, Calls, Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            viewModel.startListening()
        }
    }

    fun requestVoiceAndListen() {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasAudio) {
            viewModel.toggleListening()
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Auto-scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepVoidBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- TOP BAR ---
            TopActionBar(
                statusText = status.statusText,
                mode = status.mode,
                onOpenApps = { showInstalledApps = true },
                onOpenSettings = { showSettings = true }
            )

            // --- MAIN CONTENT AREA ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    // Empty State: Hero Orb & Assistant Welcome
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AiOrb(
                            mode = status.mode,
                            size = 170.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        VoiceWaveform(
                            mode = status.mode,
                            maxHeight = 30.dp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "DK AI Assistant",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "Bolein: “Hey DK, YouTube kholo” ya “Rahul ko call karo”",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Features Badge Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, DarkSurfaceBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("⚡ Supported Voice Commands (Hindi & English):", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("• “DK mera favourite song play karo”", color = TextPrimary, fontSize = 12.sp)
                                Text("• “DK Rahul ko WhatsApp message bhejo hello”", color = TextPrimary, fontSize = 12.sp)
                                Text("• “DK Spotify / Camera / Settings kholo”", color = TextPrimary, fontSize = 12.sp)
                                Text("• “DK Battery kitni hai?” ya “10 min timer”", color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Conversation Stream
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Compact Orb Header when conversation is active
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceElevated)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AiOrb(
                                mode = status.mode,
                                size = 52.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = status.statusText,
                                    color = if (status.mode is AssistantMode.Listening) NeonCyan else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                VoiceWaveform(
                                    mode = status.mode,
                                    barCount = 12,
                                    maxHeight = 16.dp
                                )
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                ChatBubble(
                                    message = msg,
                                    onConfirmAction = { viewModel.confirmPendingAction(it) },
                                    onCancelAction = { viewModel.cancelPendingAction(it) }
                                )
                            }
                        }
                    }
                }
            }

            // --- QUICK SUGGESTIONS ---
            QuickActionChips(
                onChipClick = { cmd ->
                    viewModel.processUserQuery(cmd)
                }
            )

            // --- BOTTOM BAR (MIC + TEXT INPUT) ---
            BottomControlBar(
                textValue = textInput,
                onTextChange = { textInput = it },
                onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.processUserQuery(textInput)
                        textInput = ""
                        keyboardController?.hide()
                    }
                },
                isListening = status.mode is AssistantMode.Listening,
                onMicClick = {
                    requestVoiceAndListen()
                }
            )
        }
    }

    // Sheets / Dialogs
    if (showSettings) {
        VoiceSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
    }

    if (showInstalledApps) {
        InstalledAppsDialog(
            viewModel = viewModel,
            onDismiss = { showInstalledApps = false }
        )
    }
}

@Composable
fun TopActionBar(
    statusText: String,
    mode: AssistantMode,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Online Status Indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (mode) {
                            is AssistantMode.Listening -> NeonCyan
                            is AssistantMode.Speaking -> ElectricViolet
                            is AssistantMode.Thinking -> GlowAmber
                            else -> SuccessGreen
                        }
                    )
            )

            Text(
                text = "DK AI",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Surface(
                color = DarkSurfaceCard,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Text(
                    text = "ASSISTANT",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Action Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onOpenApps,
                modifier = Modifier.testTag("top_apps_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Installed Apps",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("top_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Voice Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun BottomControlBar(
    textValue: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    onMicClick: () -> Unit
) {
    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Text Input Field
            OutlinedTextField(
                value = textValue,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        text = if (isListening) "Listening to speech..." else "Type command or query...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input"),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                trailingIcon = {
                    if (textValue.isNotBlank()) {
                        IconButton(
                            onClick = onSend,
                            modifier = Modifier.testTag("send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkSurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DeepVoidBlack,
                    unfocusedContainerColor = DeepVoidBlack
                )
            )

            // Glowing Floating Microphone Button
            FloatingActionButton(
                onClick = onMicClick,
                containerColor = if (isListening) LaserPink else NeonCyan,
                contentColor = DeepVoidBlack,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("mic_fab_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop Listening" else "Start Voice Listening",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
