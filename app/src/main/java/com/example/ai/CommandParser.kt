package com.example.ai

import com.example.data.model.ActionStatus
import com.example.data.model.ActionType

sealed interface ParsedCommand {
    data class LaunchApp(val appQuery: String) : ParsedCommand
    data class SearchPlayStore(val appName: String) : ParsedCommand
    data class SearchYouTube(val query: String) : ParsedCommand
    data class OpenYouTube(val placeholder: Boolean = true) : ParsedCommand
    data class PlayFavouriteSong(val placeholder: Boolean = true) : ParsedCommand
    data class SetFavouriteSong(val songName: String) : ParsedCommand
    data class OpenChatGPT(val placeholder: Boolean = true) : ParsedCommand
    data class MakeCall(val target: String) : ParsedCommand
    data class SendMessage(val target: String, val messageText: String, val isWhatsApp: Boolean) : ParsedCommand
    data class GoogleSearch(val query: String) : ParsedCommand
    data class OpenCamera(val placeholder: Boolean = true) : ParsedCommand
    data class OpenGallery(val placeholder: Boolean = true) : ParsedCommand
    data class OpenSettings(val settingsType: ActionType) : ParsedCommand
    data class SetTimer(val minutes: Int, val seconds: Int, val label: String) : ParsedCommand
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : ParsedCommand
    data class OpenMaps(val query: String) : ParsedCommand
    data class DeviceInfo(val queryType: String) : ParsedCommand
    data class ConversationalQuery(val rawText: String) : ParsedCommand
}

object CommandParser {

