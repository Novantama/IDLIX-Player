package com.idlix.data.util

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import java.util.Locale

object LocaleHelper {
    /**
     * IDLIX is an Indonesian streaming platform.
     * The primary preferred subtitle language is always Bahasa Indonesia ("id").
     */
    fun getPreferredLanguage(): String = "id"

    fun isIndonesian(): Boolean = true
}

