package br.com.crescerbem

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.crescerbem.data.repo.ThemeMode
import br.com.crescerbem.ui.navigation.CrescerBemRoot
import br.com.crescerbem.ui.theme.CrescerBemTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as CrescerBemApp).container
        setContent {
            val mode by container.themeStore.mode.collectAsStateWithLifecycle()
            val activeBaby by container.babyRepository.activeBaby.collectAsStateWithLifecycle(null)
            val darkTheme = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Ícones da barra de status acompanham o tema do app, não só o do sistema.
            DisposableEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            CrescerBemTheme(darkTheme = darkTheme, colorIndex = activeBaby?.colorIndex ?: 0) {
                CrescerBemRoot(
                    container = container,
                    darkTheme = darkTheme,
                    onToggleTheme = {
                        container.themeStore.set(if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK)
                    },
                )
            }
        }
    }
}
