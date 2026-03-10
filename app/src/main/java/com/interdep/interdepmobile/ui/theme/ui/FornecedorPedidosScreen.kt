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
import androidx.compose.ui.platform.LocalHapticFeedback
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.sql.DriverManager
import com.interdep.interdepmobile.util.toDataPremium
import com.interdep.interdepmobile.util.toPrecoFormatado

// --- Modelos ---
data class Pedido(val numero: String, val data: String, val valor: BigDecimal)
data class FornecedorGrupo(val nomeFantasia: String, val razaoSocial: String, val codigo: String, val pedidos: MutableList<Pedido>)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FornecedorPedidosScreen(onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var selectedDb by remember { mutableStateOf("Brasfit") }
    var carregando by remember { mutableStateOf(true) }
    var fornecedores by remember { mutableStateOf(listOf<FornecedorGrupo>()) }
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    val selectedPedidos = remember { mutableStateListOf<String>() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(selectedDb) {
        carregando = true
        fetchHierarquia(selectedDb) { lista ->
            fornecedores = lista
            lista.forEach { expandedState[it.codigo] = false }
            carregando = false
        }
    }

    Scaffold(
        topBar = {
            Column(Modifier.background(Color.White)) {
                PremiumTopBar(
                    "Pedidos de Compras",
                    selectedDb,
                    Icons.Default.ShoppingCart,
                    onBack = onFinish
                )
                ResponsiveDbSelector(
                    selectedDb = selectedDb,
                    onDbSelected = { newDb -> selectedDb = newDb; selectedPedidos.clear() }
                )
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
                            fetchHierarquia(selectedDb) { lista ->
                                fornecedores = lista
                                carregando = false
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
                            scope.launch { snackbarHost.showSnackbar("Nenhum pedido selecionado.") }
                            return@PremiumButton
                        }
                        scope.launch(Dispatchers.IO) {
                            val qtd = liberarSelecionados(selectedDb, selectedPedidos.toList())

                            // Dispara a vibração na Thread Principal (UI)
                            launch(Dispatchers.Main) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            // Mostra o Snackbar de forma independente
                            launch {
                                snackbarHost.showSnackbar(if (qtd > 0) "Pedidos de Compra | $qtd Pedidos Liberados | Sucesso" else "Pedidos de Compra | Erro ao liberar | Erro")
                            }

                            // Atualiza a lista imediatamente
                            fetchHierarquia(selectedDb) { lista ->
                                fornecedores = lista; carregando = false
                            }
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
                // --- NOVO: ESTADO VAZIO ---
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(72.dp), tint = Emerald500.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("Tudo Limpo!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    Text("Não há pedidos pendentes de liberação neste banco no momento.", fontSize = 14.sp, color = Slate500, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(fornecedores, key = { it.codigo }) { grupo ->
                        FornecedorCard(grupo, expandedState[grupo.codigo] ?: false, selectedPedidos) {
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
fun FornecedorCard(grupo: FornecedorGrupo, isExpanded: Boolean, selectedPedidos: SnapshotStateList<String>, onToggle: () -> Unit) {
    // --- NOVO: CÁLCULO DE RESUMO ---
    val qtdPedidos = grupo.pedidos.size
    val valorTotal = grupo.pedidos.map { it.valor }.fold(BigDecimal.ZERO, BigDecimal::add)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }
    ) {
        Column {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // --- NOVO: ÍCONE SOFISTICADO ---
                Box(
                    modifier = Modifier.size(48.dp).background(Color(0xFFDBEAFE), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF2563EB))
                }
                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Text(grupo.nomeFantasia, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                    Text(grupo.razaoSocial, fontSize = 12.sp, color = Slate500, maxLines = 1)

                    // --- NOVO: BADGE DE RESUMO FINANCEIRO ---
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$qtdPedidos pedido(s)  •  ${valorTotal.toPrecoFormatado()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Emerald500
                    )
                }
                Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Slate500)
            }

            // --- ANIMAÇÃO SUAVE DE ABERTURA ---
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                    Divider(color = Slate100, modifier = Modifier.padding(bottom = 12.dp))

                    // Lista de pedidos com espaçamento
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        grupo.pedidos.forEach { pedido ->
                            PedidoRow(pedido, selectedPedidos.contains(pedido.numero)) {
                                if (it) selectedPedidos.add(pedido.numero) else selectedPedidos.remove(pedido.numero)
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
fun PedidoRow(pedido: Pedido, isChecked: Boolean, onChecked: (Boolean) -> Unit) {
    // --- NOVO: DESIGN DE CARD INTERNO ---
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isChecked) Emerald500.copy(alpha = 0.05f) else Slate100.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { onChecked(!isChecked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(checkedColor = Emerald500)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Pedido #${pedido.numero}", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = Slate500)
                Spacer(Modifier.width(4.dp))
                Text(text = pedido.data.toDataPremium(), fontSize = 12.sp, color = Slate500)
            }
        }
        Text(
            text = pedido.valor.toPrecoFormatado(),
            fontWeight = FontWeight.Bold,
            color = if (isChecked) Emerald500 else Navy700,
            fontSize = 15.sp
        )
    }
}

// ... Manter funções fetchHierarquia e liberarSelecionados inalteradas abaixo ...
// Função que busca a hierarquia de Fornecedores e Pedidos de Compra
private fun fetchHierarquia(
    dbName: String,
    onResult: (List<FornecedorGrupo>) -> Unit
) = Thread {
    val mapa = linkedMapOf<String, FornecedorGrupo>()
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            conn.createStatement().use { st ->
                // Query original ajustada
                val sql = """
                  SELECT f.COD_FORNECEDOR,
                         f.NOM_FANTASIA,
                         f.RAZ_SOCIAL,
                         p.NUM_PEDI_COMPRA,
                         CONVERT(varchar(10), p.DAT_EMISSAO, 103) as DATA_EMISSAO,
                         p.VAL_TOTAL
                  FROM PEDIDO_COMPRA p
                  JOIN ITEM_PEDI_COMPRA i ON p.NUM_PEDI_COMPRA = i.NUM_PEDI_COMPRA
                  JOIN FORNECEDOR f ON p.COD_FORNECEDOR = f.COD_FORNECEDOR
                  WHERE i.COD_SITUACAO = 'A'
                  ORDER BY f.NOM_FANTASIA, p.NUM_PEDI_COMPRA
                """.trimIndent()

                st.executeQuery(sql).use { rs ->
                    while (rs.next()) {
                        val cod = rs.getString("COD_FORNECEDOR").trim()

                        // Cria ou recupera o Grupo do Fornecedor
                        val grupo = mapa.getOrPut(cod) {
                            FornecedorGrupo(
                                nomeFantasia = rs.getString("NOM_FANTASIA").trim(),
                                razaoSocial  = rs.getString("RAZ_SOCIAL").trim(),
                                codigo       = cod,
                                pedidos      = mutableListOf()
                            )
                        }

                        // Adiciona o pedido se ainda não existir na lista
                        val numPedido = rs.getString("NUM_PEDI_COMPRA").trim()
                        if (grupo.pedidos.none { it.numero == numPedido }) {
                            grupo.pedidos.add(
                                Pedido(
                                    numero = numPedido,
                                    data   = rs.getString("DATA_EMISSAO"),
                                    valor  = rs.getBigDecimal("VAL_TOTAL")
                                )
                            )
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    // Retorna a lista para a UI
    onResult(mapa.values.toList())
}.start()


// Função que libera os pedidos selecionados no banco
private fun liberarSelecionados(
    dbName: String,
    numeros: List<String>
): Int {
    if (numeros.isEmpty()) return 0

    var total = 0
    try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        DriverManager.getConnection(getUrl(dbName)).use { conn ->
            // AutoCommit false para garantir que ou atualiza tudo ou nada
            conn.autoCommit = false
            conn.createStatement().use { st ->
                numeros.forEach { num ->
                    st.addBatch("""
                        UPDATE ITEM_PEDI_COMPRA
                           SET COD_SITUACAO = 'B'
                         WHERE NUM_PEDI_COMPRA = '$num'
                           AND COD_SITUACAO    = 'A'
                    """.trimIndent())
                    total++
                }
                st.executeBatch()
            }
            conn.commit()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        total = -1
    }
    return total
}