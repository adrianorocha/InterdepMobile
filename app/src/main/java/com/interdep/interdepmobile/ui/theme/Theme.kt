package com.interdep.interdepmobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- Definição das Cores do Tema Escuro ---
// No modo escuro, usamos tons mais suaves para evitar fadiga ocular
private val DarkColorScheme = darkColorScheme(
    primary = Blue600,        // Azul mais vibrante para contraste no escuro
    onPrimary = Color.White,
    secondary = Emerald500,   // Verde para destaques positivos
    tertiary = Slate500,
    background = Navy900,     // Fundo Azul Profundo (quase preto)
    surface = Navy700,        // Cards levemente mais claros
    onSurface = Slate100,     // Texto claro
    error = Rose500
)

// --- Definição das Cores do Tema Claro ---
// Visual limpo, profissional e com alto contraste
private val LightColorScheme = lightColorScheme(
    primary = Navy700,        // Azul Corporativo Sóbrio
    onPrimary = Color.White,
    secondary = Slate500,     // Cinza azulado para elementos secundários
    tertiary = Blue600,       // Azul vibrante para links/ações
    background = Slate100,    // Off-white (Gelo) para o fundo da tela
    surface = Color.White,    // Cards brancos puros
    onSurface = Navy900,      // Texto escuro (quase preto) para leitura
    error = Rose500,

    /* Configurações explícitas para garantir consistência */
    onBackground = Navy900,
    onSecondary = Color.White,
    onTertiary = Color.White
)

@Composable
fun InterdepMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // IMPORTANTE: Definimos false por padrão para garantir a identidade visual da empresa.
    // Se true, o app pegaria as cores do papel de parede do usuário (Material You).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Configuração da Barra de Status (Status Bar) para combinar com o tema
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Pinta a barra de status com a cor primária (Navy700 no claro, Navy900 no escuro)
            window.statusBarColor = colorScheme.primary.toArgb()

            // Define se os ícones da barra (bateria, hora) devem ser claros ou escuros
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Certifique-se de ter o Typography.kt configurado ou use o padrão
        content = content
    )
}