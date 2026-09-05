package com.example.data.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false
)

data class ContactInfo(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

data class DeviceStatusInfo(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val timeFormatted: String,
    val dateFormatted: String,
    val isNetworkConnected: Boolean,
    val networkType: String
)
