package com.interdep.interdepmobile.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interdep.interdepmobile.ui.theme.*
import com.interdep.interdepmobile.ui.theme.ui.PremiumButton
import com.interdep.interdepmobile.ui.theme.ui.PremiumSnackbar
import com.interdep.interdepmobile.ui.theme.ui.PremiumTopBar
import com.interdep.interdepmobile.ui.theme.ui.getUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.DriverManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransferenciaFuncionarioScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dbList = listOf("Brasfit", "Interdep", "Diametal", "BrakeTube")

    var matricula by remember { mutableStateOf("") }
    var bancoOrigem by remember { mutableStateOf(dbList[0]) }
    val bancosDestino = remember { mutableStateListOf<String>() }
    var carregando by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val snackbarHost = remember { SnackbarHostState() }

    // Se o banco de origem mudar e estiver nos destinos, remove ele dos destinos
    LaunchedEffect(bancoOrigem) {
        if (bancosDestino.contains(bancoOrigem)) {
            bancosDestino.remove(bancoOrigem)
        }
    }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Transferir Funcionário",
                subtitle = "Recursos Humanos",
                icon = Icons.Default.CompareArrows,
                onBack = onFinish
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Campo de Matrícula
            OutlinedTextField(
                value = matricula,
                onValueChange = { matricula = it.filter { char -> char.isDigit() } },
                label = { Text("Número do Funcionário (Matrícula)") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Badge,
                        contentDescription = null,
                        tint = Navy700
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy700,
                    focusedLabelColor = Navy700,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // 2. Seleção de Origem (Apenas um)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Banco de Origem",
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dbList.forEach { db ->
                            val isSelected = bancoOrigem == db
                            FilterChip(
                                selected = isSelected,
                                onClick = { bancoOrigem = db },
                                label = { Text(db) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Navy700,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate100
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            // 3. Seleção de Destino (Múltiplos)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Banco(s) de Destino",
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        fontSize = 14.sp
                    )
                    Text("Selecione um ou mais destinos", color = Slate500, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dbList.forEach { db ->
                            // Desabilita a opção se for o mesmo banco de origem
                            val isDisabled = bancoOrigem == db
                            val isSelected = bancosDestino.contains(db)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!isDisabled) {
                                        if (isSelected) bancosDestino.remove(db) else bancosDestino.add(
                                            db
                                        )
                                    }
                                },
                                label = { Text(db) },
                                enabled = !isDisabled,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate100,
                                    disabledContainerColor = Slate100.copy(alpha = 0.5f),
                                    disabledLabelColor = Slate500.copy(alpha = 0.5f)
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 4. Botão Executar
            PremiumButton(
                text = if (carregando) "Transferindo..." else "Executar Transferência",
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (carregando) Slate500 else Navy700,
                icon = Icons.Default.Send
            ) {
                if (matricula.isBlank()) {
                    Toast.makeText(context, "Informe o número do funcionário", Toast.LENGTH_SHORT)
                        .show()
                    return@PremiumButton
                }
                if (bancosDestino.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Selecione pelo menos um banco de destino",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@PremiumButton
                }

                carregando = true
                scope.launch(Dispatchers.IO) {
                    val sucesso = executarProcedureTransferencia(
                        bancoOrigem,
                        bancosDestino.toList(),
                        matricula.toInt()
                    )

                    launch(Dispatchers.Main) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        carregando = false
                        if (sucesso) {
                            matricula = ""
                            bancosDestino.clear()
                        }
                    }

                    // Dispara o PremiumSnackbar
                    launch {
                        snackbarHost.showSnackbar(
                            message = if (sucesso) "Transferência de Funcionário | Transferência realizada com sucesso! | Sucesso" else "Transferência de Funcionário | Erro ao transferir. Verifique sua conexão. | Erro",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }
}

// --- Lógica JDBC ---
private fun executarProcedureTransferencia(
    origem: String,
    destinos: List<String>,
    matricula: Int
): Boolean {
    return try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")

        // DICA: Estou conectando no banco de "Origem" para executar a procedure.
        // Se a sua procedure fica em um banco Master central, mude o getUrl(origem) para getUrl("NomeDoSeuMaster")
        DriverManager.getConnection(getUrl(origem)).use { conn ->

            // Prepara a chamada da Stored Procedure
            val call = conn.prepareCall("{call TransferirFuncionario(?, ?, ?)}")

            // Executa um loop para cada banco de destino selecionado
            for (destino in destinos) {
                call.setInt(1, matricula)
                call.setString(2, destino)
                call.setString(3, origem)

                call.execute() // Dispara a procedure no SQL Server
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}