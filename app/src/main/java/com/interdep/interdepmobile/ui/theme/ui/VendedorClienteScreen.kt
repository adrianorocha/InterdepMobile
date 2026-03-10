package com.interdep.interdepmobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.interdep.interdepmobile.ui.theme.*
import com.interdep.interdepmobile.ui.theme.ui.PremiumButton
import com.interdep.interdepmobile.ui.theme.ui.PremiumSnackbar
import com.interdep.interdepmobile.ui.theme.ui.PremiumTopBar
import com.interdep.interdepmobile.ui.theme.ui.getUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.DriverManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendedorClienteScreen(dbName: String = "Brasfit", onDone: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    val sellers = listOf("Renan" to 59, "Gabriel" to 61, "Rose" to 148, "Matheus" to 794, "Cruz" to 530, "Adriano Almeida" to 9045)
    var expandedSeller by remember { mutableStateOf(false) }
    var selectedSellerName by remember { mutableStateOf("") }
    var cliente by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    val codV = sellers.find { it.first == selectedSellerName }?.second

    Scaffold(
        topBar = {
            PremiumTopBar(
                "Vínculo Vendedor",
                "Associar Cliente",
                Icons.Default.PersonAdd,
                onBack = onDone
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHost) { data ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    PremiumSnackbar(data)
                }
            }
        },
        containerColor = Slate100
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // Card de Formulário
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Dados do Vínculo", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Navy900)

                    // Dropdown estilizado
                    ExposedDropdownMenuBox(expanded = expandedSeller, onExpandedChange = { expandedSeller = !expandedSeller }) {
                        OutlinedTextField(
                            value = selectedSellerName, onValueChange = {}, readOnly = true,
                            label = { Text("Selecione o Vendedor") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSeller) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Blue600, unfocusedBorderColor = Slate500)
                        )
                        ExposedDropdownMenu(expanded = expandedSeller, onDismissRequest = { expandedSeller = false }) {
                            sellers.forEach { (name, _) ->
                                DropdownMenuItem(text = { Text(name) }, onClick = { selectedSellerName = name; expandedSeller = false })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = cliente, onValueChange = { cliente = it },
                        label = { Text("Código do Cliente") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Blue600, unfocusedBorderColor = Slate500)
                    )
                }
            }

            // Botão de Ação Full Width
            PremiumButton(
                "Salvar Vínculo",
                Modifier.fillMaxWidth(),
                Navy700,
                Icons.Default.Business
            ) {
                if (codV == null || cliente.isBlank()) {
                    scope.launch { snackbarHost.showSnackbar("Preencha todos os campos") }
                } else {
                    scope.launch(Dispatchers.IO) {
                        val qtd = upsertVendedorCliente(dbName, codV, cliente)

                        // Dispara a vibração na Thread Principal (UI)
                        launch(Dispatchers.Main) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }

                        // Mostra o aviso flutuante
                        launch {
                            snackbarHost.showSnackbar(if (qtd >= 0) "Sucesso! Registros afetados: $qtd" else "Erro ao gravar.")
                        }

                        // Limpa a tela instantaneamente
                        if (qtd >= 0) {
                            selectedSellerName = ""
                            cliente = ""
                        }
                    }
                }
            }
        }
    }
}

// Manter função upsertVendedorCliente aqui
fun upsertVendedorCliente(dbName: String, codVendedor: Int?, codCliente: String): Int {
    // ... sua lógica original MERGE ...
    var total = 0
    // Simulação rápida para compilar, use sua lógica completa
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.createStatement().use { stmt ->
                val sql = "MERGE INTO vendedor_cliente AS tgt USING (VALUES ($codVendedor, '$codCliente')) AS src(c_v, c_c) ON tgt.cod_cliente = src.c_c WHEN MATCHED THEN UPDATE SET tgt.cod_vendedor = src.c_v WHEN NOT MATCHED THEN INSERT (cod_vendedor, cod_cliente) VALUES (src.c_v, src.c_c);"
                total = stmt.executeUpdate(sql)
            }
        }
    } catch (e: Exception) { e.printStackTrace(); total = -1 }
    return total
}