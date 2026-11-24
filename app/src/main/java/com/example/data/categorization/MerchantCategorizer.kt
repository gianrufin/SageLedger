package com.example.data.categorization

import android.util.Log
import com.example.data.model.MerchantRule
import com.example.data.model.SpendingCategory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

data class CategorizationResult(
    val category: SpendingCategory,
    val usedAiFallback: Boolean
)

class MerchantCategorizer {

    private val aiModel by lazy {
        val categoryEnumValues = SpendingCategory.entries.map { it.name }
        val schema = Schema.obj(
            properties = mapOf(
                "category" to Schema.enumeration(
                    values = categoryEnumValues,
                    description = "Best matching spending category for the store or merchant"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    fun categorizeLocal(
        merchant: String,
        rawSnippet: String? = null,
        userRules: List<MerchantRule> = emptyList()
    ): SpendingCategory? {
        val normalizedMerchant = merchant.lowercase(Locale.US).trim()
        val combined = "$normalizedMerchant ${rawSnippet.orEmpty().lowercase(Locale.US)}"

        // 1. User-learned overrides take highest precedence (longest keyword first)
        val matchedUserRule = userRules
            .sortedByDescending { it.merchantKeyword.length }
            .firstOrNull { rule ->
                val kw = rule.merchantKeyword.lowercase(Locale.US).trim()
                kw.isNotEmpty() && normalizedMerchant.contains(kw)
            }
        if (matchedUserRule != null) {
            return matchedUserRule.categoryEnum
        }

        // 2. Deterministic store dictionary rules
        for ((category, keywords) in builtInCategoryRules) {
            if (keywords.any { combined.contains(it) }) {
                return category
            }
        }
        return null
    }

    suspend fun categorizeWithFallback(
        merchant: String,
        rawSnippet: String? = null,
        userRules: List<MerchantRule> = emptyList()
    ): CategorizationResult {
        categorizeLocal(merchant, rawSnippet, userRules)?.let { matched ->
            return CategorizationResult(category = matched, usedAiFallback = false)
        }

        return try {
            val prompt = buildString {
                append("Classify this purchase merchant into exactly one spending category.\n")
                append("Merchant: ").append(merchant.trim()).append("\n")
                append("Categories: ")
                append("FOOD (restaurants, cafes, fast food, food delivery), ")
                append("GROCERIES (supermarkets, fresh markets, warehouse clubs), ")
                append("TRANSPORT_FUEL (gas stations, fuel, tolls, parking, ride-hailing, transit), ")
                append("CONVENIENCE (convenience stores, mini-marts), ")
                append("GADGETS (electronics, gadget stores, phone/computer shops, accessories), ")
                append("UTILITIES (electric, water, internet, mobile plans, streaming subscriptions), ")
                append("SHOPPING (apparel, department stores, online marketplaces, home goods), ")
                append("OTHER (anything else).")
            }

            val response = withTimeout(30_000L) {
                aiModel.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                val json = JSONObject(text)
                val catName = json.optString("category", SpendingCategory.OTHER.name)
                CategorizationResult(
                    category = SpendingCategory.fromRaw(catName),
                    usedAiFallback = true
                )
            } else {
                CategorizationResult(category = SpendingCategory.OTHER, usedAiFallback = false)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI fallback categorization failed for merchant '$merchant'", e)
            CategorizationResult(category = SpendingCategory.OTHER, usedAiFallback = false)
        }
    }

    companion object {
        private const val TAG = "MerchantCategorizer"

        private val builtInCategoryRules: List<Pair<SpendingCategory, List<String>>> = listOf(
            SpendingCategory.CONVENIENCE to listOf(
                "7-eleven", "7 eleven", "7eleven", "ministop", "uncle john", "familymart",
                "lawson", "circle k", "wawa", "sheetz", "alfamart", "convenience", "mini mart",
                "minimart", "quick stop", "am pm", "ampm"
            ),
            SpendingCategory.GADGETS to listOf(
                "apple store", "apple.com", "best buy", "samsung", "sony", "lenovo",
                "power mac", "beyond the box", "digital walker", "octagon", "micro center",
                "b&h", "newegg", "xiaomi", "abenson", "datablitz", "itech", "gadget",
                "electronics", "computer", "pc express", "cyberzone", "logitech", "anker"
            ),
            SpendingCategory.TRANSPORT_FUEL to listOf(
                "shell", "petron", "caltex", "chevron", "exxon", "mobil", "bp ", "cleanfuel",
                "seaoil", "phoenix", "unioil", "gas station", "fuel", "petrol", "diesel",
                "toll", "easytrip", "autosweep", "ez-pass", "ezpass", "fastrak", "sunpass",
                "uber", "lyft", "grab ", "angkas", "joyride", "metro", "mrt", "lrt",
                "transit", "parking", "valet", "airline", "skyway", "nlex", "slex"
            ),
            SpendingCategory.GROCERIES to listOf(
                "trader joe", "whole foods", "walmart", "target", "costco", "aldi", "kroger",
                "safeway", "publix", "wegmans", "sm supermarket", "sm hypermarket",
                "robinsons supermarket", "puregold", "landers", "s&r", "marketplace",
                "rustan's", "supermarket", "grocery", "groceries", "fresh market"
            ),
            SpendingCategory.FOOD to listOf(
                "jollibee", "mcdonald", "kfc", "burger king", "wendy", "popeyes", "chowking",
                "mang inasal", "greenwich", "shake shack", "chipotle", "subway", "taco bell",
                "chick-fil-a", "panda express", "starbucks", "coffee", "cafe", "bistro",
                "ramen", "sushi", "pizza", "bakery", "bakeshop", "grill", "kitchen",
                "diner", "restaurant", "eatery", "grabfood", "foodpanda", "doordash",
                "ubereats", "deliveroo", "tim hortons", "dunkin", "bo's coffee", "pickup coffee"
            ),
            SpendingCategory.UTILITIES to listOf(
                "electric", "meralco", "water", "maynilad", "manila water", "internet",
                "fiber", "converge", "pldt", "globe", "smart", "verizon", "at&t", "t-mobile",
                "comcast", "xfinity", "netflix", "spotify", "icloud", "google one",
                "youtube premium", "insurance", "utility", "bill", "power"
            ),
            SpendingCategory.SHOPPING to listOf(
                "amazon", "shopee", "lazada", "uniqlo", "zara", "h&m", "nike", "adidas",
                "ikea", "decathlon", "sephora", "watsons", "mercury drug", "cvs", "walgreens",
                "department store", "boutique", "apparel", "mall"
            )
        )
    }
}
