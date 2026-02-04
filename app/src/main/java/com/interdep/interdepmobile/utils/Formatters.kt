package com.interdep.interdepmobile.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- Formatador de Moeda (R$ 12.000,00) ---
fun BigDecimal.toPrecoFormatado(): String {
    val ptBr = Locale("pt", "BR")
    return NumberFormat.getCurrencyInstance(ptBr).format(this)
}

// --- Formatador de Data (04 Fev 2025) ---
@RequiresApi(Build.VERSION_CODES.O)
fun String.toDataPremium(): String {
    return try {
        // 1. Define o formato que vem do seu banco SQL (dd/MM/yyyy)
        val formatadorEntrada = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        // 2. Define o formato de saída desejado
        val formatadorSaida = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("pt", "BR"))

        // 3. Converte
        val data = LocalDate.parse(this.trim(), formatadorEntrada)
        val dataFormatada = data.format(formatadorSaida)

        // 4. Ajuste manual: O padrão pt-BR retorna "fev", mas você quer "Fev" (Capitalizado)
        // E removemos o ponto final se houver (fev. -> Fev)
        dataFormatada
            .replace(".", "")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

    } catch (e: Exception) {
        // Se a data vier errada ou nula, retorna ela original para não quebrar o app
        this
    }
}