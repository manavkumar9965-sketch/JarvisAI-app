package com.jarvis.ai.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings

/** Android intents se apps aur system screens kholta hai. Koi hidden action nahi. */
object PhoneControl {

    private val packages = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "chrome" to "com.android.chrome",
    )

    /** Agar text phone command tha to reply deta hai, warna null. */
    fun handle(context: Context, raw: String): String? {
        val text = raw.trim().lowercase()
        return when {
            text.startsWith("call ") -> {
                val number = text.removePrefix("call ").filter { it.isDigit() || it == '+' }
                if (number.isEmpty()) {
                    "Number batao, jaan."
                } else {
                    start(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")), "Dialer khol rahi hoon.")
                }
            }
            text.startsWith("open ") -> {
                val target = text.removePrefix("open ").trim()
                when (target) {
                    "settings" -> start(context, Intent(Settings.ACTION_SETTINGS), "Settings khol rahi hoon.")
                    "camera" -> start(
                        context,
                        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                        "Camera khol rahi hoon.",
                    )
                    "gallery" -> start(
                        context,
                        Intent(Intent.ACTION_VIEW).setType("image/*"),
                        "Gallery khol rahi hoon.",
                    )
                    else -> {
                        val pkg = packages[target]
                        if (pkg != null) launchPackage(context, pkg, target)
                        else "Sorry, '$target' ko abhi support nahi karti."
                    }
                }
            }
            else -> null
        }
    }

    private fun launchPackage(context: Context, pkg: String, label: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return "$label installed nahi lag raha."
        return start(context, intent, "$label khol rahi hoon! 🚀")
    }

    private fun start(context: Context, intent: Intent, okMessage: String): String {
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            okMessage
        } catch (e: Exception) {
            "Ye action abhi possible nahi hai: ${e.message}"
        }
    }
}
