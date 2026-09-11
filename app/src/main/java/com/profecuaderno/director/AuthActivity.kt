package com.profecuaderno.director

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.profecuaderno.director.network.CentralBackend

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backend = CentralBackend(this)
        if (!backend.tokenStore.accessToken.isNullOrBlank()) {
            openDirector()
            return
        }

        setContent {
            val prefs = remember { getSharedPreferences("director_ui", MODE_PRIVATE) }
            val theme = remember {
                AgendaThemeStyle.entries.firstOrNull { it.key == prefs.getString("theme", null) }
                    ?: AgendaThemeStyle.MINT_LAVENDER
            }
            DirectorTheme(theme) {
                OnlineAuthScreen(
                    backend = backend,
                    onAuthenticated = { openDirector() }
                )
            }
        }
    }

    private fun openDirector() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
