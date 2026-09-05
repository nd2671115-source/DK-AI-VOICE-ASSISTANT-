package com.example.engine

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.model.AppInfo
import com.example.data.model.ContactInfo
import com.example.data.model.DeviceStatusInfo
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeviceActionsManager {

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val appList = mutableListOf<AppInfo>()

        for (resolveInfo in resolveInfos) {
            val appName = resolveInfo.loadLabel(pm).toString()
            val packageName = resolveInfo.activityInfo.packageName
            val icon = resolveInfo.loadIcon(pm)
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            // Exclude our own app from search results to avoid self-launch loops
            if (packageName != context.packageName) {
                appList.add(AppInfo(appName = appName, packageName = packageName, icon = icon, isSystemApp = isSystem))
            }
        }
        return appList.sortedBy { it.appName.lowercase() }
    }

    fun findAppByName(context: Context, query: String): AppInfo? {
        val cleanQuery = query.lowercase().trim()
        val apps = getInstalledApps(context)

        // 1. Exact match
        apps.firstOrNull { it.appName.lowercase() == cleanQuery }?.let { return it }

        // 2. Contains match
        apps.firstOrNull { it.appName.lowercase().contains(cleanQuery) || cleanQuery.contains(it.appName.lowercase()) }?.let { return it }

        // 3. Known app aliases (Hindi / English / short names)
        val aliasMap = mapOf(
            "yt" to listOf("youtube"),
            "whatsapp" to listOf("whatsapp", "व्हाट्सएप", "watsapp", "watsap"),
            "insta" to listOf("instagram", "इन्स्टाग्राम"),
            "instagram" to listOf("instagram", "इन्स्टाग्राम"),
            "spotify" to listOf("spotify", "स्पॉटिफाई"),
            "chatgpt" to listOf("chatgpt", "chat gpt", "openai"),
            "telegram" to listOf("telegram", "टेलीग्राम"),
            "gmail" to listOf("gmail", "google mail", "email", "मेल"),
            "chrome" to listOf("chrome", "google chrome", "browser", "इंटरनेट"),
            "maps" to listOf("maps", "google maps", "मैप", "नक्शा"),
            "camera" to listOf("camera", "कैमरा", "cam"),
            "gallery" to listOf("gallery", "photos", "गैलरी", "फोटो"),
            "settings" to listOf("settings", "सेटिंग्स", "setting"),
            "calculator" to listOf("calculator", "कैलकुलेटर", "calc"),
            "clock" to listOf("clock", "alarm", "घड़ी")
        )

        for ((target, aliases) in aliasMap) {
            if (aliases.any { cleanQuery.contains(it) }) {
                apps.firstOrNull { it.appName.lowercase().contains(target) || it.packageName.lowercase().contains(target) }?.let {
                    return it
                }
            }
        }

        return null
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun searchPlayStore(context: Context, appName: String) {
        val encoded = Uri.encode(appName)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$encoded&c=apps")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun openYouTube(context: Context, query: String? = null) {
        try {
            if (query.isNullOrBlank()) {
                val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                }
            }
            val targetUrl = if (!query.isNullOrBlank()) {
                "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
            } else {
                "https://www.youtube.com"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playMusic(context: Context, musicApp: String, songQuery: String) {
        val encodedSong = Uri.encode(songQuery)
        when (musicApp.lowercase()) {
            "spotify" -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$encodedSong")).apply {
                        setPackage("com.spotify.music")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to web or youtube
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$encodedSong")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                }
            }
            "youtube music" -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=$encodedSong")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    openYouTube(context, songQuery)
                }
            }
            else -> { // Default YouTube
                openYouTube(context, songQuery)
            }
        }
    }

    fun openChatGPT(context: Context) {
        val launched = launchApp(context, "com.openai.chatgpt")
        if (!launched) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchContacts(context: Context, query: String): List<ContactInfo> {
        val contacts = mutableListOf<ContactInfo>()
        try {
            val contentResolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")

            val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getString(idIdx) else ""
                    val name = if (nameIdx != -1) it.getString(nameIdx) else ""
                    val number = if (numberIdx != -1) it.getString(numberIdx) else ""
                    val photo = if (photoIdx != -1) it.getString(photoIdx) else null

                    if (number.isNotBlank() && contacts.none { c -> c.phoneNumber == number }) {
                        contacts.add(ContactInfo(id = id, name = name, phoneNumber = number, photoUri = photo))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contacts
    }

    fun openDialer(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber.trim())}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun prepareWhatsAppMessage(context: Context, phoneNumber: String?, message: String) {
        try {
            if (!phoneNumber.isNullOrBlank()) {
                val cleanNumber = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${URLEncoder.encode(message, "UTF-8")}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            // If WhatsApp not installed, fallback to SMS or generic share
            sendSms(context, phoneNumber, message)
        }
    }

    fun sendSms(context: Context, phoneNumber: String?, message: String) {
        try {
            val uri = if (!phoneNumber.isNullOrBlank()) {
                Uri.parse("smsto:${Uri.encode(phoneNumber)}")
            } else {
                Uri.parse("smsto:")
            }
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun searchGoogle(context: Context, query: String) {
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun openCamera(context: Context) {
        try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallback = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    fun openGallery(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallback = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    fun openSettings(context: Context, action: String = Settings.ACTION_SETTINGS) {
        try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun setTimer(context: Context, seconds: Int, label: String = "DK Assistant Timer"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun setAlarm(context: Context, hour: Int, minute: Int, label: String = "DK Assistant Alarm"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun openMaps(context: Context, query: String) {
        try {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun getDeviceStatusInfo(context: Context): DeviceStatusInfo {
        // Battery info
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // Time and Date
        val now = Date()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())

        // Network info
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        var isConnected = false
        var netType = "Offline"
        if (cm != null) {
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                isConnected = true
                netType = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    else -> "Connected"
                }
            }
        }

        return DeviceStatusInfo(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            timeFormatted = timeFormat.format(now),
            dateFormatted = dateFormat.format(now),
            isNetworkConnected = isConnected,
            networkType = netType
        )
    }
}
