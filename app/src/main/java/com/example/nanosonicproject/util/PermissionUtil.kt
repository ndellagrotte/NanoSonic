package com.example.nanosonicproject.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

object PermissionUtil {
    private const val TAG = "PermissionUtil"

    fun getAudioPermission(): String {
        val permission = Manifest.permission.READ_MEDIA_AUDIO
        Log.d(TAG, "Requesting permission: $permission")
        return permission
    }

    fun hasAudioPermission(context: Context): Boolean {
        val permission = getAudioPermission()
        val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Permission $permission is granted: $granted")
        return granted
    }

    fun getRequiredPermissions(): Array<String> {
        return arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    }

    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasAudioPermission(context)
    }

    fun getAudioPermissionRationale(): String {
        return "NanoSonic needs access to your audio files to display and play them with custom EQ profiles."
    }

    /**
     * Debug method to check permission status
     */
    fun debugPermissionStatus(context: Context): String {
        val permission = getAudioPermission()
        val status = ContextCompat.checkSelfPermission(context, permission)
        return when (status) {
            PackageManager.PERMISSION_GRANTED -> "GRANTED"
            PackageManager.PERMISSION_DENIED -> "DENIED"
            else -> "UNKNOWN ($status)"
        }
    }
}