    fun parse(rawInput: String): ParsedCommand {
        var text = rawInput.trim()

        // Strip wake words / prefixes like "hey dk", "dk", "hello dk", "ok dk", "सुनो dk"
        val wakePrefixes = listOf(
            "hey dk", "he dk", "hi dk", "hello dk", "ok dk", "okay dk",
            "dk", "डीके", "hey assistant", "assistant", "bhai"
        )

        for (prefix in wakePrefixes) {
            val lower = text.lowercase()
            if (lower.startsWith("$prefix ")) {
                text = text.substring(prefix.length).trim()
                break
            } else if (lower.startsWith("$prefix, ")) {
                text = text.substring(prefix.length + 1).trim()
                break
            } else if (lower == prefix) {
                return ParsedCommand.ConversationalQuery("Namaste! Main DK Assistant hoon. Main aapki kya madad kar sakta hoon?")
            }
        }

        val lower = text.lowercase()

        // 1. Favourite song setup: "Set favourite song Kesariya", "favourite song set karo Kesariya"
        if (lower.contains("set favourite song") || lower.contains("set favorite song") ||
            lower.contains("favourite song set karo") || lower.contains("favorite song set karo") ||
            lower.contains("mera favourite song hai") || lower.contains("mera favorite song hai")
        ) {
            val songName = extractSongNameFromSet(text)
            if (songName.isNotBlank()) {
                return ParsedCommand.SetFavouriteSong(songName)
            }
        }

        // 2. Play favourite song: "DK mera favourite song play karo", "play favourite song", "favourite song bajao"
        if (lower.contains("favourite song") || lower.contains("favorite song") || lower.contains("pasandeeda geet") || lower.contains("manpasand song")) {
            if (lower.contains("play") || lower.contains("chalao") || lower.contains("kholo") || lower.contains("bajao") || lower.contains("suno") || lower.contains("sunao")) {
                return ParsedCommand.PlayFavouriteSong()
            }
        }

        // 3. YouTube specific
        if (lower.contains("youtube") || lower.contains("yt") || lower.contains("यूट्यूब")) {
            val query = extractYouTubeQuery(text)
            return if (query.isNotBlank()) {
                ParsedCommand.SearchYouTube(query)
            } else {
                ParsedCommand.OpenYouTube()
            }
        }

        // 4. ChatGPT specific
        if (lower.contains("chatgpt") || lower.contains("chat gpt") || lower.contains("openai")) {
            if (lower.contains("kholo") || lower.contains("open") || lower.contains("start") || lower.contains("chalao")) {
                return ParsedCommand.OpenChatGPT()
            }
        }

        // 5. Phone Call: "Rahul ko call karo", "Call Rahul", "Call 9876543210"
        if (lower.contains("call karo") || lower.contains("call lagao") || lower.startsWith("call ") || lower.contains("ko phone karo") || lower.contains("ko dial karo")) {
            val target = extractCallTarget(text)
            if (target.isNotBlank()) {
                return ParsedCommand.MakeCall(target)
            }
        }

        // 6. Messaging: WhatsApp or SMS
        // "Rahul ko WhatsApp message bhejo hello bhai", "Rahul ko message bhejo main aa raha hoon"
        if (lower.contains("message bhejo") || lower.contains("msg bhejo") || lower.contains("send message") || lower.contains("send whatsapp") || lower.contains("whatsapp karo")) {
            val isWhatsApp = lower.contains("whatsapp") || lower.contains("वाट्सएप") || lower.contains("व्हाट्सएप")
            val (target, messageText) = extractMessageTargetAndContent(text)
            if (target.isNotBlank()) {
                return ParsedCommand.SendMessage(target, messageText, isWhatsApp)
            }
        }

        // 7. Timer: "10 minute ka timer lagao", "5 minute timer", "set timer for 10 minutes"
        if (lower.contains("timer")) {
            val (min, sec) = extractTimerDuration(lower)
            if (min > 0 || sec > 0) {
                return ParsedCommand.SetTimer(min, sec, "DK Assistant Timer")
            }
        }

        // 8. Alarm: "7 baje alarm lagao", "set alarm for 6:30 am", "subah 6 baje alarm"
        if (lower.contains("alarm") || lower.contains("अलार्म")) {
            val (hour, minute) = extractAlarmTime(lower)
            return ParsedCommand.SetAlarm(hour, minute, "DK Assistant Alarm")
        }

        // 9. Camera: "Camera kholo", "open camera", "photo khicho"
        if (lower.contains("camera") || lower.contains("कैमरा") || lower.contains("photo khicho") || lower.contains("take photo")) {
            if (lower.contains("kholo") || lower.contains("open") || lower.contains("chalao") || lower.contains("khicho") || lower.contains("take")) {
                return ParsedCommand.OpenCamera()
            }
        }

        // 10. Gallery: "Gallery kholo", "open gallery", "photos dikhao", "photos open karo"
        if (lower.contains("gallery") || lower.contains("गैलरी") || lower.contains("photos dikhao") || lower.contains("photos kholo")) {
            return ParsedCommand.OpenGallery()
        }

        // 11. Settings: WiFi, Bluetooth, Apps, System
        if (lower.contains("setting") || lower.contains("सेटिंग")) {
            return when {
                lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("वाईफाई") -> ParsedCommand.OpenSettings(ActionType.SETTINGS_WIFI)
                lower.contains("bluetooth") || lower.contains("ब्लूटूथ") -> ParsedCommand.OpenSettings(ActionType.SETTINGS_BLUETOOTH)
                lower.contains("app") || lower.contains("applications") -> ParsedCommand.OpenSettings(ActionType.SETTINGS_APPS)
                else -> ParsedCommand.OpenSettings(ActionType.SETTINGS_SYSTEM)
            }
        }

        // 12. Device Info: Battery, Time, Date, Internet
        if (lower.contains("battery") || lower.contains("चार्ज") || lower.contains("charging")) {
            return ParsedCommand.DeviceInfo("battery")
        }
        if (lower.contains("time kya hua") || lower.contains("what time") || lower.contains("kitne baje") || lower.contains("samay kya")) {
            return ParsedCommand.DeviceInfo("time")
        }
        if (lower.contains("date kya hai") || lower.contains("today's date") || lower.contains("aaj kaun si tarikh") || lower.contains("aaj kaun sa din")) {
            return ParsedCommand.DeviceInfo("date")
        }
        if (lower.contains("internet") || lower.contains("network") || lower.contains("wifi connected") || lower.contains("net chal raha")) {
            return ParsedCommand.DeviceInfo("network")
        }

        // 13. Maps / Navigation: "Delhi ka map kholo", "Nearest petrol pump search karo", "Google Maps kholo"
        if (lower.contains("map") || lower.contains("maps") || lower.contains("नक्शा") || lower.contains("navigate") || lower.contains("rasta dikhao") || lower.contains("nearest")) {
            val query = extractMapsQuery(text)
            return ParsedCommand.OpenMaps(query.ifBlank { "current location" })
        }

        // 14. Google Search explicitly: "Google par search karo...", "Search karo...", "Google search..."
        if (lower.startsWith("google par search karo ") || lower.startsWith("search karo ") || lower.startsWith("google search ") || lower.startsWith("search on google ")) {
            val q = text.replace(Regex("^(google par search karo|search karo|google search|search on google|search)\\s*", RegexOption.IGNORE_CASE), "").trim()
            if (q.isNotBlank()) {
                return ParsedCommand.GoogleSearch(q)
            }
        }

        // 15. Generic App Launch: "[App] kholo", "open [App]", "[App] app open karo", "[App] chalao"
        val appQuery = extractAppLaunchQuery(text)
        if (appQuery.isNotBlank()) {
            return ParsedCommand.LaunchApp(appQuery)
        }

        // Default: Conversational query (handled by Gemini / AI or smart offline fallback)
        return ParsedCommand.ConversationalQuery(text)
    }

