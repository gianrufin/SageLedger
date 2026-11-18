package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.CurrencyFormatter
import com.example.data.model.SpendingCategory

object SpendingAlertNotifier {

    const val CHANNEL_ID = "spending_alerts"
    const val EXTRA_TRANSACTION_ID = "extra_review_transaction_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_spending_alerts_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.channel_spending_alerts_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasPostPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showPurchaseCapturedAlert(
        context: Context,
        transactionId: String,
        merchant: String,
        amountCents: Long,
        category: SpendingCategory,
        currencyCode: String
    ) {
        if (!hasPostPermission(context)) return
        ensureChannel(context)

        val formattedAmount = CurrencyFormatter.formatCents(amountCents, currencyCode)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            transactionId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ledger)
            .setContentTitle("Made a purchase from $merchant?")
            .setContentText("$formattedAmount added to ${category.displayName}. Tap to review.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$formattedAmount was automatically recorded under ${category.displayName} and added to your monthly budget overview. Tap to confirm or change category."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stat_ledger, "Review", pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(transactionId.hashCode(), notification)
        }
    }
}
