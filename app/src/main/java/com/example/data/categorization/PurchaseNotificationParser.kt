package com.example.data.categorization

import com.example.data.model.PaymentSource
import java.util.Locale
import kotlin.math.roundToLong

data class ParsedPurchase(
    val merchant: String,
    val amountCents: Long,
    val paymentSource: PaymentSource,
    val rawSnippet: String
)

object PurchaseNotificationParser {

    private val walletPackages = setOf(
        "com.google.android.apps.walletnfcrel",
        "com.google.android.apps.nbu.paisa.user"
    )

    private val smsPackages = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms"
    )

    private val ignoreKeywords = listOf(
        "otp", "one-time password", "verification code", "security code", "login code",
        "2fa", "declined", "failed", "unsuccessful", "refunded", "credited to your",
        "salary credited", "deposit received", "promo", "cashback earned", "statement ready",
        "due date", "minimum payment"
    )

    private val spendSignalKeywords = listOf(
        "spent", "paid", "purchase", "purchased", "charged", "charge", "payment",
        "debited", "debit", "transaction", "tap", "tapped", "google pay", "google wallet",
        "card ending", "credit card", "visa", "mastercard", "amex", "pos", "approved",
        "used for", "used at", "authorized"
    )

    private val amountRegex = Regex(
        """(?i)(?:USD|PHP|EUR|GBP|SGD|AUD|CAD|JPY|Php|[$₱€£¥])\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""
    )

    private val postCurrencyAmountRegex = Regex(
        """(?i)([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:USD|PHP|EUR|GBP|SGD|AUD|CAD)"""
    )

    private val merchantPatterns = listOf(
        Regex("""(?i)\b(?:at|to|from|merchant:?|store:?)\s+([A-Za-z0-9&'.,\-/# ]{2,45}?)(?:\s+(?:on|for|using|via|with|card|ending|ref|was|is|balance|avail)\b|[.!?\n•|]|$)"""),
        Regex("""(?i)\bpaid\s+([A-Za-z0-9&'.,\-/# ]{2,40}?)\s+(?:[$₱€£¥]|USD|PHP|EUR|GBP)"""),
        Regex("""(?i)^([A-Za-z0-9&'.,\-/# ]{2,40}?)\s*[-•|]\s*(?:[$₱€£¥]|USD|PHP|EUR|GBP)""")
    )

    fun parseNotification(
        packageName: String,
        title: String,
        body: String
    ): ParsedPurchase? {
        val combined = listOf(title.trim(), body.trim())
            .filter { it.isNotBlank() }
            .joinToString(" — ")
        if (combined.isBlank()) return null

        val lower = combined.lowercase(Locale.US)
        if (ignoreKeywords.any { lower.contains(it) }) return null

        val isWalletPkg = packageName in walletPackages
        val isSmsPkg = packageName in smsPackages
        val hasSpendSignal = spendSignalKeywords.any { lower.contains(it) }
        if (!isWalletPkg && !hasSpendSignal) return null

        val amountCents = extractAmountCents(combined) ?: return null
        if (amountCents <= 0L) return null

        val merchant = extractMerchant(title, body, isWalletPkg)
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown Store"

        val source = when {
            isWalletPkg || lower.contains("google pay") || lower.contains("google wallet") || lower.contains("gpay") ->
                PaymentSource.GOOGLE_PAY
            lower.contains("credit card") || lower.contains("card ending") ||
                lower.contains("visa") || lower.contains("mastercard") || lower.contains("amex") ->
                PaymentSource.CREDIT_CARD
            isSmsPkg -> PaymentSource.SMS_ALERT
            else -> PaymentSource.CREDIT_CARD
        }

        return ParsedPurchase(
            merchant = merchant.take(120),
            amountCents = amountCents,
            paymentSource = source,
            rawSnippet = combined.take(500)
        )
    }

    fun parseRawText(snippet: String): ParsedPurchase? {
        val trimmed = snippet.trim()
        if (trimmed.isBlank()) return null
        val lower = trimmed.lowercase(Locale.US)
        if (ignoreKeywords.any { lower.contains(it) }) return null
        val amountCents = extractAmountCents(trimmed) ?: return null
        if (amountCents <= 0L) return null
        val merchant = extractMerchant("", trimmed, isWalletPkg = false) ?: "Store Purchase"
        val source = when {
            lower.contains("google pay") || lower.contains("google wallet") -> PaymentSource.GOOGLE_PAY
            lower.contains("card") || lower.contains("visa") || lower.contains("mastercard") -> PaymentSource.CREDIT_CARD
            else -> PaymentSource.SMS_ALERT
        }
        return ParsedPurchase(
            merchant = merchant.take(120),
            amountCents = amountCents,
            paymentSource = source,
            rawSnippet = trimmed.take(500)
        )
    }

    private fun extractAmountCents(text: String): Long? {
        val match = amountRegex.find(text) ?: postCurrencyAmountRegex.find(text) ?: return null
        val numericStr = match.groupValues.getOrNull(1)?.replace(",", "") ?: return null
        val value = numericStr.toDoubleOrNull() ?: return null
        return (value * 100.0).roundToLong()
    }

    private fun extractMerchant(title: String, body: String, isWalletPkg: Boolean): String? {
        val combined = "$title $body".trim()
        for (pattern in merchantPatterns) {
            val match = pattern.find(combined)
            val candidate = match?.groupValues?.getOrNull(1)?.trim()
                ?.trimEnd('.', ',', '-', ':')
                ?.replace(Regex("\\s+"), " ")
            if (!candidate.isNullOrBlank() && !isGenericTitle(candidate)) {
                return candidate
            }
        }
        if (isWalletPkg && title.isNotBlank() && !isGenericTitle(title)) {
            return title.trim().take(60)
        }
        return null
    }

    private fun isGenericTitle(text: String): Boolean {
        val lower = text.trim().lowercase(Locale.US)
        return lower in setOf(
            "google wallet", "google pay", "wallet", "payment alert", "transaction alert",
            "card alert", "bank alert", "purchase alert", "new message", "sms", "messages"
        )
    }
}
