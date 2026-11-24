package com.example

import com.example.core.LedgerAnalytics
import com.example.data.categorization.MerchantCategorizer
import com.example.data.categorization.PurchaseNotificationParser
import com.example.data.model.MerchantRule
import com.example.data.model.PaymentSource
import com.example.data.model.SpendingCategory
import com.example.data.model.SpendingTransaction
import com.example.data.remote.GitHubUpdateRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LedgerLogicUnitTest {

    @Test
    fun parsesGoogleWalletNotification() {
        val parsed = PurchaseNotificationParser.parseNotification(
            packageName = "com.google.android.apps.walletnfcrel",
            title = "Starbucks",
            body = "You paid $6.75 with Google Pay"
        )
        assertNotNull(parsed)
        assertEquals("Starbucks", parsed?.merchant)
        assertEquals(675L, parsed?.amountCents)
        assertEquals(PaymentSource.GOOGLE_PAY, parsed?.paymentSource)
    }

    @Test
    fun parsesBankSmsAlertAndIgnoresOtp() {
        val sms = PurchaseNotificationParser.parseNotification(
            packageName = "com.google.android.apps.messaging",
            title = "BankAlert",
            body = "You spent PHP 1,250.50 at Shell Station using Credit Card ending 4821."
        )
        assertNotNull(sms)
        assertEquals("Shell Station", sms?.merchant)
        assertEquals(125050L, sms?.amountCents)
        assertEquals(PaymentSource.CREDIT_CARD, sms?.paymentSource)

        val walletBullet = PurchaseNotificationParser.parseNotification(
            packageName = "com.google.android.apps.walletnfcrel",
            title = "7-Eleven",
            body = "$8.40 • Visa •••• 1234"
        )
        assertNotNull(walletBullet)
        assertEquals("7-Eleven", walletBullet?.merchant)
        assertEquals(840L, walletBullet?.amountCents)
        assertEquals(PaymentSource.GOOGLE_PAY, walletBullet?.paymentSource)

        val bankApp = PurchaseNotificationParser.parseNotification(
            packageName = "com.chase.sig.android",
            title = "Credit Card Transaction Alert",
            body = "Your card ending in 9912 was used for $42.19 at Trader Joe's on 11/18."
        )
        assertNotNull(bankApp)
        assertEquals("Trader Joe's", bankApp?.merchant)
        assertEquals(4219L, bankApp?.amountCents)
        assertEquals(PaymentSource.CREDIT_CARD, bankApp?.paymentSource)

        val otp = PurchaseNotificationParser.parseNotification(
            packageName = "com.google.android.apps.messaging",
            title = "BankAlert",
            body = "Your OTP verification code for $50.00 login is 839201."
        )
        assertNull(otp)
    }

    @Test
    fun categorizerMatchesStoresAndUserRules() {
        val categorizer = MerchantCategorizer()
        assertEquals(SpendingCategory.CONVENIENCE, categorizer.categorizeLocal("7-Eleven #123"))
        assertEquals(SpendingCategory.GADGETS, categorizer.categorizeLocal("Best Buy"))
        assertEquals(SpendingCategory.FOOD, categorizer.categorizeLocal("Jollibee"))
        assertEquals(SpendingCategory.TRANSPORT_FUEL, categorizer.categorizeLocal("Shell Station"))
        assertEquals(SpendingCategory.GROCERIES, categorizer.categorizeLocal("Trader Joe's"))

        val customRules = listOf(
            MerchantRule(id = "r1", merchantKeyword = "kissa corner", category = SpendingCategory.FOOD.name)
        )
        assertEquals(
            SpendingCategory.FOOD,
            categorizer.categorizeLocal("Kissa Corner Cafe", userRules = customRules)
        )
    }

    @Test
    fun gitHubSemverComparisonWorks() {
        assertTrue(GitHubUpdateRepository.isNewerVersion("v1.2.0", "1.0"))
        assertTrue(GitHubUpdateRepository.isNewerVersion("1.0.1", "1.0"))
        assertFalse(GitHubUpdateRepository.isNewerVersion("v1.0.0", "1.0"))
        assertFalse(GitHubUpdateRepository.isNewerVersion("v0.9.5", "1.0"))
    }

    @Test
    fun moshiParsesApkManifestAndReleaseDtos() {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val manifestJson = """
            {"versionName":"1.0.2","versionCode":3,"apkFile":"SageLedger-latest.apk","notes":"New build"}
        """.trimIndent()
        val manifest = moshi.adapter(com.example.data.remote.RepoApkManifestDto::class.java)
            .fromJson(manifestJson)
        assertNotNull(manifest)
        assertEquals("1.0.2", manifest?.versionName)
        assertEquals(3, manifest?.versionCode)
        assertEquals("SageLedger-latest.apk", manifest?.apkFile)
    }

    @Test
    fun parsesEWalletNotificationsAndSnippets() {
        val gcashNotif = PurchaseNotificationParser.parseNotification(
            packageName = "com.globe.gcash.android",
            title = "GCash Payment",
            body = "You have sent PHP 245.00 to Starbucks Reserve via QRPh."
        )
        assertNotNull(gcashNotif)
        assertEquals(24500L, gcashNotif?.amountCents)
        assertEquals(PaymentSource.E_WALLET, gcashNotif?.paymentSource)

        val mayaText = PurchaseNotificationParser.parseRawText(
            snippet = "Paid \$18.50 via Maya Wallet at 7-Eleven"
        )
        assertNotNull(mayaText)
        assertEquals("7-Eleven", mayaText?.merchant)
        assertEquals(1850L, mayaText?.amountCents)
        assertEquals(PaymentSource.E_WALLET, mayaText?.paymentSource)

        val paypalNotif = PurchaseNotificationParser.parseNotification(
            packageName = "com.paypal.android.p2pmobile",
            title = "PayPal",
            body = "You paid \$32.40 at Steam"
        )
        assertNotNull(paypalNotif)
        assertEquals("Steam", paypalNotif?.merchant)
        assertEquals(3240L, paypalNotif?.amountCents)
        assertEquals(PaymentSource.E_WALLET, paypalNotif?.paymentSource)
    }

    @Test
    fun savingsProjectionComputesExpectedMonthEndTrajectory() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        val now = cal.timeInMillis
        val tx = listOf(
            SpendingTransaction(
                id = "t1",
                merchant = "Grocery",
                amountCents = 100_000L,
                occurredAtMillis = now
            )
        )
        val proj = LedgerAnalytics.computeSavingsProjection(
            transactions = tx,
            monthlyIncomeCents = 500_000L,
            monthlyBudgetCents = 300_000L,
            nowMillis = now
        )
        assertEquals(30, proj.daysInMonth)
        assertEquals(10, proj.currentDayOfMonth)
        assertEquals(21, proj.remainingDaysInMonth)
        assertEquals(100_000L, proj.spentToDateCents)
        assertEquals(300_000L, proj.projectedMonthEndSpendCents)
        assertEquals(200_000L, proj.projectedSavingsCents)
        assertEquals(200_000L / 21L, proj.dailyAllowanceCents)
        assertEquals(9, proj.noSpendDaysThisMonth)
    }
}
