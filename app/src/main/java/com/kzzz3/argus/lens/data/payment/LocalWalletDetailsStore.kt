package com.kzzz3.argus.lens.data.payment

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalWalletDetailsStore(
    context: Context,
    private val gson: Gson,
) {
    private val preferences = context.applicationContext.getSharedPreferences("argus-lens-wallet-details", Context.MODE_PRIVATE)
    private val historyType = object : TypeToken<List<PaymentHistoryEntry>>() {}.type

    fun loadHistory(accountId: String): List<PaymentHistoryEntry>? {
        if (accountId.isBlank()) return null
        val raw = preferences.getString(historyKey(accountId), null) ?: return null
        return runCatching { gson.fromJson<List<PaymentHistoryEntry>>(raw, historyType) }.getOrNull()
    }

    fun saveHistory(accountId: String, history: List<PaymentHistoryEntry>) {
        if (accountId.isBlank()) return
        preferences.edit()
            .putString(historyKey(accountId), gson.toJson(history, historyType))
            .apply()
    }

    fun loadReceipt(paymentId: String): PaymentReceipt? {
        if (paymentId.isBlank()) return null
        val raw = preferences.getString(receiptKey(paymentId), null) ?: return null
        return runCatching { gson.fromJson(raw, PaymentReceipt::class.java) }.getOrNull()
    }

    fun saveReceipt(receipt: PaymentReceipt) {
        preferences.edit()
            .putString(receiptKey(receipt.paymentId), gson.toJson(receipt))
            .apply()
    }

    fun upsertHistoryEntry(accountId: String, entry: PaymentHistoryEntry) {
        val existing = loadHistory(accountId).orEmpty()
        val next = listOf(entry) + existing.filterNot { it.paymentId == entry.paymentId }
        saveHistory(accountId, next)
    }

    private fun historyKey(accountId: String): String = "history:${accountId.trim()}"

    private fun receiptKey(paymentId: String): String = "receipt:${paymentId.trim()}"
}
