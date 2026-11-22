package com.idlix.data.local

import android.content.Context
import android.content.SharedPreferences
import com.idlix.data.model.IdlixAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object IdlixAccountStore {

    private const val PREFS_NAME = "idlix_account_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_EMAIL = "email"
    private const val KEY_USERNAME = "username"
    private const val KEY_PLAN = "plan"
    private const val KEY_STATUS = "status"
    private const val KEY_LAST_UPDATED = "last_updated"

    @Volatile
    private var prefs: SharedPreferences? = null

    private val _accountFlow = MutableStateFlow(IdlixAccount())
    val account: StateFlow<IdlixAccount> = _accountFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs = p
                    _accountFlow.value = loadAccount(p)
                }
            }
        }
    }

    private fun loadAccount(p: SharedPreferences): IdlixAccount {
        return IdlixAccount(
            isLoggedIn = p.getBoolean(KEY_IS_LOGGED_IN, false),
            email = p.getString(KEY_EMAIL, "") ?: "",
            username = p.getString(KEY_USERNAME, "") ?: "",
            plan = p.getString(KEY_PLAN, "") ?: "",
            status = p.getString(KEY_STATUS, "Belum terhubung") ?: "Belum terhubung",
            lastUpdated = p.getLong(KEY_LAST_UPDATED, 0L)
        )
    }

    fun saveAccount(account: IdlixAccount) {
        _accountFlow.value = account
        prefs?.edit()
            ?.putBoolean(KEY_IS_LOGGED_IN, account.isLoggedIn)
            ?.putString(KEY_EMAIL, account.email)
            ?.putString(KEY_USERNAME, account.username)
            ?.putString(KEY_PLAN, account.plan)
            ?.putString(KEY_STATUS, account.status)
            ?.putLong(KEY_LAST_UPDATED, account.lastUpdated)
            ?.apply()
    }

    fun clear() {
        val empty = IdlixAccount(status = "Belum terhubung")
        _accountFlow.value = empty
        prefs?.edit()?.clear()?.apply()
    }
}
