package com.interdep.interdepmobile.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interdep.interdepmobile.ui.components.PremiumButton
import com.interdep.interdepmobile.ui.components.PremiumTopBar
import com.interdep.interdepmobile.ui.components.getUrl
import com.interdep.interdepmobile.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.DriverManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AtualizarBeneficiamentoScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estados da Tela
    var valorInput by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }

    // Tipos de Operação
    val tiposOperacao = listOf("Valor Fixo", "Acrésc. (%)", "Desconto (%)")
    var tipoSelecionado by remember { mutableStateOf(tiposOperacao[0]) }

    // Seleção de Banco (Único)
    val bancosDestino = listOf("Braketube", "Diametal")
    var bancoSelecionado by remember { mutableStateOf(bancosDestino[0]) }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Atualizar Beneficiamento",
                subtitle = "Valor beneficiamento",
                icon = Icons.Default.Build,
                onBack = onFinish
            )
        },
        containerColor = Slate100
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Seleção do Tipo de Operação
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tipo de Operação", fontWeight = FontWeight.Bold, color = Navy900)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tiposOperacao.forEach { tipo ->
                            FilterChip(
                                selected = (tipoSelecionado == tipo),
                                onClick = { tipoSelecionado = tipo },
                                label = { Text(tipo) },
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

            // 2. Campo de Valor Dinâmico
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    val labelDinâmico = if (tipoSelecionado == "Valor Fixo") "Valor (R$)" else "Percentual (%)"
                    val iconeDinâmico = if (tipoSelecionado == "Valor Fixo") Icons.Default.Edit else Icons.Default.Percent

                    Text(labelDinâmico, fontWeight = FontWeight.Bold, color = Navy900)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = valorInput,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                                valorInput = it
                            }
                        },
                        label = { Text(if (tipoSelecionado == "Valor Fixo") "Ex: 0.50" else "Ex: 15.5") },
                        leadingIcon = { Icon(iconeDinâmico, contentDescription = null, tint = Navy700) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Navy700,
                            focusedLabelColor = Navy700
                        )
                    )
                }
            }

            // 3. Seleção de Banco Alvo (Único)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Banco de Destino", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp)
                    Text("Selecione qual empresa receberá a atualização", color = Slate500, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))

                    // Botões de Seleção de Banco
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bancosDestino.forEach { banco ->
                            FilterChip(
                                selected = (bancoSelecionado == banco),
                                onClick = { bancoSelecionado = banco },
                                label = { Text(banco) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Navy700,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate100
                                ),
                                border = null
                            )
                        }
                    }

                    // Aviso Dinâmico do Grupo Afetado
                    Spacer(Modifier.height(8.dp))
                    val avisoGrupos = if (bancoSelecionado == "Braketube") "Grupos afetados: '07' e '08'" else "Grupo afetado: '01'"
                    Text(avisoGrupos, color = Navy700, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    Divider(Modifier.padding(vertical = 12.dp), color = Slate100)

                    // Aviso Brasfit (Fixo/Obrigatório)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Slate500, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Brasfit (Obrigatório)", fontWeight = FontWeight.Bold, color = Slate700, fontSize = 14.sp)
                            Text("Atualizado automaticamente como espelho.", color = Slate500, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 4. Botão de Execução
            PremiumButton(
                text = if (carregando) "Atualizando..." else "Atualizar Valores",
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (carregando) Slate500 else Emerald500,
                icon = Icons.Default.Check
            ) {
                val valorFormatado = valorInput.replace(",", ".")

                if (valorFormatado.isBlank() || valorFormatado.toDoubleOrNull() == null) {
                    Toast.makeText(context, "Informe um valor numérico válido", Toast.LENGTH_SHORT).show()
                    return@PremiumButton
                }

                carregando = true
                scope.launch(Dispatchers.IO) {
                    val sucesso = executarUpdateBeneficiamento(valorFormatado, tipoSelecionado, bancoSelecionado)

                    launch(Dispatchers.Main) {
                        carregando = false
                        if (sucesso) {
                            Toast.makeText(context, "Valores atualizados com sucesso!", Toast.LENGTH_LONG).show()
                            valorInput = "" // Limpa o campo após sucesso
                        } else {
                            Toast.makeText(context, "Erro ao atualizar o banco de dados", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}

// --- LÓGICA JDBC ATUALIZADA ---
private fun executarUpdateBeneficiamento(
    valorStr: String,
    tipoOperacao: String,
    bancoDestino: String
): Boolean {
    return try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")

        // 1. Constrói a cláusula SET baseada no tipo de operação
        val setClause = when (tipoOperacao) {
            "Acréscimo (%)" -> "VAL_BENEF = ISNULL(VAL_BENEF, 0) + (ISNULL(VAL_BENEF, 0) * ($valorStr / 100.0))"
            "Desconto (%)"  -> "VAL_BENEF = ISNULL(VAL_BENEF, 0) - (ISNULL(VAL_BENEF, 0) * ($valorStr / 100.0))"
            else            -> "VAL_BENEF = $valorStr" // Novo Valor Fixo
        }

        // 2. Define a Query e o Banco Alvo baseado na seleção única
        val (querySQL, urlAlvo) = if (bancoDestino == "Braketube") {
            "UPDATE MATERIAL SET $setClause WHERE COD_GRUPO IN ('07', '08')" to getUrl("BrakeTube")
        } else {
            "UPDATE MATERIAL SET $setClause WHERE COD_GRUPO = '01'" to getUrl("Diametal")
        }

        // 3. Executa a Query no Banco Selecionado
        executarQuery(urlAlvo, querySQL)

        // 4. Executa a mesma Query no Banco Mestre (Brasfit)
        executarQuery(getUrl("Brasfit"), querySQL)

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// Função auxiliar para conexões JDBC
private fun executarQuery(url: String, sql: String) {
    DriverManager.getConnection(url).use { conn ->
        conn.createStatement().use { st ->
            st.executeUpdate(sql)
        }
    }
}