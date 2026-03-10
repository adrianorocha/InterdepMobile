package com.interdep.interdepmobile.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interdep.interdepmobile.ui.theme.*
import com.interdep.interdepmobile.ui.theme.ui.PremiumButton
import com.interdep.interdepmobile.ui.theme.ui.PremiumSnackbar
import com.interdep.interdepmobile.ui.theme.ui.PremiumTopBar
import com.interdep.interdepmobile.ui.theme.ui.ResponsiveDbSelector
import com.interdep.interdepmobile.ui.theme.ui.getUrl
import com.interdep.interdepmobile.util.toDataPremium
import com.interdep.interdepmobile.util.toPrecoFormatado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.sql.DriverManager

// --- Modelos ---
data class PedidoS(val numero: String, val data: String, val valor: BigDecimal, val temMovimentoFinanceiro: Boolean)
data class FornecedorGrupoS(val nomeFantasia: String, val razaoSocial: String, val codigo: String, val pedidos: MutableList<PedidoS>)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosServicoScreen(onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var selectedDb by remember { mutableStateOf("Brasfit") }
    var carregando by remember { mutableStateOf(true) }
    var fornecedores by remember { mutableStateOf(listOf<FornecedorGrupoS>()) }
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    val selectedPedidos = remember { mutableStateListOf<String>() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(selectedDb) {
        carregando = true
        fetchHierarquiaServico(selectedDb) { lista ->
            fornecedores = lista
            lista.forEach { expandedState[it.codigo] = false }
            carregando = false
        }
    }

    Scaffold(
        topBar = {
            Column(Modifier.background(Color.White)) {
                PremiumTopBar(
                    "Pedidos de Serviço",
                    selectedDb,
                    Icons.Default.Handyman,
                    onBack = onFinish
                )
                ResponsiveDbSelector(
                    selectedDb = selectedDb,
                    onDbSelected = { newDb -> selectedDb = newDb; selectedPedidos.clear() })
            }
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
        bottomBar = {
            Surface(shadowElevation = 16.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumButton(
                        "Atualizar",
                        Modifier.weight(1f),
                        Navy700,
                        Icons.Default.Refresh
                    ) {
                        carregando = true
                        selectedPedidos.clear()
                        scope.launch(Dispatchers.IO) {
                            fetchHierarquiaServico(selectedDb) { lista ->
                                fornecedores = lista; carregando = false
                            }
                        }
                    }
                    PremiumButton(
                        "Liberar (${selectedPedidos.size})",
                        Modifier.weight(1f),
                        Emerald500,
                        Icons.Default.Check
                    ) {
                        if (selectedPedidos.isEmpty()) {
                            scope.launch { snackbarHost.showSnackbar("Nenhum serviço selecionado.") }
                            return@PremiumButton
                        }
                        scope.launch(Dispatchers.IO) {
                            // 1. Executa a liberação no banco
                            val qtd =
                                liberarSelecionadosServico(selectedDb, selectedPedidos.toList())

                            // 2. Dispara o feedback tátil (vibração curta de confirmação)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            // 3. Mostra o Snackbar em um novo launch para não travar a execução abaixo
                            launch {
                                snackbarHost.showSnackbar(
                                    message = if (qtd > 0) "Pedido de Serviço | $qtd Serviços Liberados | Sucesso" else "Pedido de Serviço | Erro ao liberar | Erro",
                                    duration = SnackbarDuration.Short // Usa a duração curta que configuramos
                                )
                            }

                            // 4. Atualiza a lista imediatamente (sem esperar o snackbar sumir)
                            fetchHierarquiaServico(selectedDb) { lista ->
                                fornecedores = lista
                                carregando = false
                            }

                            // 5. Limpa a seleção
                            selectedPedidos.clear()
                        }
                    }
                }
            }
        },
        containerColor = Slate100
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (carregando) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Navy700)
            } else if (fornecedores.isEmpty()) {
                // --- ESTADO VAZIO ---
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.BuildCircle, null, modifier = Modifier.size(72.dp), tint = Color(0xFFDDD6FE))
                    Spacer(Modifier.height(16.dp))
                    Text("Sem Pendências", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    Text("Nenhum pedido de serviço aguardando liberação.", fontSize = 14.sp, color = Slate500, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(fornecedores, key = { it.codigo }) { grupo ->
                        FornecedorCardS(grupo, expandedState[grupo.codigo] ?: false, selectedPedidos) {
                            expandedState[grupo.codigo] = !(expandedState[grupo.codigo] ?: false)
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FornecedorCardS(grupo: FornecedorGrupoS, isExpanded: Boolean, selectedPedidos: SnapshotStateList<String>, onToggle: () -> Unit) {
    // Resumo Financeiro
    val qtdServicos = grupo.pedidos.size
    val valorTotal = grupo.pedidos.map { it.valor }.fold(BigDecimal.ZERO, BigDecimal::add)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }
    ) {
        Column {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // Ícone Roxo (referência a Serviços)
                Box(
                    modifier = Modifier.size(48.dp).background(Color(0xFFEDE9FE), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF7C3AED))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(grupo.nomeFantasia, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                    Text(grupo.razaoSocial, fontSize = 12.sp, color = Slate500, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$qtdServicos serviço(s)  •  ${valorTotal.toPrecoFormatado()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7C3AED)
                    )
                }
                Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Slate500)
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                    Divider(color = Slate100, modifier = Modifier.padding(bottom = 12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        grupo.pedidos.forEach { pedido ->
                            PedidoRowS(pedido, selectedPedidos.contains(pedido.numero)) { isChecked ->
                                if (isChecked) selectedPedidos.add(pedido.numero) else selectedPedidos.remove(pedido.numero)
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PedidoRowS(pedido: PedidoS, isChecked: Boolean, onChecked: (Boolean) -> Unit) {
    val temFinanceiro = pedido.temMovimentoFinanceiro

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = when {
                    !temFinanceiro -> Rose500.copy(alpha = 0.08f)
                    isChecked -> Emerald500.copy(alpha = 0.05f)
                    else -> Slate100.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = temFinanceiro) { onChecked(!isChecked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onChecked,
            enabled = temFinanceiro,
            colors = CheckboxDefaults.colors(checkedColor = Emerald500)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            if (temFinanceiro) {
                Text("Serviço #${pedido.numero}", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 14.sp)
            } else {
                Text("Serviço #${pedido.numero}", fontWeight = FontWeight.Bold, color = Rose500, fontSize = 14.sp)
                Text("Movimento Financeiro não gerado", fontWeight = FontWeight.SemiBold, color = Rose500, fontSize = 11.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(12.dp), tint = Slate500)
                Spacer(Modifier.width(4.dp))
                Text(pedido.data.toDataPremium(), fontSize = 12.sp, color = Slate500)
            }
        }

        if (temFinanceiro) {
            Text(
                text = pedido.valor.toPrecoFormatado(),
                fontWeight = FontWeight.Bold,
                color = if (isChecked) Emerald500 else Navy700,
                fontSize = 15.sp
            )
        } else {
            Icon(Icons.Default.ErrorOutline, null, tint = Rose500, modifier = Modifier.size(20.dp))
        }
    }
}

private fun fetchHierarquiaServico(dbName: String, onResult: (List<FornecedorGrupoS>) -> Unit) = Thread {
    val mapa = linkedMapOf<String, FornecedorGrupoS>()
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.createStatement().use { st ->

                // --- SQL ATUALIZADO COM VALIDAÇÃO DE MOVIMENTO FINANCEIRO ---
                st.executeQuery("""
                Select Distinct 
                   f.NOM_FANTASIA, 
                   f.COD_FORNECEDOR, 
                   f.RAZ_SOCIAL, 
                   p.NUM_PEDI_SERVICO,
                   CONVERT(varchar(10), p.DAT_EMISSAO, 103) as DATA_EMISSAO,
                   (IsNull(p.VAL_TOTAL,0)+IsNull(p.VAL_INSS,0)+IsNull(p.VAL_IRRF,0)+IsNull(p.VAL_PCC,0)+IsNull(p.VAL_ISS,0)) as VAL_TOTAL,
                   -- Verifica se existe registro na tabela movimento_financeiro
                   (CASE WHEN EXISTS (
                        SELECT 1 FROM movimento_financeiro mf 
                        WHERE mf.num_pedi_servico = p.NUM_PEDI_SERVICO
                   ) THEN 1 ELSE 0 END) AS TEM_MOV_FINANCEIRO
                From PEDIDO_SERVICO p 
                Join ITEM_PEDI_SERVICO i on(p.NUM_PEDI_SERVICO = i.NUM_PEDI_SERVICO)
                Join FORNECEDOR f on (p.COD_FORNECEDOR = f.COD_FORNECEDOR)
                Where (p.COD_SITUACAO = 'N') 
                Order by f.NOM_FANTASIA
                """.trimIndent()).use { rs ->
                    while (rs.next()) {
                        val cod = rs.getString("COD_FORNECEDOR").trim()
                        val grupo = mapa.getOrPut(cod) {
                            FornecedorGrupoS(rs.getString("NOM_FANTASIA").trim(), rs.getString("RAZ_SOCIAL").trim(), cod, mutableListOf())
                        }
                        val num = rs.getString("NUM_PEDI_SERVICO").trim()

                        // Extrai a validação do banco (1 = True, 0 = False)
                        val temMovimento = rs.getInt("TEM_MOV_FINANCEIRO") == 1

                        if (grupo.pedidos.none { it.numero == num }) {
                            grupo.pedidos.add(
                                PedidoS(
                                    numero = num,
                                    data = rs.getString("DATA_EMISSAO"),
                                    valor = rs.getBigDecimal("VAL_TOTAL"),
                                    temMovimentoFinanceiro = temMovimento // Salva no objeto
                                )
                            )
                        }
                    }
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    onResult(mapa.values.toList())
}.start()
private fun liberarSelecionadosServico(dbName: String, numeros: List<String>): Int {
    if (numeros.isEmpty()) return 0
    var total = 0
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.createStatement().use { st ->
                numeros.forEach { num ->
                    st.addBatch("UPDATE PEDIDO_SERVICO SET COD_SITUACAO = 'A' WHERE NUM_PEDI_SERVICO = '$num' AND COD_SITUACAO = 'N'")
                    total++
                }
                st.executeBatch()
            }
        }
    } catch (e: Exception) { e.printStackTrace(); total = -1 }
    return total
}