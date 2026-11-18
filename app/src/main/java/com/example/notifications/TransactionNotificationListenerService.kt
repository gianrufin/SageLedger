package com.example.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.categorization.MerchantCategorizer
import com.example.data.categorization.PurchaseNotificationParser
import com.example.data.repository.LedgerRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class TransactionNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { LedgerRepository(applicationContext) }
    private val categorizer by lazy { MerchantCategorizer() }
    private val recentFingerprints = ConcurrentHashMap<String, Long>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        SpendingAlertNotifier.ensureChannel(applicationContext)
        Log.d(TAG, "SageLedger notification listener connected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val pkg = sbn.packageName ?: return
        if (pkg == applicationContext.packageName) return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = notification.extras ?: return
        val (title, body) = extractTitleAndBody(extras)

        val parsed = PurchaseNotificationParser.parseNotification(pkg, title, body) ?: return
        val now = System.currentTimeMillis()
        val fingerprint = "${parsed.merchant.lowercase()}_${parsed.amountCents}"
        val previous = recentFingerprints[fingerprint]
        if (previous != null && (now - previous) < DEDUP_WINDOW_MS) {
            return
        }
        recentFingerprints[fingerprint] = now
        cleanupStaleFingerprints(now)

        val uid = Firebase.auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.d(TAG, "Ignoring notification capture while user is signed out.")
            return
        }

        serviceScope.launch {
            try {
                val userRules = repository.getMerchantRulesOnce(uid)
                val currency = repository.getCurrencyOnce(uid)
                val catResult = categorizer.categorizeWithFallback(
                    merchant = parsed.merchant,
                    rawSnippet = parsed.rawSnippet,
                    userRules = userRules
                )
                val saveResult = repository.addTransaction(
                    userId = uid,
                    merchant = parsed.merchant,
                    amountCents = parsed.amountCents,
                    category = catResult.category,
                    paymentSource = parsed.paymentSource,
                    note = "",
                    rawSnippet = parsed.rawSnippet,
                    occurredAtMillis = sbn.postTime.takeIf { it > 0L } ?: now,
                    needsReview = true
                )
                saveResult.onSuccess { txId ->
                    SpendingAlertNotifier.showPurchaseCapturedAlert(
                        context = applicationContext,
                        transactionId = txId,
                        merchant = parsed.merchant,
                        amountCents = parsed.amountCents,
                        category = catResult.category,
                        currencyCode = currency
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed processing captured payment notification", e)
            }
        }
    }

    private fun cleanupStaleFingerprints(now: Long) {
        if (recentFingerprints.size > 40) {
            recentFingerprints.entries.removeIf { (now - it.value) > DEDUP_WINDOW_MS }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "TxNotifListener"
        private const val DEDUP_WINDOW_MS = 15_000L

        internal fun extractTitleAndBody(extras: Bundle): Pair<String, String> {
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.mapNotNull { it?.toString()?.trim() }
                ?.filter { it.isNotEmpty() }
                ?.joinToString(" ")
                .orEmpty()

            val primaryBody = listOf(bigText, text, lines, subText)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" ")
            return title to primaryBody
        }

        fun isNotificationAccessEnabled(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val componentName = ComponentName(context, TransactionNotificationListenerService::class.java)
            return enabledListeners.split(':').any {
                it.equals(componentName.flattenToString(), ignoreCase = true) ||
                    it.equals(componentName.flattenToShortString(), ignoreCase = true)
            }
        }
    }
}
