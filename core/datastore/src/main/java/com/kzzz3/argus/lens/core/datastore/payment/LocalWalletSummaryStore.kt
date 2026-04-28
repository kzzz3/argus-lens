package com.kzzz3.argus.lens.core.datastore.payment

import android.content.Context
import com.kzzz3.argus.lens.model.payment.WalletSummary

class LocalWalletSummaryStore(
    context: Context,
) : WalletSummaryCache {
    private val preferences = context.applicationContext.getSharedPreferences("argus-lens-wallet-summary", Context.MODE_PRIVATE)

    override fun load(accountId: String): WalletSummary? {
        if (accountId.isBlank()) return null
        val prefix = accountId.trim() + ":"
        val displayName = preferences.getString(prefix + DISPLAY_NAME_KEY, null) ?: return null
        val currency = preferences.getString(prefix + CURRENCY_KEY, null) ?: return null
        val balance = preferences.getString(prefix + BALANCE_KEY, null)?.toDoubleOrNull() ?: return null
        return WalletSummary(
            accountId = accountId.trim(),
            displayName = displayName,
            balance = balance,
            currency = currency,
        )
    }

    override fun save(summary: WalletSummary) {
        val prefix = summary.accountId.trim() + ":"
        preferences.edit()
            .putString(prefix + DISPLAY_NAME_KEY, summary.displayName)
            .putString(prefix + CURRENCY_KEY, summary.currency)
            .putString(prefix + BALANCE_KEY, summary.balance.toString())
            .apply()
    }

    override fun clear(accountId: String) {
        if (accountId.isBlank()) return
        val prefix = accountId.trim() + ":"
        preferences.edit()
            .remove(prefix + DISPLAY_NAME_KEY)
            .remove(prefix + CURRENCY_KEY)
            .remove(prefix + BALANCE_KEY)
            .apply()
    }

    private companion object {
        const val DISPLAY_NAME_KEY = "display_name"
        const val CURRENCY_KEY = "currency"
        const val BALANCE_KEY = "balance"
    }
}

interface WalletSummaryCache {
    fun load(accountId: String): WalletSummary?

    fun save(summary: WalletSummary)

    fun clear(accountId: String)
}
