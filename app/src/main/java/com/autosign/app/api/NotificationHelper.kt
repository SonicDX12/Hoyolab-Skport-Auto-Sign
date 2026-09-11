package com.autosign.app.api

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "autosign_results"
    private const val CHANNEL_NAME = "Sign-in Results"

    fun notify(context: Context, results: List<SignResult>) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        results.forEachIndexed { index, result ->
            val successCount = result.lines.count { it.second }
            val totalCount = result.lines.size

            val summary = if (successCount == totalCount) "All check-ins successful ✅"
                          else "$successCount / $totalCount successful"

            val bigText = result.lines.joinToString("\n") { (line, ok) ->
                "${if (ok) "✅" else "⚠️"} $line"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(result.accountNickname)
                .setContentText(summary)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            nm.notify(index + 1, notification)
        }
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}