    private fun extractSongNameFromSet(text: String): String {
        return text.replace(Regex("(?i)(set favourite song|set favorite song|favourite song set karo|favorite song set karo|mera favourite song hai|mera favorite song hai|ko favourite banao|ko favorite song set karo)"), "")
            .replace(":", "")
            .trim()
    }

    private fun extractYouTubeQuery(text: String): String {
        var clean = text.replace(Regex("(?i)(youtube par search karo|search karo youtube par|youtube search karo|search on youtube|search youtube for|youtube par search|youtube par|youtube pe|youtube open karo|youtube kholo|open youtube)"), "").trim()
        clean = clean.replace(Regex("(?i)(search|kholo|open|chalao|dikhao|play)"), "").trim()
        return clean
    }

    private fun extractCallTarget(text: String): String {
        var clean = text.replace(Regex("(?i)(call karo|call lagao|ko call karo|ko call lagao|ko phone karo|ko dial karo|call|dial|phone karo)"), "").trim()
        clean = clean.replace(Regex("(?i)^(to|a|ko)\\s*"), "").trim()
        return clean
    }

    private fun extractMessageTargetAndContent(text: String): Pair<String, String> {
        // e.g. "Rahul ko WhatsApp message bhejo main 10 minute me aa raha hoon"
        // e.g. "Rahul ko message bhejo hello bhai"
        var clean = text.replace(Regex("(?i)(whatsapp message bhejo|message bhejo|whatsapp msg bhejo|msg bhejo|send whatsapp message to|send message to|send whatsapp to|whatsapp karo)"), "|SPLIT|").trim()
        val parts = clean.split("|SPLIT|")
        if (parts.size >= 2) {
            val target = parts[0].replace(Regex("(?i)(ko|to)\\s*$"), "").trim()
            val msg = parts[1].replace(Regex("(?i)^(ki|that|:)\\s*"), "").trim()
            return Pair(target, msg)
        }
        return Pair(text, "")
    }

    private fun extractTimerDuration(lower: String): Pair<Int, Int> {
        // e.g. "10 minute ka timer", "5 minute", "30 second"
        val minMatcher = Regex("(\\d+)\\s*(minute|min|मिनट)").find(lower)
        val secMatcher = Regex("(\\d+)\\s*(second|sec|सेकंड)").find(lower)

        val minutes = minMatcher?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val seconds = secMatcher?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

        if (minutes == 0 && seconds == 0) {
            val digitOnly = Regex("(\\d+)").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            if (digitOnly > 0) return Pair(digitOnly, 0)
        }
        return Pair(minutes, seconds)
    }

    private fun extractAlarmTime(lower: String): Pair<Int, Int> {
        // e.g. "7 baje", "6:30", "subah 6 baje", "evening 8 baje"
        val isPm = lower.contains("pm") || lower.contains("shaam") || lower.contains("evening") || lower.contains("raat") || lower.contains("dopahar")
        val colonMatcher = Regex("(\\d{1,2}):(\\d{2})").find(lower)
        if (colonMatcher != null) {
            var hour = colonMatcher.groupValues[1].toIntOrNull() ?: 7
            val min = colonMatcher.groupValues[2].toIntOrNull() ?: 0
            if (isPm && hour < 12) hour += 12
            return Pair(hour, min)
        }

        val digitMatcher = Regex("(\\d{1,2})\\s*(baje|o'clock|am|pm)?").find(lower)
        var hour = digitMatcher?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 7
        if (isPm && hour < 12) hour += 12
        return Pair(hour, 0)
    }

    private fun extractMapsQuery(text: String): String {
        return text.replace(Regex("(?i)(ka map kholo|ka naksha dikhao|map kholo|maps kholo|open map|open maps|ka rasta dikhao|search karo|navigate to|dikhao)"), "").trim()
    }

    private fun extractAppLaunchQuery(text: String): String {
        val lower = text.lowercase()
        val patterns = listOf(
            Regex("(?i)^open\\s+([a-zA-Z0-9\\s]+?)(?:\\s+app)?$"),
            Regex("(?i)^launch\\s+([a-zA-Z0-9\\s]+?)(?:\\s+app)?$"),
            Regex("(?i)^([a-zA-Z0-9\\s]+?)(?:\\s+app)?\\s+(?:kholo|open karo|chalao|start karo)$"),
            Regex("(?i)^([a-zA-Z0-9\\s]+?)\\s+kholo$"),
            Regex("(?i)^([a-zA-Z0-9\\s]+?)\\s+chalao$")
        )

        for (pattern in patterns) {
            val match = pattern.find(text.trim())
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotBlank() && candidate.length > 1) {
                    return candidate
                }
            }
        }
        return ""
    }
}
